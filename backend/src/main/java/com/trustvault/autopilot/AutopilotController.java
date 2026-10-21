package com.trustvault.autopilot;

import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trustvault.access.AccessService;
import com.trustvault.audit.AuditService;
import com.trustvault.auth.AuthUser;

// Autopilot turns a plain request into a proposed action. It never changes anything by itself: the owner
// must confirm the exact action, and the confirmation runs the same rules as the endpoints a person would use.
@RestController
@RequestMapping("/api/autopilot")
public class AutopilotController {

    public record ProposeRequest(String text) {}
    public record ConfirmRequest(String token) {}

    private static final Pattern DOC_ID = Pattern.compile("document\\s*#?\\s*(\\d+)");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+");
    private static final Pattern LIMIT = Pattern.compile("limit\\s*(?:of\\s*)?(\\d+)|(\\d+)\\s*downloads?");

    private final JdbcTemplate jdbc;
    private final AccessService access;
    private final AuditService audit;

    public AutopilotController(JdbcTemplate jdbc, AccessService access, AuditService audit) {
        this.jdbc = jdbc;
        this.access = access;
        this.audit = audit;
    }

    // Step 1: understand the request and describe the action. Nothing is changed here.
    @PostMapping("/propose")
    public ResponseEntity<?> propose(@AuthenticationPrincipal AuthUser user, @RequestBody ProposeRequest req) {
        String text = req == null || req.text() == null ? "" : req.text().trim();
        String lower = text.toLowerCase(Locale.ROOT);

        Matcher idMatch = DOC_ID.matcher(lower);
        if (!idMatch.find()) {
            return ask("Which document? Name it by its number, for example: document 59.");
        }
        long docId = Long.parseLong(idMatch.group(1));

        // The proposal is only as good as the user's own rights, so check the owner now. The confirm step checks again.
        if (!access.isOwnerOf(user.id(), docId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "This document is not available to you."));
        }

        Matcher email = EMAIL.matcher(text);
        String person = email.find() ? email.group().toLowerCase(Locale.ROOT) : null;

        Map<String, Object> action;
        if (lower.contains("remove") || lower.contains("revoke") || lower.contains("stop sharing")) {
            if (person == null) return ask("Whose access should I remove? Give me their email address.");
            action = Map.of("type", "REMOVE_ACCESS", "documentId", docId, "email", person);
        } else if (lower.contains("share") || lower.contains("give access")) {
            if (person == null) return ask("Who should receive it? Give me their email address.");
            String permission = lower.contains("edit") ? "EDIT" : "READ";
            action = Map.of("type", "SHARE", "documentId", docId, "email", person, "permission", permission);
        } else if (lower.contains("limit") || lower.contains("download")) {
            if (person == null) return ask("For which person should I set the limit? Give me their email address.");
            Matcher limit = LIMIT.matcher(lower);
            if (!limit.find()) return ask("How many downloads should be allowed? For example: limit 1.");
            int n = Integer.parseInt(limit.group(1) != null ? limit.group(1) : limit.group(2));
            action = Map.of("type", "SET_DOWNLOAD_LIMIT", "documentId", docId, "email", person, "downloadLimit", n);
        } else {
            return ask("I can share a document, remove access, or set a download limit. Which would you like?");
        }

        // The token ties the proposal to this user and this exact action, so it cannot be changed before confirming.
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(
                (user.id() + "|" + action.toString()).getBytes(java.nio.charset.StandardCharsets.UTF_8));

        audit.record(user.id(), "AUTOPILOT_PROPOSE", docId, true, action.get("type") + " proposed");
        return ResponseEntity.ok(Map.of(
                "needsConfirmation", true,
                "summary", describe(action),
                "action", action,
                "token", token));
    }

    // Step 2: run the action, only after the owner confirms. The same rules as the normal endpoints apply.
    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@AuthenticationPrincipal AuthUser user, @RequestBody ConfirmRequest req) {
        String decoded;
        try {
            decoded = new String(Base64.getUrlDecoder().decode(req.token()), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "This proposal is not valid."));
        }
        if (!decoded.startsWith(user.id() + "|")) {
            audit.record(user.id(), "AUTOPILOT_CONFIRM", null, false, "proposal belongs to another user");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "This proposal is not yours."));
        }
        // The proposal is a fixed description, so it can be applied only in the way it was written.
        String action = decoded.substring(decoded.indexOf('|') + 1);
        long docId = extract(action, "documentId");
        if (!access.isOwnerOf(user.id(), docId)) {
            audit.record(user.id(), "AUTOPILOT_CONFIRM", docId, false, "not owner");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "This document is not available to you."));
        }

        String type = field(action, "type");
        String email = field(action, "email");
        audit.record(user.id(), "AUTOPILOT_CONFIRM", docId, true, type + " confirmed");

        // Each branch runs the same database change as the matching endpoint, after the owner check above.
        switch (type) {
            case "SHARE" -> {
                String permission = field(action, "permission");
                Long target = findUser(email);
                if (target == null) return ResponseEntity.badRequest().body(Map.of("error", "No active user with that email."));
                jdbc.update("""
                        INSERT INTO grants (document_id, user_id, permission, granted_by) VALUES (?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE permission = VALUES(permission), granted_by = VALUES(granted_by)""",
                        docId, target, permission, user.id());
                audit.record(user.id(), "GRANT", docId, true, permission + " to user " + target + " (autopilot)");
            }
            case "REMOVE_ACCESS" -> {
                Long target = findUser(email);
                if (target == null) return ResponseEntity.badRequest().body(Map.of("error", "No active user with that email."));
                jdbc.update("DELETE FROM grants WHERE document_id = ? AND user_id = ?", docId, target);
                audit.record(user.id(), "REVOKE", docId, true, "revoked user " + target + " (autopilot)");
            }
            case "SET_DOWNLOAD_LIMIT" -> {
                Long target = findUser(email);
                if (target == null) return ResponseEntity.badRequest().body(Map.of("error", "No active user with that email."));
                int n = (int) extract(action, "downloadLimit");
                jdbc.update("UPDATE grants SET download_limit = ? WHERE document_id = ? AND user_id = ?", n, docId, target);
                audit.record(user.id(), "SET_CAPSULE", docId, true, "limit " + n + " for user " + target + " (autopilot)");
            }
            default -> {
                return ResponseEntity.badRequest().body(Map.of("error", "This action is not supported."));
            }
        }
        return ResponseEntity.ok(Map.of("done", true, "summary", "Done. The change is recorded in the audit trail."));
    }

    private static ResponseEntity<Map<String, Object>> ask(String question) {
        return ResponseEntity.ok(Map.of("needsConfirmation", false, "question", question));
    }

    private static String describe(Map<String, Object> action) {
        return switch ((String) action.get("type")) {
            case "SHARE" -> "Share document " + action.get("documentId") + " with " + action.get("email")
                    + " as " + action.get("permission") + ".";
            case "REMOVE_ACCESS" -> "Remove " + action.get("email") + "'s access to document " + action.get("documentId") + ".";
            case "SET_DOWNLOAD_LIMIT" -> "Allow " + action.get("email") + " " + action.get("downloadLimit")
                    + " download(s) of document " + action.get("documentId") + ".";
            default -> "Unknown action.";
        };
    }

    private Long findUser(String email) {
        var rows = jdbc.queryForList("SELECT id FROM users WHERE email = ? AND active = TRUE", Long.class, email);
        return rows.isEmpty() ? null : rows.get(0);
    }

    // Reads a value from the fixed proposal text, such as "documentId=59".
    private static String field(String action, String key) {
        Matcher m = Pattern.compile(key + "=([^,}]+)").matcher(action);
        return m.find() ? m.group(1).trim() : "";
    }

    private static long extract(String action, String key) {
        String v = field(action, key);
        return v.isEmpty() ? 0 : Long.parseLong(v);
    }
}
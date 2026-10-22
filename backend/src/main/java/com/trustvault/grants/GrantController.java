package com.trustvault.grants;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.trustvault.access.AccessService;
import com.trustvault.audit.AuditService;
import com.trustvault.auth.AuthUser;
import com.trustvault.notify.Mailer;

@RestController
@RequestMapping("/api/documents/{id}/grants")
public class GrantController {

    public record GrantRequest(String email, String permission) {}
    public record ConditionRequest(String expiresAt, Integer downloadLimit) {}

    private final JdbcTemplate jdbc;
    private final AccessService access;
    private final AuditService audit;
    private final Mailer mailer;

    public GrantController(JdbcTemplate jdbc, AccessService access, AuditService audit, Mailer mailer) {
        this.jdbc = jdbc;
        this.access = access;
        this.audit = audit;
        this.mailer = mailer;
    }

    // Owner only: who currently has access to this document.
    @GetMapping
    public List<Map<String, Object>> list(@AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        access.requireOwner(user.id(), id, "LIST_GRANTS");
        return jdbc.queryForList("""
                SELECT u.id AS user_id, u.email, g.permission, g.granted_by,
                       g.expires_at, g.download_limit, g.downloads_used
                FROM grants g
                JOIN users u ON u.id = g.user_id
                WHERE g.document_id = ?""", id);
    }

    // Owner only: give a user READ or EDIT. Granting again changes the permission.
    // The recipient is then emailed that a file has been shared. The email names no file and holds no contents,
    // and a failed email never undoes the share.
    @PostMapping
    public ResponseEntity<?> grant(@AuthenticationPrincipal AuthUser user, @PathVariable long id,
                                   @RequestBody GrantRequest request) {
        access.requireOwner(user.id(), id, "GRANT");

        if (request == null || request.email() == null || request.permission() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and permission are required."));
        }
        String permission = request.permission().trim().toUpperCase(Locale.ROOT);
        if (!permission.equals("READ") && !permission.equals("EDIT")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Permission must be READ or EDIT."));
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        List<Long> found = jdbc.queryForList(
                "SELECT id FROM users WHERE email = ? AND active = TRUE", Long.class, email);
        if (found.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No active user with that email."));
        }
        long target = found.get(0);

        jdbc.update("""
                INSERT INTO grants (document_id, user_id, permission, granted_by)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE permission = VALUES(permission), granted_by = VALUES(granted_by)""",
                id, target, permission, user.id());

        audit.record(user.id(), "GRANT", id, true, permission + " to user " + target);

        // Tell the recipient to sign in. No file name and no contents go in the email.
        String recipientEmail = jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, target);
        boolean notified = mailer.send(recipientEmail,
                "A file has been shared with you on TrustVault",
                "Someone has shared a document with you. Sign in to TrustVault to view it.\n\n"
                + "This message does not include the document or its name.");
        audit.record(user.id(), "NOTIFY_SHARE", id, notified,
                notified ? "share notification sent" : "share notification not sent");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("documentId", id, "userId", target, "permission", permission));
    }

    // Owner only: set or change the conditions on a recipient's grant (the Smart File Capsule).
    // A null value clears that condition. The server enforces both conditions on every request.
    @PatchMapping("/{userId}")
    public ResponseEntity<?> conditions(@AuthenticationPrincipal AuthUser user, @PathVariable long id,
                                        @PathVariable long userId, @RequestBody ConditionRequest req) {
        access.requireOwner(user.id(), id, "SET_CAPSULE");

        java.sql.Timestamp expires = null;
        if (req.expiresAt() != null && !req.expiresAt().isBlank()) {
            try {
                expires = java.sql.Timestamp.from(Instant.parse(req.expiresAt()));
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Expiry must be an ISO date-time, such as 2026-12-31T23:59:00Z."));
            }
        }
        if (req.downloadLimit() != null && req.downloadLimit() < 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "The download limit must be at least 1."));
        }

        int updated = jdbc.update(
                "UPDATE grants SET expires_at = ?, download_limit = ? WHERE document_id = ? AND user_id = ?",
                expires, req.downloadLimit(), id, userId);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        audit.record(user.id(), "SET_CAPSULE", id, true,
                "conditions for user " + userId
                + (expires != null ? ", expires" : "")
                + (req.downloadLimit() != null ? ", limit " + req.downloadLimit() : ""));

        return ResponseEntity.ok(Map.of(
                "documentId", id,
                "userId", userId,
                "expiresAt", expires == null ? "none" : expires.toInstant().toString(),
                "downloadLimit", req.downloadLimit() == null ? "none" : req.downloadLimit()));
    }

    // Owner only: remove a user's access. It takes effect on that user's next request.
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal AuthUser user, @PathVariable long id,
                                       @PathVariable long userId) {
        access.requireOwner(user.id(), id, "REVOKE");

        int removed = jdbc.update("DELETE FROM grants WHERE document_id = ? AND user_id = ?", id, userId);
        if (removed == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        audit.record(user.id(), "REVOKE", id, true, "revoked user " + userId);
        return ResponseEntity.noContent().build();
    }
}
package com.trustvault.grants;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.trustvault.access.AccessService;
import com.trustvault.audit.AuditService;
import com.trustvault.auth.AuthUser;

@RestController
@RequestMapping("/api/documents/{id}/grants")
public class GrantController {

    public record GrantRequest(String email, String permission) {}

    private final JdbcTemplate jdbc;
    private final AccessService access;
    private final AuditService audit;

    public GrantController(JdbcTemplate jdbc, AccessService access, AuditService audit) {
        this.jdbc = jdbc;
        this.access = access;
        this.audit = audit;
    }

    // Owner only: who currently has access to this document.
    @GetMapping
    public List<Map<String, Object>> list(@AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        access.requireOwner(user.id(), id, "LIST_GRANTS");
        return jdbc.queryForList("""
                SELECT u.id AS user_id, u.email, g.permission, g.granted_by
                FROM grants g
                JOIN users u ON u.id = g.user_id
                WHERE g.document_id = ?""", id);
    }

    // Owner only: give a user READ or EDIT. Granting again changes the permission.
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

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("documentId", id, "userId", target, "permission", permission));
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
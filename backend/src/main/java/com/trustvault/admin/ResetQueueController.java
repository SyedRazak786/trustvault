package com.trustvault.admin;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trustvault.audit.AuditService;
import com.trustvault.auth.AuthUser;

// Administrators only. Shows pending reset requests without tokens, and records when support has
// delivered a link. A delivery mark never changes a password; the user still needs the one-time link.
@RestController
@RequestMapping("/api/admin/reset-queue")
public class ResetQueueController {

    private final JdbcTemplate jdbc;
    private final AuditService audit;

    public ResetQueueController(JdbcTemplate jdbc, AuditService audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal AuthUser user) {
        if (!"ADMIN".equals(user.role())) {
            audit.record(user.id(), "ADMIN_VIEW_RESET_QUEUE", null, false, "role " + user.role() + " is not admin");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Administrators only."));
        }
        // The token hash is deliberately not selected. Only identifying metadata is returned.
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT r.id, u.email, r.created_at, r.expires_at, r.used,
                       CASE WHEN r.used = FALSE AND r.expires_at > CURRENT_TIMESTAMP THEN 'PENDING'
                            WHEN r.used = TRUE THEN 'USED OR REPLACED'
                            ELSE 'EXPIRED' END AS status
                FROM password_resets r
                JOIN users u ON u.id = r.user_id
                ORDER BY r.id DESC
                LIMIT 100""");
        audit.record(user.id(), "ADMIN_VIEW_RESET_QUEUE", null, true, "reset queue viewed");
        return ResponseEntity.ok(rows);
    }

    // Records that support has handed the link to the user by a trusted route. Only pending requests can be marked.
    @PostMapping("/{id}/delivered")
    public ResponseEntity<?> delivered(@AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        if (!"ADMIN".equals(user.role())) {
            audit.record(user.id(), "RESET_DELIVERED", id, false, "role " + user.role() + " is not admin");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Administrators only."));
        }
        // Mark only while the token is still pending, so a used or expired request cannot be re-marked.
        int updated = jdbc.update("""
                UPDATE password_resets SET delivered_at = CURRENT_TIMESTAMP
                WHERE id = ? AND used = FALSE AND expires_at > CURRENT_TIMESTAMP AND delivered_at IS NULL""", id);
        if (updated == 0) {
            audit.record(user.id(), "RESET_DELIVERED", id, false, "not pending or already marked");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No pending request with that id."));
        }
        audit.record(user.id(), "RESET_DELIVERED", id, true, "marked delivered by support");
        return ResponseEntity.noContent().build();
    }
}
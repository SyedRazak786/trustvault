package com.trustvault.admin;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trustvault.audit.AuditService;
import com.trustvault.auth.AuthUser;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final JdbcTemplate jdbc;
    private final AuditService audit;

    public AdminController(JdbcTemplate jdbc, AuditService audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    // Recent activity across all documents. Administrators see who did what and when,
    // but this endpoint returns no document contents.
    @GetMapping("/activity")
    public ResponseEntity<?> activity(@AuthenticationPrincipal AuthUser user,
                                      @RequestParam(value = "limit", defaultValue = "100") int limit) {
        if (!isAdmin(user, "ADMIN_VIEW_ACTIVITY")) {
            return forbidden();
        }
        return ResponseEntity.ok(jdbc.queryForList("""
                SELECT a.id, a.created_at, actor.email AS actor, a.action, a.document_id, a.outcome, a.detail
                FROM audit_log a
                LEFT JOIN users actor ON actor.id = a.actor_id
                ORDER BY a.id DESC LIMIT ?""", clamp(limit)));
    }

    // Permission changes only: every grant and revoke, including the owner who made it.
    @GetMapping("/permissions")
    public ResponseEntity<?> permissions(@AuthenticationPrincipal AuthUser user,
                                         @RequestParam(value = "limit", defaultValue = "100") int limit) {
        if (!isAdmin(user, "ADMIN_VIEW_PERMISSIONS")) {
            return forbidden();
        }
        return ResponseEntity.ok(jdbc.queryForList("""
                SELECT a.id, a.created_at, actor.email AS actor, a.action, a.document_id, a.outcome, a.detail
                FROM audit_log a
                LEFT JOIN users actor ON actor.id = a.actor_id
                WHERE a.action IN ('GRANT', 'REVOKE')
                ORDER BY a.id DESC LIMIT ?""", clamp(limit)));
    }

    // Recomputes the whole audit hash chain and reports the first row that does not match.
    @GetMapping("/audit/verify")
    public ResponseEntity<?> verify(@AuthenticationPrincipal AuthUser user) {
        if (!isAdmin(user, "ADMIN_VERIFY_AUDIT")) {
            return forbidden();
        }
        return ResponseEntity.ok(audit.verifyChain());
    }

    // Anyone who is not an administrator is refused, and the refusal is recorded.
    private boolean isAdmin(AuthUser user, String action) {
        if ("ADMIN".equals(user.role())) {
            return true;
        }
        audit.record(user.id(), action, null, false, "role " + user.role() + " is not admin");
        return false;
    }

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Administrators only."));
    }

    private static int clamp(int limit) {
        return Math.min(Math.max(limit, 1), 500);
    }
}
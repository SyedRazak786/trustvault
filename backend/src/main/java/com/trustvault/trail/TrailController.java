package com.trustvault.trail;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trustvault.access.AccessService;
import com.trustvault.audit.AuditService;
import com.trustvault.auth.AuthUser;

// The trail for one document: its important events in order. Only the owner or an administrator may read it.
// A reader with only a grant cannot see the trail, because the trail reveals who else has touched the file.
@RestController
@RequestMapping("/api/documents/{id}/trail")
public class TrailController {

    private final JdbcTemplate jdbc;
    private final AccessService access;
    private final AuditService audit;

    public TrailController(JdbcTemplate jdbc, AccessService access, AuditService audit) {
        this.jdbc = jdbc;
        this.access = access;
        this.audit = audit;
    }

    @GetMapping
    public ResponseEntity<?> trail(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser user,
                                   @PathVariable long id) {
        boolean admin = "ADMIN".equals(user.role());
        boolean owner = !admin && access.isOwnerOf(user.id(), id);
        if (!admin && !owner) {
            audit.record(user.id(), "VIEW_TRAIL", id, false, "not owner or administrator");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "This document is not available to you."));
        }

        List<Map<String, Object>> events = jdbc.queryForList("""
                SELECT a.id, a.created_at, actor.email AS actor, a.action, a.outcome, a.detail
                FROM audit_log a
                LEFT JOIN users actor ON actor.id = a.actor_id
                WHERE a.document_id = ?
                ORDER BY a.id ASC""", id);

        audit.record(user.id(), "VIEW_TRAIL", id, true, "trail viewed");
        AuditService.ChainResult chain = audit.verifyChain();
        return ResponseEntity.ok(Map.of("documentId", id, "events", events, "chainIntact", chain.intact()));
    }
}
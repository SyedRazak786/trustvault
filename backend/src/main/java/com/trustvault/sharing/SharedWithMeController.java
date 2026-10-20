package com.trustvault.sharing;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trustvault.auth.AuthUser;

@RestController
@RequestMapping("/api/shares")
public class SharedWithMeController {

    private final JdbcTemplate jdbc;

    public SharedWithMeController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Documents other owners have granted to the caller. The caller's own documents are excluded.
    @GetMapping
    public List<Map<String, Object>> shared(@AuthenticationPrincipal AuthUser user) {
        return jdbc.queryForList("""
                SELECT d.id, d.name, d.category, d.content_type, d.size_bytes, d.created_at,
                       o.email AS owner_email, g.permission
                FROM grants g
                JOIN documents d ON d.id = g.document_id AND d.deleted = FALSE
                JOIN users o ON o.id = d.owner_id
                JOIN users me ON me.id = g.user_id AND me.active = TRUE
                WHERE g.user_id = ? AND d.owner_id <> g.user_id
                ORDER BY d.created_at DESC""", user.id());
    }
}
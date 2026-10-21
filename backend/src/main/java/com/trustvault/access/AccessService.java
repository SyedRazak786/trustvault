package com.trustvault.access;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.trustvault.audit.AuditService;

@Service
public class AccessService {

    public enum Permission { READ, EDIT }

    private final JdbcTemplate jdbc;
    private final AuditService audit;

    public AccessService(JdbcTemplate jdbc, AuditService audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    // Read from MySQL on every call, so a revoked grant stops working immediately.
    // The owner has full rights. EDIT includes READ. Inactive users and deleted documents are refused.
    public boolean can(long userId, long documentId, Permission needed) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM documents d
                JOIN users u ON u.id = ? AND u.active = TRUE
                WHERE d.id = ? AND d.deleted = FALSE AND (
                    d.owner_id = u.id
                    OR EXISTS (
                        SELECT 1 FROM grants g
                        WHERE g.document_id = d.id AND g.user_id = u.id
                          AND (g.permission = 'EDIT' OR ? = 'READ')))""",
                Integer.class, userId, documentId, needed.name());
        return n != null && n > 0;
    }

    // Refused attempts are recorded, then answered with 404. The same answer is used for
    // a missing document and a forbidden one, so a caller cannot tell which it is.
    public void require(long userId, long documentId, Permission needed, String action) {
        if (!can(userId, documentId, needed)) {
            audit.record(userId, action, documentId, false, "missing " + needed + " access");
            throw new ResponseStatusException(NOT_FOUND);
        }
    }

    // Only the owner may grant, revoke or view grants. A grant holder with EDIT cannot share further.
    public void requireOwner(long userId, long documentId, String action) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM documents d
                JOIN users u ON u.id = ? AND u.active = TRUE
                WHERE d.id = ? AND d.deleted = FALSE AND d.owner_id = u.id""",
                Integer.class, userId, documentId);
        if (n == null || n == 0) {
            audit.record(userId, action, documentId, false, "not owner");
            throw new ResponseStatusException(NOT_FOUND);
        }
    }
        public boolean isOwnerOf(long userId, long documentId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM documents WHERE id = ? AND owner_id = ? AND deleted = FALSE",
                Integer.class, documentId, userId);
        return n != null && n > 0;
    }

    // The list uses the same visibility rule as can(), so search and list cannot show more than downloads allow.
    public List<Map<String, Object>> listVisible(long userId) {
        return jdbc.queryForList("""
                SELECT d.id, d.name, d.category, d.content_type, d.size_bytes, d.created_at
                FROM documents d
                JOIN users u ON u.id = ? AND u.active = TRUE
                WHERE d.deleted = FALSE AND (
                    d.owner_id = u.id
                    OR EXISTS (SELECT 1 FROM grants g WHERE g.document_id = d.id AND g.user_id = u.id))
                ORDER BY d.created_at DESC""", userId);
    }
}
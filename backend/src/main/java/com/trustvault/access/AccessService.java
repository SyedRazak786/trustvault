package com.trustvault.access;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.List;
import java.util.Map;

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
    // The owner has full rights and is never limited by a capsule. EDIT includes READ.
    // A grant can carry an expiry and a download limit (the Smart File Capsule); once either is reached,
    // the grant stops working. Inactive users and deleted documents are refused.
    public boolean can(long userId, long documentId, Permission needed) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM documents d
                JOIN users u ON u.id = ? AND u.active = TRUE
                WHERE d.id = ? AND d.deleted = FALSE AND (
                    d.owner_id = u.id
                    OR EXISTS (
                        SELECT 1 FROM grants g
                        WHERE g.document_id = d.id AND g.user_id = u.id
                          AND (g.permission = 'EDIT' OR ? = 'READ')
                          AND (g.expires_at IS NULL OR g.expires_at > CURRENT_TIMESTAMP)
                          AND (g.download_limit IS NULL OR g.downloads_used < g.download_limit)))""",
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

    // Only the owner may grant, revoke, view grants or set capsule conditions. A grant holder cannot share further.
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

    // True only for the owner of an active document. Used where the owner alone may see something.
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
                    OR EXISTS (
                        SELECT 1 FROM grants g
                        WHERE g.document_id = d.id AND g.user_id = u.id
                          AND (g.expires_at IS NULL OR g.expires_at > CURRENT_TIMESTAMP)
                          AND (g.download_limit IS NULL OR g.downloads_used < g.download_limit)))
                ORDER BY d.created_at DESC""", userId);
    }
}
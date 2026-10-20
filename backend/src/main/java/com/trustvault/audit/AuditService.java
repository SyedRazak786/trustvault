package com.trustvault.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class AuditService {

    private static final String GENESIS = "0".repeat(64);
    private final JdbcTemplate jdbc;

    public record ChainResult(boolean intact, long rowsChecked, Long firstBadId) {}

    public AuditService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // synchronized so two requests cannot both link to the same previous hash
    public synchronized void record(Long actorId, String action, Long documentId,
                                    boolean allowed, String detail) {
        String prev = jdbc.query(
                "SELECT hash FROM audit_log ORDER BY id DESC LIMIT 1",
                rs -> rs.next() ? rs.getString(1) : GENESIS);

        String safeDetail = detail == null ? "" : detail.substring(0, Math.min(detail.length(), 500));
        String payload = String.join("|", prev,
                String.valueOf(actorId), action, String.valueOf(documentId),
                String.valueOf(allowed), safeDetail);

        jdbc.update("""
                INSERT INTO audit_log (prev_hash, hash, actor_id, action, document_id, outcome, detail)
                VALUES (?, ?, ?, ?, ?, ?, ?)""",
                prev, sha256(payload), actorId, action, documentId,
                allowed ? "ALLOWED" : "DENIED", safeDetail);
    }

    // Walks the log from the first row. Each row must link to the previous hash, and its stored hash
    // must match a fresh calculation from its own fields. Any edit that does not recompute the whole
    // chain is detected at the first changed row.
    public synchronized ChainResult verifyChain() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, prev_hash, hash, actor_id, action, document_id, outcome, detail
                FROM audit_log ORDER BY id ASC""");

        String expectedPrev = GENESIS;
        long checked = 0;
        for (Map<String, Object> r : rows) {
            checked++;
            long id = ((Number) r.get("id")).longValue();
            String prev = (String) r.get("prev_hash");
            String hash = (String) r.get("hash");
            boolean allowed = "ALLOWED".equals(r.get("outcome"));
            String detail = r.get("detail") == null ? "" : (String) r.get("detail");

            String payload = String.join("|", prev,
                    String.valueOf(r.get("actor_id")), (String) r.get("action"),
                    String.valueOf(r.get("document_id")), String.valueOf(allowed), detail);

            if (!expectedPrev.equals(prev) || !sha256(payload).equals(hash)) {
                return new ChainResult(false, checked, id);
            }
            expectedPrev = hash;
        }
        return new ChainResult(true, checked, null);
    }

    private static String sha256(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
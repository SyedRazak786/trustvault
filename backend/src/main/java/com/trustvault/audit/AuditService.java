package com.trustvault.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class AuditService {

    private static final String GENESIS = "0".repeat(64);
    private final JdbcTemplate jdbc;

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
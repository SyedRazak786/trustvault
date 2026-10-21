package com.trustvault.account;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.trustvault.audit.AuditService;

@RestController
public class PasswordResetController {

    public record RequestReset(String email) {}
    public record ConfirmReset(String token, String newPassword) {}

    private static final int MIN_PASSWORD = 12;
    private static final int MAX_PASSWORD = 128;
    private static final long TTL_MINUTES = 30;

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetController(JdbcTemplate jdbc, PasswordEncoder encoder, AuditService audit) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.audit = audit;
    }

    // Always returns the same answer, so the form cannot be used to find accounts.
    // For a real account, a one-time token is created and recorded for support to deliver.
    @PostMapping("/api/auth/reset/request")
    public ResponseEntity<?> request(@RequestBody RequestReset req) {
        String same = "If an account exists for that email, a reset link will be sent by support.";
        if (req == null || req.email() == null) {
            return ResponseEntity.ok(Map.of("message", same));
        }
        String email = req.email().trim().toLowerCase(Locale.ROOT);

        List<Long> ids = jdbc.queryForList(
                "SELECT id FROM users WHERE email = ? AND active = TRUE", Long.class, email);

        if (!ids.isEmpty()) {
            long userId = ids.get(0);
            // Cancel any earlier unused token, so only the newest one works.
            jdbc.update("UPDATE password_resets SET used = TRUE WHERE user_id = ? AND used = FALSE", userId);

            byte[] raw = new byte[32];
            random.nextBytes(raw);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

            jdbc.update("INSERT INTO password_resets (user_id, token_hash, expires_at) VALUES (?, ?, ?)",
                    userId, sha256(token), java.sql.Timestamp.from(Instant.now().plusSeconds(TTL_MINUTES * 60)));

            // The token itself is never written to the log. Support receives it through the admin queue.
            audit.record(userId, "PASSWORD_RESET_REQUEST", null, true, "reset token issued");
        } else {
            audit.record(null, "PASSWORD_RESET_REQUEST", null, false, "unknown account");
        }
        return ResponseEntity.ok(Map.of("message", same));
    }

    // Uses the one-time token. Expired, used or unknown tokens all get the same refusal.
    @PostMapping("/api/auth/reset/confirm")
    public ResponseEntity<?> confirm(@RequestBody ConfirmReset req) {
        String refused = "This reset link is invalid or has expired. Request a new one.";
        if (req == null || req.token() == null || req.newPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", refused));
        }
        if (req.newPassword().length() < MIN_PASSWORD || req.newPassword().length() > MAX_PASSWORD) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be 12 to 128 characters."));
        }

        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, user_id FROM password_resets
                WHERE token_hash = ? AND used = FALSE AND expires_at > CURRENT_TIMESTAMP""", sha256(req.token()));

        if (rows.isEmpty()) {
            audit.record(null, "PASSWORD_RESET_CONFIRM", null, false, "invalid or expired token");
            return ResponseEntity.badRequest().body(Map.of("error", refused));
        }

        long resetId = ((Number) rows.get(0).get("id")).longValue();
        long userId = ((Number) rows.get(0).get("user_id")).longValue();

        // Mark the token used first, so it cannot be used twice even if two requests race.
        int claimed = jdbc.update("UPDATE password_resets SET used = TRUE WHERE id = ? AND used = FALSE", resetId);
        if (claimed == 0) {
            return ResponseEntity.badRequest().body(Map.of("error", refused));
        }

        jdbc.update("UPDATE users SET password_hash = ? WHERE id = ?", encoder.encode(req.newPassword()), userId);
        audit.record(userId, "PASSWORD_RESET_CONFIRM", null, true, "password reset with one-time token");
        return ResponseEntity.noContent().build();
    }

    private static String sha256(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
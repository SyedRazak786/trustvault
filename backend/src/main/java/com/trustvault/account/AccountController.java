package com.trustvault.account;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.trustvault.audit.AuditService;
import com.trustvault.auth.AuthUser;

@RestController
public class AccountController {

    public record SignupRequest(String email, String password, String displayName) {}
    public record ProfileRequest(String displayName, String phone) {}
    public record PasswordRequest(String currentPassword, String newPassword) {}

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^[0-9+()\\- ]{0,30}$");
    private static final int MIN_PASSWORD = 12;
    private static final int MAX_PASSWORD = 128;

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public AccountController(JdbcTemplate jdbc, PasswordEncoder encoder, AuditService audit) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.audit = audit;
    }

    // Public. Every new account is an OWNER. Any role field sent by the browser is ignored.
    @PostMapping("/api/auth/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest req) {
        if (req == null || req.email() == null || req.password() == null) {
            return bad("Email and password are required.");
        }
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (email.length() > 255 || !EMAIL.matcher(email).matches()) {
            return bad("Enter a valid email address.");
        }
        if (req.password().length() < MIN_PASSWORD || req.password().length() > MAX_PASSWORD) {
            return bad("Password must be 12 to 128 characters.");
        }

        try {
            jdbc.update("INSERT INTO users (email, password_hash, role, active, display_name) VALUES (?, ?, 'OWNER', TRUE, ?)",
                    email, encoder.encode(req.password()), clean(req.displayName(), 100));
        } catch (DuplicateKeyException e) {
            // The email is not written to the audit detail, so the log does not copy personal data.
            audit.record(null, "SIGNUP", null, false, "email already registered");
            return ResponseEntity.status(409).body(Map.of("error", "An account with this email already exists."));
        }

        long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        audit.record(id, "SIGNUP", null, true, "account created");
        return ResponseEntity.status(201).body(Map.of("id", id, "email", email, "role", "OWNER"));
    }

    @GetMapping("/api/account/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal AuthUser user) {
        return profileOf(user.id());
    }

    @PutMapping("/api/account/me")
    public ResponseEntity<?> update(@AuthenticationPrincipal AuthUser user, @RequestBody ProfileRequest req) {
        if (req == null) {
            return bad("Profile details are required.");
        }
        String phone = clean(req.phone(), 30);
        if (phone != null && !PHONE.matcher(phone).matches()) {
            return bad("Phone may contain digits, spaces, +, -, and brackets only.");
        }
        jdbc.update("UPDATE users SET display_name = ?, phone = ? WHERE id = ? AND active = TRUE",
                clean(req.displayName(), 100), phone, user.id());
        audit.record(user.id(), "PROFILE_UPDATE", null, true, "profile updated");
        return profileOf(user.id());
    }

    // Requires the current password. The email and role cannot be changed here.
    @PostMapping("/api/account/password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal AuthUser user, @RequestBody PasswordRequest req) {
        if (req == null || req.currentPassword() == null || req.newPassword() == null) {
            return bad("Current and new password are required.");
        }
        if (req.newPassword().length() < MIN_PASSWORD || req.newPassword().length() > MAX_PASSWORD) {
            return bad("New password must be 12 to 128 characters.");
        }
        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ? AND active = TRUE",
                String.class, user.id());
        if (!encoder.matches(req.currentPassword(), hash)) {
            audit.record(user.id(), "PASSWORD_CHANGE", null, false, "current password incorrect");
            return bad("The current password is not correct.");
        }
        jdbc.update("UPDATE users SET password_hash = ? WHERE id = ?", encoder.encode(req.newPassword()), user.id());
        audit.record(user.id(), "PASSWORD_CHANGE", null, true, "password changed");
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> profileOf(long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, email, role, display_name, phone FROM users WHERE id = ? AND active = TRUE", id);
        if (rows.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        return ResponseEntity.ok(rows.get(0));
    }

    private static ResponseEntity<Map<String, String>> bad(String message) {
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    private static String clean(String value, int max) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }
}
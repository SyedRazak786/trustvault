package com.trustvault.auth;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trustvault.audit.AuditService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(String email, String password) {}

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;
    private final String dummyHash;

    public AuthController(JdbcTemplate jdbc, PasswordEncoder encoder, JwtService jwt, AuditService audit) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.jwt = jwt;
        this.audit = audit;
        // Used for unknown emails so the response takes about as long as a real check.
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null || request.email() == null || request.password() == null) {
            return ResponseEntity.badRequest().build();
        }
        String email = request.email().trim().toLowerCase();

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, role, password_hash, active FROM users WHERE email = ?", email);

        if (rows.isEmpty()) {
            encoder.matches(request.password(), dummyHash);
            audit.record(null, "LOGIN", null, false, "unknown account");
            return refused();
        }

        Map<String, Object> user = rows.get(0);
        long userId = ((Number) user.get("id")).longValue();
        String role = (String) user.get("role");
        String hash = (String) user.get("password_hash");
        boolean active = isTrue(user.get("active"));

        boolean passwordOk = encoder.matches(request.password(), hash);
        if (!active || !passwordOk) {
            audit.record(userId, "LOGIN", null, false, active ? "wrong password" : "inactive account");
            return refused();
        }

        audit.record(userId, "LOGIN", null, true, "signed in");
        return ResponseEntity.ok(Map.of(
                "token", jwt.issue(userId, role),
                "email", email,
                "role", role));
    }

    // The same response for an unknown email, a wrong password and an inactive account.
    private ResponseEntity<Map<String, String>> refused() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Email or password is incorrect."));
    }

    private static boolean isTrue(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        return false;
    }
}
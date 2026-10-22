package com.trustvault.notify;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.trustvault.audit.AuditService;
import com.trustvault.auth.AuthUser;

// Administrators only. Sends one test email, so the mail setup can be checked before sharing uses it.
@RestController
public class MailTestController {

    public record TestRequest(String to) {}

    private final Mailer mailer;
    private final AuditService audit;

    public MailTestController(Mailer mailer, AuditService audit) {
        this.mailer = mailer;
        this.audit = audit;
    }

    @PostMapping("/api/admin/mail-test")
    public ResponseEntity<?> test(@AuthenticationPrincipal AuthUser user, @RequestBody TestRequest req) {
        if (!"ADMIN".equals(user.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Administrators only."));
        }
        boolean sent = mailer.send(req.to(), "TrustVault test email",
                "This is a test. If you received it, TrustVault can send email.");
        audit.record(user.id(), "MAIL_TEST", null, sent, sent ? "test email sent" : "test email not sent");
        return ResponseEntity.ok(Map.of("sent", sent));
    }
}
package com.trustvault.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// Sends email. The message carries no file name and no document contents: it only tells the
// recipient to sign in. A failed send is logged and never blocks the sharing itself.
@Service
public class Mailer {

    private static final Logger log = LoggerFactory.getLogger(Mailer.class);

    private final JavaMailSender sender;
    private final String from;

    public Mailer(JavaMailSender sender, @Value("${trustvault.mail-from:}") String from) {
        this.sender = sender;
        this.from = from;
    }

    public boolean isConfigured() {
        return from != null && !from.isBlank();
    }

    // Returns true if the message was handed to the mail server.
    public boolean send(String to, String subject, String body) {
        if (!isConfigured()) {
            log.warn("Mail is not configured, so no email was sent.");
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            return true;
        } catch (Exception e) {
            // The reason from the mail server is logged, but never the key or the message body.
            log.warn("Email could not be sent: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }
}
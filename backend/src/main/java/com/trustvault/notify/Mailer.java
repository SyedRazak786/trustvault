package com.trustvault.notify;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

// Sends email through Brevo's HTTPS API. Port 443 is allowed on hosts that block SMTP ports.
// The message carries no file name and no document contents. A failed send is logged and never blocks sharing.
@Service
public class Mailer {

    private static final Logger log = LoggerFactory.getLogger(Mailer.class);

    private final RestClient client;
    private final String from;
    private final String apiKey;

    public Mailer(@Value("${trustvault.mail-from:}") String from,
                  @Value("${trustvault.mail-api-key:}") String apiKey) {
        this.client = RestClient.builder().baseUrl("https://api.brevo.com").build();
        this.from = from;
        this.apiKey = apiKey;
    }

    public boolean isConfigured() {
        return from != null && !from.isBlank() && apiKey != null && !apiKey.isBlank();
    }

    // Returns true if Brevo accepted the message.
    public boolean send(String to, String subject, String body) {
        if (!isConfigured()) {
            log.warn("Mail is not configured, so no email was sent.");
            return false;
        }
        try {
            client.post()
                    .uri("/v3/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "sender", Map.of("email", from),
                            "to", List.of(Map.of("email", to)),
                            "subject", subject,
                            "textContent", body))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            // Logs the reason from Brevo, never the key or the message body.
            log.warn("Email could not be sent: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }
}
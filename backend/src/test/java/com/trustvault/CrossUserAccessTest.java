package com.trustvault;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trustvault.auth.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "trustvault.storage-root=target/test-files")
class CrossUserAccessTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtService jwt;
    @Autowired PasswordEncoder encoder;

    private final List<Long> users = new ArrayList<>();

    // Removes only the users, documents and grants this test created. The demo accounts are not touched.
    @AfterEach
    void cleanUp() {
        for (long id : users) {
            jdbc.update("DELETE FROM grants WHERE user_id = ? OR document_id IN (SELECT id FROM documents WHERE owner_id = ?)", id, id);
            jdbc.update("DELETE FROM documents WHERE owner_id = ?", id);
            jdbc.update("DELETE FROM users WHERE id = ?", id);
        }
        users.clear();
    }

    @Test
    void strangerCannotDownloadAnotherUsersDocument() throws Exception {
        long owner = user("OWNER");
        long stranger = user("VIEWER");
        long doc = document(owner, "salary letter");

        mvc.perform(get("/api/documents/{id}/download", doc)
                        .header(HttpHeaders.AUTHORIZATION, token(stranger)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("salary letter"))));
    }

    @Test
    void strangerSeesNoDocumentsInTheList() throws Exception {
        long owner = user("OWNER");
        long stranger = user("VIEWER");
        document(owner, "salary letter");

        mvc.perform(get("/api/documents")
                        .header(HttpHeaders.AUTHORIZATION, token(stranger)))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
    }

    @Test
    void grantOnOneDocumentDoesNotOpenAnother() throws Exception {
        long owner = user("OWNER");
        long reader = user("VIEWER");
        long granted = document(owner, "granted letter");
        long other = document(owner, "other letter");
        grant(granted, reader, "READ", owner);

        mvc.perform(get("/api/documents/{id}/download", granted)
                        .header(HttpHeaders.AUTHORIZATION, token(reader)))
                .andExpect(status().isOk())
                .andExpect(content().string("granted letter"));

        mvc.perform(get("/api/documents/{id}/download", other)
                        .header(HttpHeaders.AUTHORIZATION, token(reader)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("other letter"))));
    }

    @Test
    void ownerCanDownloadOwnDocument() throws Exception {
        long owner = user("OWNER");
        long doc = document(owner, "owner letter");

        mvc.perform(get("/api/documents/{id}/download", doc)
                        .header(HttpHeaders.AUTHORIZATION, token(owner)))
                .andExpect(status().isOk());
    }

    @Test
    void revokedGrantStopsWorkingWithTheSameToken() throws Exception {
        long owner = user("OWNER");
        long reader = user("VIEWER");
        long doc = document(owner, "letter");
        grant(doc, reader, "READ", owner);
        String token = token(reader);

        mvc.perform(get("/api/documents/{id}/download", doc)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());

        jdbc.update("DELETE FROM grants WHERE document_id = ? AND user_id = ?", doc, reader);

        mvc.perform(get("/api/documents/{id}/download", doc)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }

    @Test
    void refusedAttemptIsRecordedAsDenied() throws Exception {
        long owner = user("OWNER");
        long stranger = user("VIEWER");
        long doc = document(owner, "letter");

        mvc.perform(get("/api/documents/{id}/download", doc)
                        .header(HttpHeaders.AUTHORIZATION, token(stranger)))
                .andExpect(status().isNotFound());

        Integer denied = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE actor_id = ? AND document_id = ? AND outcome = 'DENIED'",
                Integer.class, stranger, doc);
        assertEquals(1, denied);
    }

    // Helpers: create test users and documents, and issue tokens the same way the login does.

    private long user(String role) {
        String email = "test-" + UUID.randomUUID() + "@trustvault.test";
        jdbc.update("INSERT INTO users (email, password_hash, role, active) VALUES (?, ?, ?, TRUE)",
                email, encoder.encode("not-used-for-login"), role);
        long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        users.add(id);
        return id;
    }

    private long document(long ownerId, String text) throws Exception {
        String key = UUID.randomUUID().toString();
        Path dir = Path.of("target/test-files");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(key), text);
        jdbc.update("INSERT INTO documents (owner_id, name, category, storage_key, content_type, size_bytes) VALUES (?, 'test.txt', 'TEST', ?, 'text/plain', ?)",
                ownerId, key, text.length());
        return jdbc.queryForObject("SELECT id FROM documents WHERE storage_key = ?", Long.class, key);
    }

    private void grant(long docId, long userId, String permission, long grantedBy) {
        jdbc.update("INSERT INTO grants (document_id, user_id, permission, granted_by) VALUES (?, ?, ?, ?)",
                docId, userId, permission, grantedBy);
    }

    private String token(long userId) {
        return "Bearer " + jwt.issue(userId, "VIEWER");
    }
}
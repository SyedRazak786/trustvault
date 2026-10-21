package com.trustvault.documents;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.trustvault.access.AccessService;
import com.trustvault.access.AccessService.Permission;
import com.trustvault.audit.AuditService;
import com.trustvault.auth.AuthUser;
import com.trustvault.privacy.PrivacyScanner;
import com.trustvault.upload.UploadValidator;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final JdbcTemplate jdbc;
    private final AccessService access;
    private final AuditService audit;
    private final UploadValidator validator;
    private final PrivacyScanner privacy;
    private final Path storageRoot;

    public DocumentController(JdbcTemplate jdbc, AccessService access, AuditService audit,
                              UploadValidator validator, PrivacyScanner privacy,
                              @Value("${trustvault.storage-root}") String storageRoot) {
        this.jdbc = jdbc;
        this.access = access;
        this.audit = audit;
        this.validator = validator;
        this.privacy = privacy;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @GetMapping
    public List<Map<String, Object>> list(@AuthenticationPrincipal AuthUser user) {
        return access.listVisible(user.id());
    }

    // Owners only. The file type comes from the file's bytes, the stored name is a random key,
    // a SHA-256 fingerprint is recorded for File DNA, text is scanned for sensitive patterns (Privacy Guardian),
    // and every upload and refusal is audited.
    @PostMapping
    public ResponseEntity<?> upload(@AuthenticationPrincipal AuthUser user,
                                    @RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "category", required = false) String category)
            throws IOException, NoSuchAlgorithmException {

        if (!"OWNER".equals(user.role())) {
            audit.record(user.id(), "UPLOAD", null, false, "role " + user.role() + " may not upload");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only document owners can upload."));
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file was uploaded."));
        }
        if (file.getSize() > UploadValidator.MAX_BYTES) {
            audit.record(user.id(), "UPLOAD", null, false, "rejected: larger than limit");
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("error", "File is larger than 10 MB."));
        }

        byte[] data = file.getBytes();
        Optional<UploadValidator.Checked> type = validator.detect(data);
        if (type.isEmpty()) {
            audit.record(user.id(), "UPLOAD", null, false, "rejected: type not allowed");
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(Map.of("error", "Only PDF, PNG, JPEG and plain text files are accepted."));
        }

        String name = validator.displayName(file.getOriginalFilename(), type.get().extension());
        String safeCategory = validator.cleanCategory(category);

        // The fingerprint is calculated by the server from the bytes it saves, never from anything the browser sends.
        String fingerprint = fingerprint(data);

        // The storage name is a random UUID, never the uploaded name. The path must stay inside the storage root.
        String key = UUID.randomUUID().toString();
        Path target = storageRoot.resolve(key).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Files.createDirectories(storageRoot);
        Files.write(target, data, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

        jdbc.update("""
                INSERT INTO documents (owner_id, name, category, storage_key, content_type, size_bytes, sha256)
                VALUES (?, ?, ?, ?, ?, ?, ?)""",
                user.id(), name, safeCategory, key, type.get().contentType(), data.length, fingerprint);

        long id = jdbc.queryForObject("SELECT id FROM documents WHERE storage_key = ?", Long.class, key);
        audit.record(user.id(), "UPLOAD", id, true, "uploaded " + type.get().contentType());

        // Privacy Guardian: text uploads are scanned for sensitive patterns. The findings are shown to the
        // owner, who decides whether to share. Nothing is blocked, and the stored file is not changed.
        List<Map<String, Object>> warnings = new ArrayList<>();
        String scanNote;
        if ("txt".equals(type.get().extension())) {
            String text = new String(data, StandardCharsets.UTF_8);
            for (PrivacyScanner.Finding f : privacy.scan(text)) {
                warnings.add(Map.of("kind", f.kind(), "count", f.count()));
            }
            scanNote = "Text scanned for sensitive patterns.";
            if (!warnings.isEmpty()) {
                audit.record(user.id(), "PRIVACY_SCAN", id, true, "warnings: " + warnings.size() + " kind(s)");
            }
        } else {
            scanNote = "This file type cannot be scanned for text yet. Check its contents before sharing.";
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", id, "name", name, "contentType", type.get().contentType(),
                        "privacyNote", scanNote, "privacyWarnings", warnings));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<FileSystemResource> download(@AuthenticationPrincipal AuthUser user,
                                                       @PathVariable long id)
            throws IOException, NoSuchAlgorithmException {
        access.require(user.id(), id, Permission.READ, "DOWNLOAD");

        Map<String, Object> doc = jdbc.queryForMap(
                "SELECT storage_key, name, content_type, sha256 FROM documents WHERE id = ?", id);

        // The storage key is a server-generated UUID, never user input. The path must stay inside the storage root.
        Path file = storageRoot.resolve((String) doc.get("storage_key")).normalize();
        if (!file.startsWith(storageRoot) || !file.toFile().isFile()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        // File DNA: the bytes on disk must still match the fingerprint recorded at upload.
        // A mismatch means the stored file changed, so the download is refused and the event is recorded.
        String expected = (String) doc.get("sha256");
        if (expected != null && !fingerprint(Files.readAllBytes(file)).equals(expected)) {
            audit.record(user.id(), "DOWNLOAD", id, false, "file integrity check failed");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        audit.record(user.id(), "DOWNLOAD", id, true, "downloaded");

        // Smart File Capsule: count this download against the recipient's grant, so a download limit
        // is enforced on the server. Owners have no grant row, so this changes nothing for them.
        jdbc.update("UPDATE grants SET downloads_used = downloads_used + 1 WHERE document_id = ? AND user_id = ?",
                id, user.id());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType((String) doc.get("content_type")))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename((String) doc.get("name")).build().toString())
                .body(new FileSystemResource(file));
    }

    // File DNA, visible to anyone allowed to read the document: recalculates the fingerprint of the stored file
    // and compares it with the one recorded at upload. It changes nothing and does not count as a download.
    @GetMapping("/{id}/verify")
    public ResponseEntity<?> verify(@AuthenticationPrincipal AuthUser user, @PathVariable long id)
            throws IOException, NoSuchAlgorithmException {
        access.require(user.id(), id, Permission.READ, "VERIFY");

        Map<String, Object> doc = jdbc.queryForMap(
                "SELECT storage_key, sha256 FROM documents WHERE id = ?", id);
        Path file = storageRoot.resolve((String) doc.get("storage_key")).normalize();
        if (!file.startsWith(storageRoot) || !file.toFile().isFile()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        String expected = (String) doc.get("sha256");
        boolean intact = expected != null && fingerprint(Files.readAllBytes(file)).equals(expected);
        String result = expected == null ? "no fingerprint recorded (uploaded before File DNA)"
                : intact ? "matches the original upload" : "changed since upload";

        audit.record(user.id(), "VERIFY", id, intact, "file fingerprint " + result);
        return ResponseEntity.ok(Map.of("documentId", id, "intact", intact, "result", result));
    }

    // Owner only. Soft delete: the document disappears from every list and download,
    // but its record and file stay so the audit trail still points to something real.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser user, @PathVariable long id) {
        access.requireOwner(user.id(), id, "DELETE");

        jdbc.update("UPDATE documents SET deleted = TRUE WHERE id = ?", id);

        audit.record(user.id(), "DELETE", id, true, "soft-deleted");
        return ResponseEntity.noContent().build();
    }

    private static String fingerprint(byte[] data) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }
}
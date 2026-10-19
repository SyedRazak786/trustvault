package com.trustvault.documents;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.trustvault.access.AccessService;
import com.trustvault.access.AccessService.Permission;
import com.trustvault.audit.AuditService;
import com.trustvault.auth.AuthUser;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final JdbcTemplate jdbc;
    private final AccessService access;
    private final AuditService audit;
    private final Path storageRoot;

    public DocumentController(JdbcTemplate jdbc, AccessService access, AuditService audit,
                              @Value("${trustvault.storage-root}") String storageRoot) {
        this.jdbc = jdbc;
        this.access = access;
        this.audit = audit;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @GetMapping
    public List<Map<String, Object>> list(@AuthenticationPrincipal AuthUser user) {
        return access.listVisible(user.id());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<FileSystemResource> download(@AuthenticationPrincipal AuthUser user,
                                                       @PathVariable long id) {
        access.require(user.id(), id, Permission.READ, "DOWNLOAD");

        Map<String, Object> doc = jdbc.queryForMap(
                "SELECT storage_key, name, content_type FROM documents WHERE id = ?", id);

        // The storage key is a server-generated UUID, never user input. The path must stay inside the storage root.
        Path file = storageRoot.resolve((String) doc.get("storage_key")).normalize();
        if (!file.startsWith(storageRoot) || !file.toFile().isFile()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        audit.record(user.id(), "DOWNLOAD", id, true, "downloaded");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType((String) doc.get("content_type")))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename((String) doc.get("name")).build().toString())
                .body(new FileSystemResource(file));
    }
}
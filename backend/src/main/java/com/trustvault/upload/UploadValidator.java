package com.trustvault.upload;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class UploadValidator {

    public static final long MAX_BYTES = 10L * 1024 * 1024;

    private static final int MAX_NAME_LENGTH = 200;
    private static final Pattern UNSAFE = Pattern.compile("[^A-Za-z0-9 ._()-]");

    public record Checked(String contentType, String extension) {}

    // Decides the type from the file's own bytes. Anything not recognised is refused.
    public Optional<Checked> detect(byte[] data) {
        if (data.length == 0) {
            return Optional.empty();
        }
        if (startsWith(data, 0x25, 0x50, 0x44, 0x46, 0x2D)) {                  // %PDF-
            return Optional.of(new Checked("application/pdf", "pdf"));
        }
        if (startsWith(data, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) { // PNG
            return Optional.of(new Checked("image/png", "png"));
        }
        if (startsWith(data, 0xFF, 0xD8, 0xFF)) {                              // JPEG
            return Optional.of(new Checked("image/jpeg", "jpg"));
        }
        if (isPlainText(data)) {
            return Optional.of(new Checked("text/plain", "txt"));
        }
        return Optional.empty();
    }

    // A safe display name: no folders, no unusual characters, and the extension matches the detected type.
    public String displayName(String original, String extension) {
        String name = original == null ? "" : original;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = UNSAFE.matcher(name).replaceAll("_").trim();

        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        if (base.isBlank()) {
            base = "document";
        }
        int maxBase = MAX_NAME_LENGTH - extension.length() - 1;
        if (base.length() > maxBase) {
            base = base.substring(0, maxBase);
        }
        return base + "." + extension;
    }

    public String cleanCategory(String category) {
        if (category == null || category.isBlank()) {
            return "GENERAL";
        }
        String cleaned = UNSAFE.matcher(category).replaceAll("_").trim();
        return cleaned.length() > 100 ? cleaned.substring(0, 100) : cleaned;
    }

    private static boolean startsWith(byte[] data, int... signature) {
        if (data.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((data[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    // Plain text: no NUL bytes and valid UTF-8. Binary data is refused.
    private static boolean isPlainText(byte[] data) {
        for (byte b : data) {
            if (b == 0) {
                return false;
            }
        }
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(data));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }
}
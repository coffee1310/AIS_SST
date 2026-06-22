package org.example.ais_sst.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.config.PdfStorageProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@RestController
@RequestMapping("/api/terms")
@Slf4j
public class TermsController {

    private static final String TERMS_FILE_NAME = "terms-of-service.pdf";
    private final PdfStorageProperties storageProperties;

    public TermsController(PdfStorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @GetMapping(produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> getTermsOfService(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            @RequestHeader(value = "If-Modified-Since", required = false) String ifModifiedSince) {

        try {
            Path termsPath = Paths.get(storageProperties.getPath(), TERMS_FILE_NAME);

            if (!Files.exists(termsPath)) {
                log.warn("Terms of service PDF not found at: {}", termsPath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Получаем информацию о файле
            FileTime fileTime = Files.getLastModifiedTime(termsPath);
            long lastModified = fileTime.toMillis();

            // Генерируем ETag
            String eTag = generateETag(termsPath);

            // Проверяем If-None-Match
            if (eTag.equals(ifNoneMatch)) {
                log.debug("Returning 304 Not Modified for terms (ETag match)");
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }

            // Проверяем If-Modified-Since
            if (ifModifiedSince != null) {
                try {
                    // Парсим дату из HTTP заголовка
                    Instant ifModifiedSinceInstant = Instant.from(
                            DateTimeFormatter.RFC_1123_DATE_TIME.parse(ifModifiedSince)
                    );
                    long ifModifiedSinceTime = ifModifiedSinceInstant.toEpochMilli();

                    // Округляем до секунд как в HTTP
                    if (lastModified / 1000 <= ifModifiedSinceTime / 1000) {
                        log.debug("Returning 304 Not Modified for terms (If-Modified-Since)");
                        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse If-Modified-Since header: {}", ifModifiedSince);
                }
            }

            Resource resource = new FileSystemResource(termsPath);
            long contentLength = Files.size(termsPath);

            log.info("Serving terms of service PDF, size: {} bytes, last modified: {}",
                    contentLength, Instant.ofEpochMilli(lastModified));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"terms-of-service.pdf\"")
                    .header(HttpHeaders.ETAG, eTag)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600, must-revalidate")
                    .header(HttpHeaders.LAST_MODIFIED,
                            DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant.ofEpochMilli(lastModified)))
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(contentLength)
                    .body(resource);

        } catch (IOException e) {
            log.error("Error serving terms of service PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String generateETag(Path path) throws IOException {
        // Используем комбинацию размера и времени модификации
        FileTime lastModified = Files.getLastModifiedTime(path);
        long size = Files.size(path);
        String data = size + "_" + lastModified.toMillis();

        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return "\"" + Base64.getEncoder().encodeToString(hash) + "\"";
        } catch (NoSuchAlgorithmException e) {
            // Fallback
            return "\"" + data.hashCode() + "\"";
        }
    }
}
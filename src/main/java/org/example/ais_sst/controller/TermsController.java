package org.example.ais_sst.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

@RestController
@RequestMapping("/api")
@Slf4j
public class TermsController {

    private static final String TERMS_FILE_NAME = "terms-of-service.pdf";

    // Формат для HTTP заголовка Last-Modified (RFC 1123)
    private static final DateTimeFormatter RFC_1123_FORMATTER =
            DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US);

    @Value("${pdf.storage.path:/app/storage/terms}")
    private String storagePath;

    @GetMapping(value = "/terms", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> getTermsOfService(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            @RequestHeader(value = "If-Modified-Since", required = false) String ifModifiedSince) {

        try {
            Path termsPath = Paths.get(storagePath, TERMS_FILE_NAME);

            if (!Files.exists(termsPath)) {
                log.warn("Terms of service PDF not found at: {}", termsPath.toAbsolutePath());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            FileTime fileTime = Files.getLastModifiedTime(termsPath);
            long lastModifiedMillis = fileTime.toMillis();
            String eTag = generateETag(termsPath);

            // Проверяем If-None-Match
            if (eTag.equals(ifNoneMatch)) {
                log.debug("Returning 304 Not Modified for terms (ETag match)");
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }

            // Проверяем If-Modified-Since
            if (ifModifiedSince != null) {
                try {
                    // Парсим дату из заголовка
                    Instant ifModifiedSinceInstant = Instant.from(
                            DateTimeFormatter.RFC_1123_DATE_TIME.parse(ifModifiedSince)
                    );
                    long ifModifiedSinceTime = ifModifiedSinceInstant.toEpochMilli();

                    // Округляем до секунд как в HTTP
                    if (lastModifiedMillis / 1000 <= ifModifiedSinceTime / 1000) {
                        log.debug("Returning 304 Not Modified for terms (If-Modified-Since)");
                        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse If-Modified-Since header: {}", ifModifiedSince);
                }
            }

            Resource resource = new FileSystemResource(termsPath);
            long contentLength = Files.size(termsPath);

            // Правильное форматирование для Last-Modified
            String lastModifiedHeader = RFC_1123_FORMATTER.format(
                    Instant.ofEpochMilli(lastModifiedMillis).atZone(ZoneId.of("GMT"))
            );

            log.info("Serving terms of service PDF from: {}, size: {} bytes, last modified: {}",
                    termsPath.toAbsolutePath(), contentLength, lastModifiedHeader);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"terms-of-service.pdf\"")
                    .header(HttpHeaders.ETAG, eTag)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600, must-revalidate")
                    .header(HttpHeaders.LAST_MODIFIED, lastModifiedHeader)
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(contentLength)
                    .body(resource);

        } catch (IOException e) {
            log.error("Error serving terms of service PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String generateETag(Path path) throws IOException {
        FileTime lastModified = Files.getLastModifiedTime(path);
        long size = Files.size(path);
        String data = size + "_" + lastModified.toMillis();

        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return "\"" + Base64.getEncoder().encodeToString(hash) + "\"";
        } catch (NoSuchAlgorithmException e) {
            return "\"" + data.hashCode() + "\"";
        }
    }
}
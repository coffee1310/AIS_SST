package org.example.ais_sst.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@ConfigurationProperties(prefix = "pdf.storage")
@Data
@Slf4j
public class PdfStorageProperties {
    private String path = "/app/storage/terms"; // значение по умолчанию

    @PostConstruct
    public void init() {
        try {
            Path storagePath = Paths.get(path);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
                log.info("Created PDF storage directory: {}", storagePath.toAbsolutePath());
            }
            log.info("PDF storage path initialized: {}", storagePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Could not create PDF storage directory at: {}", path, e);
            // Не бросаем исключение, чтобы приложение запустилось даже если директорию нельзя создать
        }
    }
}
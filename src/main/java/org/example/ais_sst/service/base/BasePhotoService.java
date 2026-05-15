package org.example.ais_sst.service.base;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@Slf4j
public abstract class BasePhotoService {

    @Value("${app.upload.dir:uploads}")
    protected String uploadDir;

    protected String savePhotoFromBase64(String base64Photo, Long entityId, String entityType) throws IOException {
        if (base64Photo == null || base64Photo.isEmpty()) {
            return null;
        }

        String base64Data = base64Photo.contains(",")
                ? base64Photo.substring(base64Photo.indexOf(",") + 1)
                : base64Photo;

        byte[] photoBytes = Base64.getDecoder().decode(base64Data);

        Path uploadPath = Paths.get(uploadDir, entityType);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = String.format("%s_%d_%d.jpg", entityType, entityId, System.currentTimeMillis());
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, photoBytes);

        log.info("Photo saved: {}", filePath.toString());
        return filePath.toString();
    }

    protected String getPhotoAsBase64(String photoPath) {
        if (photoPath == null || photoPath.isEmpty()) {
            return null;
        }

        try {
            Path path = Paths.get(photoPath);
            if (!Files.exists(path)) {
                log.warn("Photo file not found: {}", photoPath);
                return null;
            }

            byte[] photoBytes = Files.readAllBytes(path);
            return Base64.getEncoder().encodeToString(photoBytes);
        } catch (IOException e) {
            log.error("Failed to read photo: {}", photoPath, e);
            return null;
        }
    }

    protected void deletePhoto(String photoPath) throws IOException {
        if (photoPath == null || photoPath.isEmpty()) {
            return;
        }

        Path path = Paths.get(photoPath);
        if (Files.exists(path)) {
            Files.delete(path);
            log.info("Deleted photo: {}", photoPath);
        }
    }
}
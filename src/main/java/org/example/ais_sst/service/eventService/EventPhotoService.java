package org.example.ais_sst.service.eventService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
public class EventPhotoService {

    @Value("${app.upload.dir:uploads/events}")
    private String uploadDir;

    public String savePhotoFromBase64(String base64Photo) throws IOException {
        if (base64Photo == null || base64Photo.isEmpty()) {
            log.warn("Base64 photo is null or empty");
            return null;
        }

        log.info("Saving photo, base64 length: {}", base64Photo.length());

        String base64Data = base64Photo;
        if (base64Photo.contains(",")) {
            base64Data = base64Photo.split(",")[1];
            log.info("Removed data:image prefix, new length: {}", base64Data.length());
        }

        byte[] photoBytes = Base64.getDecoder().decode(base64Data);
        log.info("Decoded photo bytes length: {}", photoBytes.length);

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
        }

        String fileName = UUID.randomUUID().toString() + ".jpg";
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, photoBytes);

        String savedPath = "/" + uploadDir + "/" + fileName;
        log.info("Photo saved at: {}, absolute path: {}", savedPath, filePath.toAbsolutePath());

        return savedPath;
    }

    public String getPhotoAsBase64(String photoPath) {
        if (photoPath == null || photoPath.isEmpty()) {
            log.warn("Photo path is null or empty");
            return null;
        }

        log.info("Reading photo from path: {}", photoPath);

        try {
            String cleanPath = photoPath.startsWith("/") ? photoPath.substring(1) : photoPath;
            Path filePath = Paths.get(cleanPath);

            log.info("Absolute file path: {}", filePath.toAbsolutePath());

            if (!Files.exists(filePath)) {
                log.warn("Photo file not found: {}", filePath.toAbsolutePath());
                return null;
            }

            byte[] photoBytes = Files.readAllBytes(filePath);
            log.info("Read photo bytes length: {}", photoBytes.length);

            String base64 = Base64.getEncoder().encodeToString(photoBytes);
            log.info("Converted to base64, length: {}", base64.length());

            return base64;
        } catch (IOException e) {
            log.error("Failed to read photo: {}", photoPath, e);
            return null;
        }
    }

    public void deletePhoto(String photoPath) throws IOException {
        if (photoPath == null || photoPath.isEmpty()) {
            return;
        }

        String cleanPath = photoPath.startsWith("/") ? photoPath.substring(1) : photoPath;
        Path filePath = Paths.get(cleanPath);

        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("Photo deleted: {}", photoPath);
        }
    }
}
package org.example.ais_sst.service.sectorService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@Slf4j
@Service
public class SectorPhotoService {

    @Value("${app.upload.sectors-dir:uploads/sectors}")
    private String uploadDir;

    public String savePhotoFromBase64(String base64Photo, Long sectorId) throws IOException {
        if (base64Photo == null || base64Photo.isEmpty()) {
            return null;
        }

        String base64Data = base64Photo;
        if (base64Photo.contains(",")) {
            base64Data = base64Photo.split(",")[1];
        }

        byte[] photoBytes = Base64.getDecoder().decode(base64Data);

        Path sectorDir = Paths.get(uploadDir, String.valueOf(sectorId));
        if (!Files.exists(sectorDir)) {
            Files.createDirectories(sectorDir);
            log.info("Created directory for sector {}: {}", sectorId, sectorDir);
        }

        String fileName = "sector.jpg";
        Path filePath = sectorDir.resolve(fileName);
        Files.write(filePath, photoBytes);

        String relativePath = uploadDir + "/" + sectorId + "/" + fileName;
        log.info("Photo saved for sector {} at: {}", sectorId, relativePath);

        return relativePath;
    }

    public String getPhotoAsBase64(String photoPath) {
        if (photoPath == null || photoPath.isEmpty()) {
            return null;
        }

        try {
            Path filePath = Paths.get(photoPath);
            if (!Files.exists(filePath)) {
                log.warn("Photo file not found: {}", filePath.toAbsolutePath());
                return null;
            }
            byte[] photoBytes = Files.readAllBytes(filePath);
            return Base64.getEncoder().encodeToString(photoBytes);
        } catch (IOException e) {
            log.error("Failed to read photo: {}", photoPath, e);
            return null;
        }
    }

    public void deletePhoto(String photoPath) {
        if (photoPath == null || photoPath.isEmpty()) {
            return;
        }

        try {
            Path filePath = Paths.get(photoPath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Photo deleted: {}", photoPath);

                // Удаляем директорию если она пуста
                Path parentDir = filePath.getParent();
                if (Files.exists(parentDir) && Files.list(parentDir).count() == 0) {
                    Files.delete(parentDir);
                    log.info("Empty directory deleted: {}", parentDir);
                }
            }
        } catch (IOException e) {
            log.error("Failed to delete photo: {}", photoPath, e);
        }
    }
}
package org.example.ais_sst.service.accountCreatingRequestsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@Slf4j
@Service
public class AccountRequestPhotoService {

    @Value("${app.upload.account-requests-dir:uploads/account_creating_requests_avatars}")
    private String uploadDir;

    public String savePhotoFromBase64(String base64Photo, Long requestId) throws IOException {
        if (base64Photo == null || base64Photo.isEmpty()) {
            return null;
        }

        String base64Data = base64Photo;
        if (base64Photo.contains(",")) {
            base64Data = base64Photo.split(",")[1];
        }

        byte[] photoBytes = Base64.getDecoder().decode(base64Data);

        Path requestDir = Paths.get(uploadDir, String.valueOf(requestId));
        if (!Files.exists(requestDir)) {
            Files.createDirectories(requestDir);
        }

        String fileName = "avatar.jpg";
        Path filePath = requestDir.resolve(fileName);
        Files.write(filePath, photoBytes);

        return uploadDir + "/" + requestId + "/" + fileName;
    }

    public String getPhotoAsBase64(String photoPath) {
        if (photoPath == null || photoPath.isEmpty()) {
            return null;
        }

        try {
            Path filePath = Paths.get(photoPath);
            if (!Files.exists(filePath)) {
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

                Path parentDir = filePath.getParent();
                if (Files.exists(parentDir) && Files.list(parentDir).count() == 0) {
                    Files.delete(parentDir);
                }
            }
        } catch (IOException e) {
            log.error("Failed to delete photo: {}", photoPath, e);
        }
    }
}
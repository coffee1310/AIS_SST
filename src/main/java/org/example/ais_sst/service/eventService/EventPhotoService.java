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

    /**
     * Сохранение фото из Base64
     */
    public String savePhotoFromBase64(String base64Photo) throws IOException {
        if (base64Photo == null || base64Photo.isEmpty()) {
            return null;
        }

        // Убираем префикс data:image/png;base64, если есть
        String base64Data = base64Photo;
        if (base64Photo.contains(",")) {
            base64Data = base64Photo.split(",")[1];
        }

        // Декодируем Base64 в байты
        byte[] photoBytes = Base64.getDecoder().decode(base64Data);

        // Создаем директорию если не существует
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Генерируем уникальное имя файла
        String fileName = UUID.randomUUID().toString() + ".jpg";
        Path filePath = uploadPath.resolve(fileName);

        // Сохраняем файл
        Files.write(filePath, photoBytes);

        String savedPath = "/" + uploadDir + "/" + fileName;
        log.info("Photo saved at: {}", savedPath);

        return savedPath;
    }

    /**
     * Удаление фото
     */
    public void deletePhoto(String photoPath) throws IOException {
        if (photoPath == null || photoPath.isEmpty()) {
            return;
        }

        // Убираем ведущий слэш если есть
        String cleanPath = photoPath.startsWith("/") ? photoPath.substring(1) : photoPath;
        Path filePath = Paths.get(cleanPath);

        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("Photo deleted: {}", photoPath);
        }
    }
}
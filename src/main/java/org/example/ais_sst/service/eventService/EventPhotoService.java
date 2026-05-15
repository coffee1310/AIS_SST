package org.example.ais_sst.service.eventService;

import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.controller.base.PhotoService;
import org.example.ais_sst.service.base.BasePhotoService;
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
public class EventPhotoService extends BasePhotoService implements PhotoService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public String savePhotoFromBase64(String base64Photo) throws IOException {
        return savePhotoFromBase64(base64Photo, System.currentTimeMillis(), "events");
    }

    @Override
    public String savePhotoFromBase64(String base64Photo, Long entityId) throws IOException {
        return savePhotoFromBase64(base64Photo, entityId, "events");
    }

    @Override
    public String getPhotoAsBase64(String photoPath) {
        return getPhotoAsBase64(photoPath);
    }

    @Override
    public void deletePhoto(String photoPath) throws IOException {
        deletePhoto(photoPath);
    }
}
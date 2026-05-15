package org.example.ais_sst.controller.base;

import java.io.IOException;

public interface PhotoService {
    String savePhotoFromBase64(String base64Photo, Long entityId) throws IOException;
    String getPhotoAsBase64(String photoPath);
    void deletePhoto(String photoPath) throws IOException;
}
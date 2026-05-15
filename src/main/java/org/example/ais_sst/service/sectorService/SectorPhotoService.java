package org.example.ais_sst.service.sectorService;

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

@Slf4j
@Service
public class SectorPhotoService extends BasePhotoService implements PhotoService {

    public String savePhotoFromBase64(String base64Photo, Long sectorId) throws IOException {
        return savePhotoFromBase64(base64Photo, sectorId, "sectors");
    }

    @Override
    public String getPhotoAsBase64(String photoPath) {
        return super.getPhotoAsBase64(photoPath);
    }

    @Override
    public void deletePhoto(String photoPath) throws IOException {
        super.deletePhoto(photoPath);
    }
}
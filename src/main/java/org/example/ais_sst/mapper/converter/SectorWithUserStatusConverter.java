package org.example.ais_sst.mapper.converter;

import lombok.RequiredArgsConstructor;
import org.example.ais_sst.dto.sector.SectorWithUserStatusDTO;
import org.example.ais_sst.service.userService.UserPhotoService;
import org.example.ais_sst.utils.ImageUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class SectorWithUserStatusConverter {

    private final UserPhotoService userPhotoService;

    public SectorWithUserStatusDTO fromNativeQuery(Object[] row, UserPhotoService photoService) {
        if (row == null || row.length < 9) {  // Теперь полей меньше
            return null;
        }

        // Фото сектора
        String sectorPhotoBase64 = null;
        Object sectorPhotoObj = row[6];
        if (sectorPhotoObj != null) {
            if (sectorPhotoObj instanceof byte[]) {
                byte[] sectorPhotoBytes = (byte[]) sectorPhotoObj;
                sectorPhotoBase64 = sectorPhotoBytes.length > 0 ? ImageUtil.encodeToBase64(sectorPhotoBytes) : null;
            } else if (sectorPhotoObj instanceof String) {
                String sectorPhotoPath = (String) sectorPhotoObj;
                sectorPhotoBase64 = photoService != null ? photoService.getPhotoAsBase64(sectorPhotoPath) : null;
            }
        }

        return SectorWithUserStatusDTO.builder()
                .id(((Number) row[0]).longValue())
                .title((String) row[1])
                .description((String) row[2])
                .isParticipant((Boolean) row[3])
                .hasActiveRequest((Boolean) row[4])
                .isCoordinator((Boolean) row[5])
                .photo(sectorPhotoBase64)
                .requestStatus((String) row[7])
                .participantCount(((Number) row[8]).intValue())
                .coordinators(new ArrayList<>())  // Пустой список, заполнится позже
                .build();
    }

    public SectorWithUserStatusDTO fromNativeQuery(Object[] row) {
        return fromNativeQuery(row, userPhotoService);
    }
}
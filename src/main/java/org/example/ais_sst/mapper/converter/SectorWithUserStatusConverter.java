package org.example.ais_sst.mapper.converter;

import org.example.ais_sst.dto.sector.SectorWithUserStatusDTO;
import org.example.ais_sst.utils.ImageUtil;
import org.springframework.stereotype.Component;

@Component
public class SectorWithUserStatusConverter {

    public SectorWithUserStatusDTO fromNativeQuery(Object[] row) {
        if (row == null || row.length < 13) {  // Теперь 13 полей
            return null;
        }

        // Фото сектора
        byte[] sectorPhotoBytes = (byte[]) row[6];
        String sectorPhotoBase64 = sectorPhotoBytes != null && sectorPhotoBytes.length > 0
                ? ImageUtil.encodeToBase64(sectorPhotoBytes) : null;

        // Фото координатора
        byte[] coordinatorPhotoBytes = (byte[]) row[12];
        String coordinatorPhotoBase64 = coordinatorPhotoBytes != null && coordinatorPhotoBytes.length > 0
                ? ImageUtil.encodeToBase64(coordinatorPhotoBytes) : null;

        // Полное ФИО координатора
        String coordinatorName = (String) row[9];
        String coordinatorSurname = (String) row[10];
        String coordinatorPatronymic = (String) row[11];
        String coordinatorFullName = null;

        if (coordinatorName != null || coordinatorSurname != null) {
            coordinatorFullName = (coordinatorSurname != null ? coordinatorSurname : "") + " " +
                    (coordinatorName != null ? coordinatorName : "") + " " +
                    (coordinatorPatronymic != null ? coordinatorPatronymic : "");
            coordinatorFullName = coordinatorFullName.trim();
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
                .coordinatorName(coordinatorName)
                .coordinatorSurname(coordinatorSurname)
                .coordinatorPatronymic(coordinatorPatronymic)
                .coordinatorFullName(coordinatorFullName)
                .coordinatorPhoto(coordinatorPhotoBase64)
                .build();
    }
}
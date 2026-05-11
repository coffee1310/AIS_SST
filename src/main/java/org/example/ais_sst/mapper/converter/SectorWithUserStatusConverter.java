package org.example.ais_sst.mapper.converter;

import lombok.RequiredArgsConstructor;
import org.example.ais_sst.dto.sector.SectorWithUserStatusDTO;
import org.example.ais_sst.service.userService.UserPhotoService;
import org.example.ais_sst.utils.ImageUtil;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SectorWithUserStatusConverter {

    private final UserPhotoService userPhotoService;

    // Основной метод с UserPhotoService
    public SectorWithUserStatusDTO fromNativeQuery(Object[] row, UserPhotoService photoService) {
        if (row == null || row.length < 13) {
            return null;
        }

        // Фото сектора - теперь путь
        String sectorPhotoPath = row[6] != null ? (String) row[6] : null;
        String sectorPhotoBase64 = photoService != null && sectorPhotoPath != null
                ? photoService.getPhotoAsBase64(sectorPhotoPath) : null;

        // Фото координатора - теперь путь
        String coordinatorPhotoPath = row[12] != null ? (String) row[12] : null;
        String coordinatorPhotoBase64 = photoService != null && coordinatorPhotoPath != null
                ? photoService.getPhotoAsBase64(coordinatorPhotoPath) : null;

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

    // Метод без параметров для обратной совместимости
    public SectorWithUserStatusDTO fromNativeQuery(Object[] row) {
        return fromNativeQuery(row, userPhotoService);
    }
}
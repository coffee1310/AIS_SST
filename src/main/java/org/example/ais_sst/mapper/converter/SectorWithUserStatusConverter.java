package org.example.ais_sst.mapper.converter;

import org.example.ais_sst.dto.sector.SectorWithUserStatusDTO;
import org.example.ais_sst.utils.ImageUtil;
import org.springframework.stereotype.Component;

@Component
public class SectorWithUserStatusConverter {

    public SectorWithUserStatusDTO fromNativeQuery(Object[] row) {
        if (row == null || row.length < 8) {
            return null;
        }

        // Получаем фото как byte[] и конвертируем в Base64
        byte[] photoBytes = (byte[]) row[6]; // Индекс для фото
        String photoBase64 = photoBytes != null ? ImageUtil.encodeToBase64(photoBytes) : null;

        return SectorWithUserStatusDTO.builder()
                .id(((Number) row[0]).longValue())
                .title((String) row[1])
                .description((String) row[2])
                .isParticipant((Boolean) row[3])
                .hasActiveRequest((Boolean) row[4])
                .isCoordinator((Boolean) row[5])
                .requestStatus((String) row[7])  // status на индексе 7
                .participantCount(((Number) row[8]).intValue())
                .photo(photoBase64)  // Добавляем фото
                .build();
    }
}
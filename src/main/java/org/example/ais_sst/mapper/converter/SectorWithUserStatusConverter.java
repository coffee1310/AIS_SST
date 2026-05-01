package org.example.ais_sst.mapper.converter;

import org.example.ais_sst.dto.sector.SectorWithUserStatusDTO;
import org.springframework.stereotype.Component;

@Component
public class SectorWithUserStatusConverter {

    public SectorWithUserStatusDTO fromNativeQuery(Object[] row) {
        if (row == null || row.length < 8) {  // Теперь 8 полей
            return null;
        }

        return SectorWithUserStatusDTO.builder()
                .id(((Number) row[0]).longValue())
                .title((String) row[1])
                .description((String) row[2])
                .isParticipant((Boolean) row[3])
                .hasActiveRequest((Boolean) row[4])
                .isCoordinator((Boolean) row[5])  // Добавленное поле
                .requestStatus((String) row[6])
                .participantCount(((Number) row[7]).intValue())
                .build();
    }
}
package org.example.ais_sst.mapper.converter;

import org.example.ais_sst.dto.sector.SectorWithUserStatusDTO;
import org.springframework.stereotype.Component;

@Component
public class SectorWithUserStatusConverter {

    public SectorWithUserStatusDTO fromNativeQuery(Object[] row) {
        if (row == null || row.length < 7) {  // Теперь 7 полей
            return null;
        }

        return SectorWithUserStatusDTO.builder()
                .id(((Number) row[0]).longValue())
                .title((String) row[1])
                .description((String) row[2])
                .isParticipant((Boolean) row[3])
                .hasActiveRequest((Boolean) row[4])
                .requestStatus((String) row[5])  // Статус заявки
                .participantCount(((Number) row[6]).intValue())
                .build();
    }
}
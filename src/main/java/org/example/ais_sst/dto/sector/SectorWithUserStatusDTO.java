package org.example.ais_sst.dto.sector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ais_sst.entity.Sector;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectorWithUserStatusDTO {
    private Long id;
    private String title;
    private String description;
    private Boolean isParticipant;
    private Boolean isCoordinator;  // Добавленное поле
    private Boolean hasActiveRequest;
    private String requestStatus;
    private Integer participantCount;
    private String photo;  // Base64 строка
}
package org.example.ais_sst.dto.event_roles_application;

import lombok.Builder;
import lombok.Data;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Builder
public class RoleApplicationFilterDTO {
    private Long id;
    private Long studentId;
    private Long eventRoleId;
    private Long eventId;
    private RoleApplicationStatuses status;
    private Boolean isReserve;
    private Long sectorParticipantId;
    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateTo;

    private Long currentUserId;
}
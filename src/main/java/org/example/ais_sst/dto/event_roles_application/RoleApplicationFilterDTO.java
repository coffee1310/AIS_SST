package org.example.ais_sst.dto.event_roles_application;

import lombok.Builder;
import lombok.Data;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;

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
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
}
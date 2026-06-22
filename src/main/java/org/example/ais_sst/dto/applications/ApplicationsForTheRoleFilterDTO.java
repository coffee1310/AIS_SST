package org.example.ais_sst.dto.applications;

import lombok.Builder;
import lombok.Data;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ApplicationsForTheRoleFilterDTO {
    private Long id;
    private Long sectorParticipantId;
    private Long eventRoleId;
    private Long sectorId;
    private Long eventId;
    private Long userId;
    private Boolean isReserve;
    private RoleApplicationStatuses status;
    private String rejectionReason;
    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdAtFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdAtTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime updatedAtFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime updatedAtTo;
}
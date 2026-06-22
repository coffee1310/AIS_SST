package org.example.ais_sst.dto.applications;

import lombok.Builder;
import lombok.Data;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;

import java.time.LocalDateTime;

@Data
@Builder
public class ApplicationsForTheRoleResponseDTO {
    private Long id;

    // Информация о пользователе (ФИО)
    private Long userId;
    private String userFullName;
    private String userEmail;

    // Информация о секторе
    private Long sectorId;
    private String sectorTitle;

    // Информация о роли
    private Long eventRoleId;
    private String eventRoleTitle;
    private Long eventId;
    private String eventTitle;

    // Информация о заявке
    private Boolean isReserve;
    private RoleApplicationStatuses status;
    private String rejectionReason;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Дополнительная информация о сектор-участнике
    private Long sectorParticipantId;
    private String sectorParticipantStatus;
}
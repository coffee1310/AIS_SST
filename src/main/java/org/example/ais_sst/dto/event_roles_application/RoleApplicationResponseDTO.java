package org.example.ais_sst.dto.event_roles_application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleApplicationResponseDTO {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentSurname;
    private String studentPatronymic;
    private String studentEmail;
    private Long eventRoleId;
    private String eventRoleName;
    private Long eventId;
    private String eventTitle;
    private Boolean isReserve;
    private RoleApplicationStatuses status;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

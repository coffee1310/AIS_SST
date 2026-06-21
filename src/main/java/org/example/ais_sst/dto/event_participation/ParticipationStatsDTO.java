package org.example.ais_sst.dto.event_participation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationStatsDTO {
    private Long eventId;
    private String eventTitle;
    private Integer totalParticipants;
    private Integer presentParticipants;
    private Integer absentParticipants;
    private Integer totalOrganizers;
    private Integer presentOrganizers;
    private Integer absentOrganizers;
    private Integer totalRoles;
    private Integer presentRoles;
    private Integer absentRoles;
    private Integer totalParticipantPoints;
    private Integer totalOrganizerPoints;
    private Integer totalRolePoints;
    private Integer totalPoints;

    private List<RoleStatsDTO> roleStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleStatsDTO {
        private Long roleId;
        private String roleTitle;
        private Integer totalRecords;
        private Integer presentRecords;
        private Integer absentRecords;
        private Integer totalPoints;
    }
}
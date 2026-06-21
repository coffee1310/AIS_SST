package org.example.ais_sst.dto.event_participation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventParticipantInfoDTO {
    private Long id;
    private String role;           // "ОРГАНИЗАТОР", "УЧАСТНИК", или название роли из GlobalEventRole
    private String fullName;       // ФИО
    private Integer totalPoints;   // Баллы (может быть null)
    private Boolean wasPresent;    // Присутствие
    private String entityType;     // "ORGANIZER", "PARTICIPANT", "PARTICIPATION_RECORD"
}
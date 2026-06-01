package org.example.ais_sst.dto.event_roles;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRoleResponseDTO {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private Long globalEventRoleId;
    private String globalEventRoleTitle;
    private Integer capacity;
    private Integer reserveCapacity;
    private String description;
    private LocalDateTime deadline;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Поля для информации о занятости мест
    private Integer occupiedMainSlots;
    private Integer occupiedReserveSlots;
    private Integer availableMainSlots;
    private Integer availableReserveSlots;
    private Integer totalOccupiedSlots;
    private Integer totalAvailableSlots;
    private Boolean isMainFull;
    private Boolean isReserveFull;
    private Boolean isFullyFull;
}
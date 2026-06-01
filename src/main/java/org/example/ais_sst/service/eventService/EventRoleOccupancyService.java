package org.example.ais_sst.service.eventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.event_roles.EventRoleResponseDTO;
import org.example.ais_sst.entity.ApplicationsForTheRole;
import org.example.ais_sst.entity.EventRole;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;
import org.example.ais_sst.repository.ApplicationsForTheRoleRepository;
import org.example.ais_sst.repository.EventRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventRoleOccupancyService {

    private final ApplicationsForTheRoleRepository applicationsForTheRoleRepository;
    private final EventRoleRepository eventRoleRepository;

    /**
     * Заполнить DTO информацией о занятости мест
     */
    public void enrichWithOccupancy(EventRoleResponseDTO dto, Long eventRoleId) {
        Map<Long, EventRoleResponseDTO> map = enrichMultipleWithOccupancy(List.of(eventRoleId));
        EventRoleResponseDTO enriched = map.get(eventRoleId);

        if (enriched != null) {
            dto.setOccupiedMainSlots(enriched.getOccupiedMainSlots());
            dto.setOccupiedReserveSlots(enriched.getOccupiedReserveSlots());
            dto.setAvailableMainSlots(enriched.getAvailableMainSlots());
            dto.setAvailableReserveSlots(enriched.getAvailableReserveSlots());
            dto.setTotalOccupiedSlots(enriched.getTotalOccupiedSlots());
            dto.setTotalAvailableSlots(enriched.getTotalAvailableSlots());
            dto.setIsMainFull(enriched.getIsMainFull());
            dto.setIsReserveFull(enriched.getIsReserveFull());
            dto.setIsFullyFull(enriched.getIsFullyFull());
        }
    }

    /**
     * Заполнить несколько DTO информацией о занятости мест
     */
    public Map<Long, EventRoleResponseDTO> enrichMultipleWithOccupancy(List<Long> eventRoleIds) {
        if (eventRoleIds == null || eventRoleIds.isEmpty()) {
            return new HashMap<>();
        }

        List<ApplicationsForTheRole> approvedApplications = applicationsForTheRoleRepository
                .findByEventRoleIdInAndStatus(eventRoleIds, RoleApplicationStatuses.ОДОБРЕНА);

        Map<Long, Long> mainOccupancy = approvedApplications.stream()
                .filter(app -> !Boolean.TRUE.equals(app.getIsReserve()))
                .collect(Collectors.groupingBy(
                        app -> app.getEventRole().getId(),
                        Collectors.counting()
                ));

        Map<Long, Long> reserveOccupancy = approvedApplications.stream()
                .filter(app -> Boolean.TRUE.equals(app.getIsReserve()))
                .collect(Collectors.groupingBy(
                        app -> app.getEventRole().getId(),
                        Collectors.counting()
                ));

        List<EventRole> eventRoles = eventRoleRepository.findAllById(eventRoleIds);
        Map<Long, EventRoleResponseDTO> result = new HashMap<>();

        for (EventRole role : eventRoles) {
            result.put(role.getId(), buildOccupancyDTO(role, mainOccupancy, reserveOccupancy));
        }

        return result;
    }

    private EventRoleResponseDTO buildOccupancyDTO(EventRole role,
                                                   Map<Long, Long> mainOccupancy,
                                                   Map<Long, Long> reserveOccupancy) {
        long occupiedMain = mainOccupancy.getOrDefault(role.getId(), 0L);
        long occupiedReserve = reserveOccupancy.getOrDefault(role.getId(), 0L);

        int capacity = role.getCapacity() != null ? role.getCapacity() : 0;
        int reserveCapacity = role.getReserveCapacity() != null ? role.getReserveCapacity() : 0;

        int availableMain = Math.max(0, capacity - (int) occupiedMain);
        int availableReserve = Math.max(0, reserveCapacity - (int) occupiedReserve);

        return EventRoleResponseDTO.builder()
                .occupiedMainSlots((int) occupiedMain)
                .occupiedReserveSlots((int) occupiedReserve)
                .availableMainSlots(availableMain)
                .availableReserveSlots(availableReserve)
                .totalOccupiedSlots((int) (occupiedMain + occupiedReserve))
                .totalAvailableSlots(availableMain + availableReserve)
                .isMainFull(occupiedMain >= capacity && capacity > 0)
                .isReserveFull(occupiedReserve >= reserveCapacity && reserveCapacity > 0)
                .isFullyFull(occupiedMain >= capacity && occupiedReserve >= reserveCapacity && capacity > 0)
                .build();
    }

    @Transactional(readOnly = true)
    public boolean hasAvailableSpots(Long eventRoleId, int requestedSpots) {
        EventRoleResponseDTO occupancy = enrichMultipleWithOccupancy(List.of(eventRoleId)).get(eventRoleId);
        return occupancy != null && occupancy.getTotalAvailableSlots() >= requestedSpots;
    }
}
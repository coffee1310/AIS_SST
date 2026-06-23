package org.example.ais_sst.service.eventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.event_roles.EventRoleResponseDTO;
import org.example.ais_sst.entity.EventParticipationRecord;
import org.example.ais_sst.entity.EventRole;
import org.example.ais_sst.repository.EventParticipationRecordRepository;
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

    private final EventParticipationRecordRepository participationRecordRepository;
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

        // ⭐ Получаем все записи участия для указанных ролей (не удаленные)
        List<EventParticipationRecord> records = participationRecordRepository
                .findByEventRoleIdInAndIsDeletedFalse(eventRoleIds);

        // ⭐ Считаем занятые места в основном составе (isReserve = false)
        Map<Long, Long> mainOccupancy = records.stream()
                .filter(record -> !Boolean.TRUE.equals(record.getIsReserve()))
                .collect(Collectors.groupingBy(
                        record -> record.getEventRole().getId(),
                        Collectors.counting()
                ));

        // ⭐ Считаем занятые места в резерве (isReserve = true)
        Map<Long, Long> reserveOccupancy = records.stream()
                .filter(record -> Boolean.TRUE.equals(record.getIsReserve()))
                .collect(Collectors.groupingBy(
                        record -> record.getEventRole().getId(),
                        Collectors.counting()
                ));

        // ⭐ Получаем роли
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
                .isFullyFull(occupiedMain >= capacity && occupiedReserve >= reserveCapacity && capacity > 0 && reserveCapacity > 0)
                .build();
    }

    @Transactional(readOnly = true)
    public boolean hasAvailableSpots(Long eventRoleId, int requestedSpots) {
        EventRoleResponseDTO occupancy = enrichMultipleWithOccupancy(List.of(eventRoleId)).get(eventRoleId);
        return occupancy != null && occupancy.getTotalAvailableSlots() >= requestedSpots;
    }

    /**
     * Получить количество мест в основном составе
     */
    @Transactional(readOnly = true)
    public long getMainOccupancy(Long eventRoleId) {
        return participationRecordRepository.countByEventRoleIdAndIsReserveFalseAndIsDeletedFalse(eventRoleId);
    }

    /**
     * Получить количество мест в резерве
     */
    @Transactional(readOnly = true)
    public long getReserveOccupancy(Long eventRoleId) {
        return participationRecordRepository.countByEventRoleIdAndIsReserveTrueAndIsDeletedFalse(eventRoleId);
    }

    /**
     * Проверить, есть ли место в основном составе
     */
    @Transactional(readOnly = true)
    public boolean hasMainSpots(Long eventRoleId) {
        EventRole role = eventRoleRepository.findById(eventRoleId).orElse(null);
        if (role == null || role.getCapacity() == null || role.getCapacity() == 0) {
            return false;
        }
        long occupied = getMainOccupancy(eventRoleId);
        return occupied < role.getCapacity();
    }

    /**
     * Проверить, есть ли место в резерве
     */
    @Transactional(readOnly = true)
    public boolean hasReserveSpots(Long eventRoleId) {
        EventRole role = eventRoleRepository.findById(eventRoleId).orElse(null);
        if (role == null || role.getReserveCapacity() == null || role.getReserveCapacity() == 0) {
            return false;
        }
        long occupied = getReserveOccupancy(eventRoleId);
        return occupied < role.getReserveCapacity();
    }

    /**
     * Получить информацию о занятости в виде строки
     */
    @Transactional(readOnly = true)
    public String getOccupancyInfo(Long eventRoleId) {
        EventRole role = eventRoleRepository.findById(eventRoleId).orElse(null);
        if (role == null) {
            return "Роль не найдена";
        }

        long mainOccupied = getMainOccupancy(eventRoleId);
        long reserveOccupied = getReserveOccupancy(eventRoleId);

        int capacity = role.getCapacity() != null ? role.getCapacity() : 0;
        int reserveCapacity = role.getReserveCapacity() != null ? role.getReserveCapacity() : 0;

        return String.format("Основной состав: %d/%d, Резерв: %d/%d",
                mainOccupied, capacity, reserveOccupied, reserveCapacity);
    }
}
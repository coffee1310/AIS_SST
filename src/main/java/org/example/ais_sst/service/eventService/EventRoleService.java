package org.example.ais_sst.service.eventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.event_roles.EventRoleCreateDTO;
import org.example.ais_sst.dto.event_roles.EventRoleFilterDTO;
import org.example.ais_sst.dto.event_roles.EventRoleResponseDTO;
import org.example.ais_sst.dto.event_roles.EventRoleUpdateDTO;
import org.example.ais_sst.entity.Event;
import org.example.ais_sst.entity.EventRole;
import org.example.ais_sst.entity.GlobalEventRole;
import org.example.ais_sst.exception.*;
import org.example.ais_sst.mapper.EventRoleMapper;
import org.example.ais_sst.repository.EventRepository;
import org.example.ais_sst.repository.EventRoleRepository;
import org.example.ais_sst.repository.GlobalEventRolesRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventRoleService {

    private final EventRoleRepository eventRoleRepository;
    private final EventRepository eventRepository;
    private final GlobalEventRolesRepository globalEventRoleRepository;
    private final EventRoleMapper eventRoleMapper;
    private final EventRoleOccupancyService occupancyService;

    private static final int MIN_CAPACITY = 1;
    private static final int MAX_CAPACITY = 10000;

    @Transactional
    public EventRoleResponseDTO createEventRole(EventRoleCreateDTO dto) {
        log.info("Creating event role for eventId: {}, globalEventRoleId: {}",
                dto.getEventId(), dto.getGlobalEventRoleId());

        validateCapacity(dto.getCapacity(), dto.getReserveCapacity());
        validateDeadline(dto.getDeadline());

        Event event = getEventById(dto.getEventId());
        GlobalEventRole globalEventRole = getGlobalRoleById(dto.getGlobalEventRoleId());

        checkDuplicateGlobalRole(dto.getEventId(), dto.getGlobalEventRoleId(), null);

        EventRole eventRole = eventRoleMapper.toEntity(dto);
        eventRole.setEvent(event);
        eventRole.setGlobalEventRole(globalEventRole);

        EventRole savedEventRole = eventRoleRepository.save(eventRole);
        log.info("Event role created with id: {}", savedEventRole.getId());

        return enrichWithOccupancy(eventRoleMapper.toResponseDto(savedEventRole), savedEventRole.getId());
    }

    @Transactional(readOnly = true)
    public EventRoleResponseDTO getEventRoleById(Long id) {
        log.info("Getting event role by id: {}", id);

        EventRole eventRole = getEventRoleByIdOrThrow(id);
        return enrichWithOccupancy(eventRoleMapper.toResponseDto(eventRole), eventRole.getId());
    }

    @Transactional
    public EventRoleResponseDTO updateEventRole(Long id, EventRoleUpdateDTO dto) {
        log.info("Updating event role with id: {}", id);

        EventRole eventRole = getEventRoleByIdOrThrow(id);

        updateCapacityAndReserve(eventRole, dto);
        validateDeadline(dto.getDeadline());

        if (dto.getEventId() != null) {
            eventRole.setEvent(getEventById(dto.getEventId()));
        }

        if (dto.getGlobalEventRoleId() != null) {
            checkDuplicateGlobalRole(eventRole.getEvent().getId(), dto.getGlobalEventRoleId(), id);
            eventRole.setGlobalEventRole(getGlobalRoleById(dto.getGlobalEventRoleId()));
        }

        eventRoleMapper.updateEntity(dto, eventRole);

        EventRole updatedEventRole = eventRoleRepository.save(eventRole);
        log.info("Event role updated with id: {}", id);

        return enrichWithOccupancy(eventRoleMapper.toResponseDto(updatedEventRole), updatedEventRole.getId());
    }

    @Transactional
    public void deleteEventRole(Long id) {
        log.info("Soft deleting event role with id: {}", id);

        EventRole eventRole = getEventRoleByIdOrThrow(id);

        if (eventRole.getDeleted()) {
            throw new IllegalStateException("Роль мероприятия уже удалена");
        }

        eventRole.setDeleted(true);
        eventRoleRepository.save(eventRole);
        log.info("Event role soft deleted with id: {}", id);
    }

    @Transactional
    public void hardDeleteEventRole(Long id) {
        log.info("Hard deleting event role with id: {}", id);

        EventRole eventRole = getEventRoleByIdOrThrow(id);
        eventRoleRepository.delete(eventRole);
        log.info("Event role hard deleted with id: {}", id);
    }

    @Transactional(readOnly = true)
    public Page<EventRoleResponseDTO> getAllEventRoles(EventRoleFilterDTO filter, Pageable pageable) {
        log.info("Getting event roles with filters: {}", filter);

        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();

        List<EventRole> eventRoles;
        long total;

        if (filter.getCurrentUserId() != null) {
            if (Boolean.TRUE.equals(filter.getIsMySector())) {
                // Только роли из секторов пользователя
                eventRoles = eventRoleRepository.findAllWithFiltersAndMySectorNative(
                        filter.getId(),
                        filter.getEventId(),
                        filter.getGlobalEventRoleId(),
                        filter.getDeleted(),
                        filter.getDeadlineFrom(),
                        filter.getDeadlineTo(),
                        filter.getCurrentUserId(),
                        offset,
                        limit
                );
                total = eventRoleRepository.countAllWithFiltersAndMySectorNative(
                        filter.getId(),
                        filter.getEventId(),
                        filter.getGlobalEventRoleId(),
                        filter.getDeleted(),
                        filter.getDeadlineFrom(),
                        filter.getDeadlineTo(),
                        filter.getCurrentUserId()
                );
            } else if (Boolean.FALSE.equals(filter.getIsMySector())) {
                // Только роли НЕ из секторов пользователя
                eventRoles = eventRoleRepository.findAllWithFiltersAndNotMySectorNative(
                        filter.getId(),
                        filter.getEventId(),
                        filter.getGlobalEventRoleId(),
                        filter.getDeleted(),
                        filter.getDeadlineFrom(),
                        filter.getDeadlineTo(),
                        filter.getCurrentUserId(),
                        offset,
                        limit
                );
                total = eventRoleRepository.countAllWithFiltersAndNotMySectorNative(
                        filter.getId(),
                        filter.getEventId(),
                        filter.getGlobalEventRoleId(),
                        filter.getDeleted(),
                        filter.getDeadlineFrom(),
                        filter.getDeadlineTo(),
                        filter.getCurrentUserId()
                );
            } else {
                // isMySector = null - все роли
                eventRoles = eventRoleRepository.findAllWithFiltersNative(
                        filter.getId(),
                        filter.getEventId(),
                        filter.getGlobalEventRoleId(),
                        filter.getDeleted(),
                        filter.getDeadlineFrom(),
                        filter.getDeadlineTo(),
                        offset,
                        limit
                );
                total = eventRoleRepository.countAllWithFiltersNative(
                        filter.getId(),
                        filter.getEventId(),
                        filter.getGlobalEventRoleId(),
                        filter.getDeleted(),
                        filter.getDeadlineFrom(),
                        filter.getDeadlineTo()
                );
            }
        } else {
            // userId = null - все роли
            eventRoles = eventRoleRepository.findAllWithFiltersNative(
                    filter.getId(),
                    filter.getEventId(),
                    filter.getGlobalEventRoleId(),
                    filter.getDeleted(),
                    filter.getDeadlineFrom(),
                    filter.getDeadlineTo(),
                    offset,
                    limit
            );
            total = eventRoleRepository.countAllWithFiltersNative(
                    filter.getId(),
                    filter.getEventId(),
                    filter.getGlobalEventRoleId(),
                    filter.getDeleted(),
                    filter.getDeadlineFrom(),
                    filter.getDeadlineTo()
            );
        }

        List<Long> eventRoleIds = eventRoles.stream()
                .map(EventRole::getId)
                .toList();

        Map<Long, EventRoleResponseDTO> occupancyMap = occupancyService.enrichMultipleWithOccupancy(eventRoleIds);

        List<EventRoleResponseDTO> dtos = eventRoles.stream()
                .map(role -> {
                    EventRoleResponseDTO dto = eventRoleMapper.toResponseDto(role);
                    EventRoleResponseDTO occupancy = occupancyMap.get(role.getId());
                    if (occupancy != null) {
                        copyOccupancyFields(occupancy, dto);
                    }
                    return dto;
                })
                .toList();

        return new PageImpl<>(dtos, pageable, total);
    }

    // ==================== Private Helper Methods ====================

    private void validateCapacity(Integer capacity, Integer reserveCapacity) {
        if (capacity != null) {
            if (capacity < MIN_CAPACITY) {
                throw new IllegalArgumentException("Capacity должен быть не менее " + MIN_CAPACITY);
            }
            if (capacity > MAX_CAPACITY) {
                throw new IllegalArgumentException("Capacity не может превышать " + MAX_CAPACITY);
            }
        }

        if (reserveCapacity != null) {
            if (reserveCapacity < 0) {
                throw new IllegalArgumentException("Reserve capacity не может быть отрицательным");
            }
            int actualCapacity = capacity != null ? capacity : 0;
            if (reserveCapacity > actualCapacity) {
                throw new IllegalArgumentException("Reserve capacity не может быть больше capacity");
            }
        }
    }

    private void validateDeadline(LocalDateTime deadline) {
        if (deadline != null && deadline.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Дедлайн должен быть в будущем");
        }
    }

    private void checkDuplicateGlobalRole(Long eventId, Long globalEventRoleId, Long excludeRoleId) {
        boolean exists = eventRoleRepository.existsByEventIdAndGlobalEventRoleIdAndDeletedFalse(eventId, globalEventRoleId);

        if (exists && excludeRoleId == null) {
            throw new EventRoleAlreadyExistsException("Роль уже существует для этого мероприятия");
        }

        if (exists && excludeRoleId != null) {
            eventRoleRepository.findByEventIdAndGlobalEventRoleIdAndDeletedFalse(eventId, globalEventRoleId)
                    .ifPresent(existingRole -> {
                        if (!existingRole.getId().equals(excludeRoleId)) {
                            throw new EventRoleAlreadyExistsException("Роль уже существует для этого мероприятия");
                        }
                    });
        }
    }

    private Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException("Мероприятие не найдено с id: " + eventId));
    }

    private GlobalEventRole getGlobalRoleById(Long globalRoleId) {
        return globalEventRoleRepository.findById(globalRoleId)
                .orElseThrow(() -> new GlobalRoleDoesNotExistException("Глобальная роль не найдена с id: " + globalRoleId));
    }

    private EventRole getEventRoleByIdOrThrow(Long id) {
        return eventRoleRepository.findById(id)
                .orElseThrow(() -> new EventRoleDoesNotFoundException("Роль мероприятия не найдена с id: " + id));
    }

    private void updateCapacityAndReserve(EventRole eventRole, EventRoleUpdateDTO dto) {
        Integer newCapacity = dto.getCapacity() != null ? dto.getCapacity() : eventRole.getCapacity();
        Integer newReserveCapacity = dto.getReserveCapacity() != null ? dto.getReserveCapacity() : eventRole.getReserveCapacity();

        if (dto.getCapacity() != null || dto.getReserveCapacity() != null) {
            validateCapacity(newCapacity, newReserveCapacity);
        }
    }

    private EventRoleResponseDTO enrichWithOccupancy(EventRoleResponseDTO dto, Long eventRoleId) {
        occupancyService.enrichWithOccupancy(dto, eventRoleId);
        return dto;
    }

    private void copyOccupancyFields(EventRoleResponseDTO source, EventRoleResponseDTO target) {
        target.setOccupiedMainSlots(source.getOccupiedMainSlots());
        target.setOccupiedReserveSlots(source.getOccupiedReserveSlots());
        target.setAvailableMainSlots(source.getAvailableMainSlots());
        target.setAvailableReserveSlots(source.getAvailableReserveSlots());
        target.setTotalOccupiedSlots(source.getTotalOccupiedSlots());
        target.setTotalAvailableSlots(source.getTotalAvailableSlots());
        target.setIsMainFull(source.getIsMainFull());
        target.setIsReserveFull(source.getIsReserveFull());
        target.setIsFullyFull(source.getIsFullyFull());
    }
}
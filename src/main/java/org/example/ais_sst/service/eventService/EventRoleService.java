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

@Slf4j
@Service
@RequiredArgsConstructor
public class EventRoleService {

    private final EventRoleRepository eventRoleRepository;
    private final EventRepository eventRepository;
    private final GlobalEventRolesRepository globalEventRoleRepository;
    private final EventRoleMapper eventRoleMapper;

    private static final int MIN_CAPACITY = 1;
    private static final int MAX_CAPACITY = 10000;

    private void validateCapacity(Integer capacity, Integer reserveCapacity) {
        if (capacity != null) {
            if (capacity < MIN_CAPACITY) {
                throw new IllegalArgumentException(
                        String.format("Capacity должен быть не менее %d (текущее: %d)", MIN_CAPACITY, capacity));
            }
            if (capacity > MAX_CAPACITY) {
                throw new IllegalArgumentException(
                        String.format("Capacity не может превышать %d (текущее: %d)", MAX_CAPACITY, capacity));
            }
        }

        if (reserveCapacity != null) {
            if (reserveCapacity < 0) {
                throw new IllegalArgumentException(
                        String.format("Reserve capacity не может быть отрицательным (текущее: %d)", reserveCapacity));
            }
            int actualCapacity = capacity != null ? capacity : 0;
            if (reserveCapacity > actualCapacity) {
                throw new IllegalArgumentException(
                        String.format("Reserve capacity (%d) не может быть больше capacity (%d)",
                                reserveCapacity, actualCapacity));
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

        if (exists) {
            if (excludeRoleId != null) {
                eventRoleRepository.findByEventIdAndGlobalEventRoleIdAndDeletedFalse(eventId, globalEventRoleId)
                        .ifPresent(existingRole -> {
                            if (!existingRole.getId().equals(excludeRoleId)) {
                                throw new EventRoleAlreadyExistsException(
                                        String.format("Роль с globalEventRoleId=%d уже существует для этого мероприятия", globalEventRoleId));
                            }
                        });
            } else {
                throw new EventRoleAlreadyExistsException(
                        String.format("Роль с globalEventRoleId=%d уже существует для этого мероприятия", globalEventRoleId));
            }
        }
    }

    @Transactional
    public EventRoleResponseDTO createEventRole(EventRoleCreateDTO dto) {
        log.info("Creating event role for eventId: {}, globalEventRoleId: {}",
                dto.getEventId(), dto.getGlobalEventRoleId());

        validateCapacity(dto.getCapacity(), dto.getReserveCapacity());
        validateDeadline(dto.getDeadline());

        Event event = eventRepository.findById(dto.getEventId())
                .orElseThrow(() -> new EventDoesNotExistException(
                        String.format("Мероприятие с id=%d не найдено", dto.getEventId())));

        GlobalEventRole globalEventRole = globalEventRoleRepository.findById(dto.getGlobalEventRoleId())
                .orElseThrow(() -> new GlobalRoleDoesNotExistException(
                        String.format("Глобальная роль с id=%d не найдена", dto.getGlobalEventRoleId())));

        checkDuplicateGlobalRole(dto.getEventId(), dto.getGlobalEventRoleId(), null);

        EventRole eventRole = eventRoleMapper.toEntity(dto);
        eventRole.setEvent(event);
        eventRole.setGlobalEventRole(globalEventRole);

        EventRole savedEventRole = eventRoleRepository.save(eventRole);
        log.info("Event role created with id: {}, deadline: {}", savedEventRole.getId(), savedEventRole.getDeadline());

        return eventRoleMapper.toResponseDto(savedEventRole);
    }

    @Transactional(readOnly = true)
    public EventRoleResponseDTO getEventRoleById(Long id) {
        log.info("Getting event role by id: {}", id);

        EventRole eventRole = eventRoleRepository.findById(id)
                .orElseThrow(() -> new EventRoleDoesNotFoundException(
                        String.format("Роль мероприятия с id=%d не найдена", id)));

        return eventRoleMapper.toResponseDto(eventRole);
    }

    @Transactional
    public EventRoleResponseDTO updateEventRole(Long id, EventRoleUpdateDTO dto) {
        log.info("Updating event role with id: {}", id);

        EventRole eventRole = eventRoleRepository.findById(id)
                .orElseThrow(() -> new EventRoleDoesNotFoundException(
                        String.format("Роль мероприятия с id=%d не найдена", id)));

        Integer newCapacity = dto.getCapacity() != null ? dto.getCapacity() : eventRole.getCapacity();
        Integer newReserveCapacity = dto.getReserveCapacity() != null ?
                dto.getReserveCapacity() : eventRole.getReserveCapacity();

        if (dto.getCapacity() != null || dto.getReserveCapacity() != null) {
            validateCapacity(newCapacity, newReserveCapacity);
        }

        validateDeadline(dto.getDeadline());

        if (dto.getEventId() != null) {
            Event event = eventRepository.findById(dto.getEventId())
                    .orElseThrow(() -> new EventDoesNotExistException(
                            String.format("Мероприятие с id=%d не найдено", dto.getEventId())));
            eventRole.setEvent(event);
        }

        if (dto.getGlobalEventRoleId() != null) {
            checkDuplicateGlobalRole(eventRole.getEvent().getId(), dto.getGlobalEventRoleId(), id);

            GlobalEventRole globalEventRole = globalEventRoleRepository.findById(dto.getGlobalEventRoleId())
                    .orElseThrow(() -> new GlobalRoleDoesNotExistException(
                            String.format("Глобальная роль с id=%d не найдена", dto.getGlobalEventRoleId())));
            eventRole.setGlobalEventRole(globalEventRole);
        }

        eventRoleMapper.updateEntity(dto, eventRole);

        EventRole updatedEventRole = eventRoleRepository.save(eventRole);
        log.info("Event role updated with id: {}, deadline: {}", id, updatedEventRole.getDeadline());

        return eventRoleMapper.toResponseDto(updatedEventRole);
    }

    @Transactional
    public void deleteEventRole(Long id) {
        log.info("Soft deleting event role with id: {}", id);

        EventRole eventRole = eventRoleRepository.findById(id)
                .orElseThrow(() -> new EventRoleDoesNotFoundException(
                        String.format("Роль мероприятия с id=%d не найдена", id)));

        if (eventRole.getDeleted()) {
            throw new IllegalStateException(
                    String.format("Роль мероприятия с id=%d уже удалена", id));
        }

        eventRole.setDeleted(true);
        eventRoleRepository.save(eventRole);
        log.info("Event role soft deleted with id: {}", id);
    }

    @Transactional
    public void hardDeleteEventRole(Long id) {
        log.info("Hard deleting event role with id: {}", id);

        EventRole eventRole = eventRoleRepository.findById(id)
                .orElseThrow(() -> new EventRoleDoesNotFoundException(
                        String.format("Роль мероприятия с id=%d не найдена", id)));

        eventRoleRepository.delete(eventRole);
        log.info("Event role hard deleted with id: {}", id);
    }

    @Transactional(readOnly = true)
    public Page<EventRoleResponseDTO> getAllEventRoles(EventRoleFilterDTO filter, Pageable pageable) {
        log.info("Getting event roles with filters: {}", filter);

        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();

        List<EventRole> eventRoles = eventRoleRepository.findAllWithFiltersNative(
                filter.getId(),
                filter.getEventId(),
                filter.getGlobalEventRoleId(),
                filter.getDeleted(),
                filter.getDeadlineFrom(),
                filter.getDeadlineTo(),
                offset,
                limit);

        long total = eventRoleRepository.countAllWithFiltersNative(
                filter.getId(),
                filter.getEventId(),
                filter.getGlobalEventRoleId(),
                filter.getDeleted(),
                filter.getDeadlineFrom(),
                filter.getDeadlineTo());

        Page<EventRole> eventRolePage = new PageImpl<>(eventRoles, pageable, total);

        return eventRolePage.map(eventRoleMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public boolean hasAvailableSpots(Long eventRoleId, int requestedSpots) {
        EventRole eventRole = eventRoleRepository.findById(eventRoleId)
                .orElseThrow(() -> new EventRoleDoesNotFoundException(
                        String.format("Роль мероприятия с id=%d не найдена", eventRoleId)));

        int occupiedSpots = getOccupiedSpotsCount(eventRoleId);
        int availableSpots = eventRole.getCapacity() - occupiedSpots;

        return availableSpots >= requestedSpots;
    }

    private int getOccupiedSpotsCount(Long eventRoleId) {
        // TODO: Реализовать запрос к репозиторию участников
        return 0;
    }

    @Transactional(readOnly = true)
    public boolean isDeadlineExpired(Long eventRoleId) {
        EventRole eventRole = eventRoleRepository.findById(eventRoleId)
                .orElseThrow(() -> new EventRoleDoesNotFoundException(
                        String.format("Роль мероприятия с id=%d не найдена", eventRoleId)));

        return eventRole.getDeadline() != null && eventRole.getDeadline().isBefore(LocalDateTime.now());
    }
}

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventRoleService {

    private final EventRoleRepository eventRoleRepository;
    private final EventRepository eventRepository;
    private final GlobalEventRolesRepository globalEventRoleRepository;
    private final EventRoleMapper eventRoleMapper;

    @Transactional
    public EventRoleResponseDTO createEventRole(EventRoleCreateDTO dto) {
        log.info("Creating event role for eventId: {}, globalEventRoleId: {}",
                dto.getEventId(), dto.getGlobalEventRoleId());

        Event event = eventRepository.findById(dto.getEventId())
                .orElseThrow(() -> new EventDoesNotExistException("Мероприятие не найдено"));

        GlobalEventRole globalEventRole = globalEventRoleRepository.findById(dto.getGlobalEventRoleId())
                .orElseThrow(() -> new GlobalRoleDoesNotExistException("Глобальная роль не найдена"));

        if (eventRoleRepository.existsByEventIdAndGlobalEventRoleIdAndDeletedFalse(
                dto.getEventId(), dto.getGlobalEventRoleId())) {
            throw new EventRoleAlreadyExistsException("Роль уже существует для этого мероприятия");
        }

        EventRole eventRole = eventRoleMapper.toEntity(dto);
        eventRole.setEvent(event);
        eventRole.setGlobalEventRole(globalEventRole);

        EventRole savedEventRole = eventRoleRepository.save(eventRole);
        log.info("Event role created with id: {}", savedEventRole.getId());

        return eventRoleMapper.toResponseDto(savedEventRole);
    }

    @Transactional(readOnly = true)
    public EventRoleResponseDTO getEventRoleById(Long id) {
        log.info("Getting event role by id: {}", id);

        EventRole eventRole = eventRoleRepository.findById(id)
                .orElseThrow(() -> new EventRoleDoesNotFoundException("Роль мероприятия не найдена"));

        return eventRoleMapper.toResponseDto(eventRole);
    }

    @Transactional
    public EventRoleResponseDTO updateEventRole(Long id, EventRoleUpdateDTO dto) {
        log.info("Updating event role with id: {}", id);

        EventRole eventRole = eventRoleRepository.findById(id)
                .orElseThrow(() -> new EventRoleDoesNotFoundException("Роль мероприятия не найдена"));

        if (dto.getEventId() != null) {
            Event event = eventRepository.findById(dto.getEventId())
                    .orElseThrow(() -> new EventDoesNotExistException("Мероприятие не найдено"));
            eventRole.setEvent(event);
        }

        if (dto.getGlobalEventRoleId() != null) {
            GlobalEventRole globalEventRole = globalEventRoleRepository.findById(dto.getGlobalEventRoleId())
                    .orElseThrow(() -> new GlobalRoleDoesNotExistException("Глобальная роль не найдена"));
            eventRole.setGlobalEventRole(globalEventRole);
        }

        if (dto.getCapacity() != null) {
            eventRole.setCapacity(dto.getCapacity());
        }

        if (dto.getReserveCapacity() != null) {
            eventRole.setReserveCapacity(dto.getReserveCapacity());
        }

        if (dto.getDeleted() != null) {
            eventRole.setDeleted(dto.getDeleted());
        }

        EventRole updatedEventRole = eventRoleRepository.save(eventRole);
        log.info("Event role updated with id: {}", id);

        return eventRoleMapper.toResponseDto(updatedEventRole);
    }

    @Transactional
    public void deleteEventRole(Long id) {
        log.info("Soft deleting event role with id: {}", id);

        EventRole eventRole = eventRoleRepository.findById(id)
                .orElseThrow(() -> new EventRoleDoesNotFoundException("Роль мероприятия не найдена"));

        eventRole.setDeleted(true);
        eventRoleRepository.save(eventRole);
        log.info("Event role soft deleted with id: {}", id);
    }

    @Transactional
    public void hardDeleteEventRole(Long id) {
        log.info("Hard deleting event role with id: {}", id);

        EventRole eventRole = eventRoleRepository.findById(id)
                .orElseThrow(() -> new EventRoleDoesNotFoundException("Роль мероприятия не найдена"));

        eventRoleRepository.delete(eventRole);
        log.info("Event role hard deleted with id: {}", id);
    }

    @Transactional(readOnly = true)
    public Page<EventRoleResponseDTO> getAllEventRoles(EventRoleFilterDTO filter, Pageable pageable) {
        log.info("Getting event roles with filters: {}", filter);

        Page<EventRole> eventRoles = eventRoleRepository.findAllWithFilters(
                filter.getId(),
                filter.getEventId(),
                filter.getGlobalEventRoleId(),
                filter.getDeleted(),
                pageable);

        return eventRoles.map(eventRoleMapper::toResponseDto);
    }

}

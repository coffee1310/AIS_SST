package org.example.ais_sst.service.eventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.events.*;
import org.example.ais_sst.entity.*;
import org.example.ais_sst.entity.enums.SectorParticipantStatuses;
import org.example.ais_sst.exception.*;
import org.example.ais_sst.mapper.EventMapper;
import org.example.ais_sst.repository.*;
import org.example.ais_sst.service.base.BaseEntityService;
import org.example.ais_sst.specification.EventSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class EventService extends BaseEntityService {

    private static final Integer UNLIMITED_ORGANIZERS = 0;
    private final EventRepository eventRepository;
    private final EventOrganizerRepository eventOrganizerRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final EventPhotoService eventPhotoService;
    private final EventParticipantsRepository eventParticipantsRepository;
    private final SectorRepository sectorRepository;
    private final EventRoleRepository eventRoleRepository;
    private final SectorParticipantRepository sectorParticipantRepository;

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "Administrator", "Curator", "Deputy_chairman",
            "Sector_coordinator", "Chairman"
    );

    @Transactional(readOnly = true)
    public EventResponseDTO getEventById(Long eventId, Long userId) {
        return executeWithLogging(() -> {
            Event event = findEntityOrThrow(eventId, eventRepository::findById,
                    () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

            EventResponseDTO response = eventMapper.toResponseDto(event);

            response.setCurrentParticipantsCount(eventParticipantsRepository.countByEventId(eventId));
            response.setCurrentOrganizersCount(eventOrganizerRepository.countByEventId(eventId));

            if (event.getPhoto() != null && !event.getPhoto().isEmpty()) {
                String base64Photo = eventPhotoService.getPhotoAsBase64(event.getPhoto());
                response.setPhoto(base64Photo);
            }

            response.setSectors(getEventSectors(event));
            response.setIsMySector(isUserHasSectorForEvent(event, userId));

            return response;
        }, "getEventById", eventId);
    }

    @Transactional
    public EventResponseDTO createEvent(EventCreateDTO dto, Long creatorId) {
        return executeWithLogging(() -> {
            // Валидация sector_ids
            validateSectorsRequired(dto);

            User creator = findEntityOrThrow(creatorId, userRepository::findById,
                    () -> new UserDoesNotExistException("Пользователь не найден"), "User");

            validateState(ALLOWED_ROLES.contains(creator.getRole().getTitle()),
                    () -> new UnauthorizedException("У вас нет прав для создания мероприятий"),
                    "User not allowed to create events");

            Event event = eventMapper.toEntity(dto);
            event.setPhoto(savePhoto(dto.getPhoto()));
            event.setEventCreator(creator);

            // Устанавливаем сектора
            if (dto.getSectorIds() != null && !dto.getSectorIds().isEmpty()) {
                for (Long sectorId : dto.getSectorIds()) {
                    Sector sector = findEntityOrThrow(sectorId, sectorRepository::findById,
                            () -> new IllegalArgumentException("Сектор не найден: " + sectorId), "Sector");
                    event.addSector(sector);
                }
            }

            if (event.getMaxOrganizersCount() < event.getOrganizers().size()) {
                throw new OrganizerLimitExceededException("Вы указали некорректное максимальное количество организаторов");
            }

            if (event.getMaxOrganizersCount() == null) {
                event.setMaxOrganizersCount(event.getOrganizers().size());
            }

            Event savedEvent = eventRepository.save(event);
            addOrganizersToEvent(savedEvent, dto.getOrganizerIds());

            EventResponseDTO response = eventMapper.toResponseDto(savedEvent);
            response.setCurrentParticipantsCount(0L);
            response.setCurrentOrganizersCount(eventOrganizerRepository.countByEventId(savedEvent.getId()));
            response.setSectors(getEventSectors(savedEvent));
            // Для создателя мероприятия isMySector всегда true, так как он создатель
            response.setIsMySector(true);

            return response;
        }, "createEvent", dto.getTitle(), creatorId);
    }

    @Transactional
    public EventResponseDTO updateEvent(Long eventId, EventUpdateDTO dto, Long userId) {
        return executeWithLogging(() -> {
            log.info("=== START updateEvent ===");
            log.info("Updating event with ID: {}", eventId);
            log.info("DTO title: {}", dto.getTitle());
            log.info("DTO description: {}", dto.getDescription());

            validateIsEventCreator(eventId, userId);

            Event event = findEntityOrThrow(eventId, eventRepository::findById,
                    () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

            // Валидация sector_ids
            validateSectorsRequired(dto, event);

            log.info("Existing event - ID: {}, Title: '{}'", event.getId(), event.getTitle());

            // Обновляем поля с логированием
            if (dto.getTitle() != null) {
                log.info("Changing title from '{}' to '{}'", event.getTitle(), dto.getTitle());
                event.setTitle(dto.getTitle());
            }
            if (dto.getDescription() != null) {
                log.info("Changing description");
                event.setDescription(dto.getDescription());
            }
            if (dto.getDateOfEvent() != null) {
                log.info("Changing dateOfEvent from {} to {}", event.getDateOfEvent(), dto.getDateOfEvent());
                event.setDateOfEvent(dto.getDateOfEvent());
            }
            if (dto.getStartTime() != null) {
                log.info("Changing startTime from {} to {}", event.getStartTime(), dto.getStartTime());
                event.setStartTime(dto.getStartTime());
            }
            if (dto.getEndTime() != null) {
                log.info("Changing endTime from {} to {}", event.getEndTime(), dto.getEndTime());
                event.setEndTime(dto.getEndTime());
            }
            if (dto.getVenue() != null) {
                log.info("Changing venue from '{}' to '{}'", event.getVenue(), dto.getVenue());
                event.setVenue(dto.getVenue());
            }
            if (dto.getReferenceToPosition() != null) {
                log.info("Changing referenceToPosition");
                event.setReferenceToPosition(dto.getReferenceToPosition());
            }
            if (dto.getIsPublic() != null) {
                log.info("Changing isPublic from {} to {}", event.getIsPublic(), dto.getIsPublic());
                event.setIsPublic(dto.getIsPublic());
            }
            if (dto.getIsDraft() != null) {
                log.info("Changing isDraft from {} to {}", event.getIsDraft(), dto.getIsDraft());
                event.setIsDraft(dto.getIsDraft());
            }
            if (dto.getIsActive() != null) {
                log.info("Changing isActive from {} to {}", event.getIsActive(), dto.getIsActive());
                event.setIsActive(dto.getIsActive());
            }

            if (dto.getPhoto() != null && !dto.getPhoto().isEmpty()) {
                log.info("Updating photo");
                updateEventPhoto(event, dto.getPhoto());
            }

            if (dto.getOrganizerIds() != null) {
                log.info("Updating organizers: {}", dto.getOrganizerIds());
                updateOrganizers(event, dto.getOrganizerIds());
            }

            if (dto.getSectorIds() != null) {
                // Очищаем существующие сектора
                event.getEventSectors().clear();

                // Добавляем новые сектора
                for (Long sectorId : dto.getSectorIds()) {
                    Sector sector = findEntityOrThrow(sectorId, sectorRepository::findById,
                            () -> new IllegalArgumentException("Сектор не найден: " + sectorId), "Sector");
                    event.addSector(sector);
                }
            } else if (dto.getSectorIds() == null && dto.getIsPublic() != null && Boolean.FALSE.equals(dto.getIsPublic())) {
                // Если isPublic = false и sectorIds не указан - ошибка уже выброшена в validateSectorsRequired
            }

                if (event.getMaxOrganizersCount() < event.getOrganizers().size()) {throw new OrganizerLimitExceededException("Вы указали некорректное максимальное количество организаторов");}

            if (event.getMaxOrganizersCount() == null) {event.setMaxOrganizersCount(event.getOrganizers().size());}

            log.info("Saving event with title: '{}'", event.getTitle());
            Event updatedEvent = eventRepository.save(event);
            log.info("Event saved successfully with ID: {}", updatedEvent.getId());

            EventResponseDTO response = eventMapper.toResponseDto(updatedEvent);
            response.setCurrentParticipantsCount(eventParticipantsRepository.countByEventId(eventId));
            response.setCurrentOrganizersCount(eventOrganizerRepository.countByEventId(eventId));
            response.setSectors(getEventSectors(updatedEvent));
            response.setIsMySector(isUserHasSectorForEvent(updatedEvent, userId));

            return response;
        }, "updateEvent", eventId, userId);
    }

    private void validateSectorsRequired(EventCreateDTO dto) {
        if (Boolean.FALSE.equals(dto.getIsPublic()) &&
                (dto.getSectorIds() == null || dto.getSectorIds().isEmpty())) {
            throw new IllegalArgumentException("Для закрытого мероприятия необходимо указать хотя бы один сектор");
        }
    }

    private void validateSectorsRequired(EventUpdateDTO dto, Event existingEvent) {
        Boolean isPublic = dto.getIsPublic() != null ? dto.getIsPublic() : existingEvent.getIsPublic();
        List<Long> sectorIds = dto.getSectorIds() != null ? dto.getSectorIds() : existingEvent.getSectorIds();

        if (Boolean.FALSE.equals(isPublic) && (sectorIds == null || sectorIds.isEmpty())) {
            throw new IllegalArgumentException("Для закрытого мероприятия необходимо указать хотя бы один сектор");
        }
    }

    private List<EventSectorResponseDTO> getEventSectors(Event event) {
        if (event.getEventSectors() == null || event.getEventSectors().isEmpty()) {
            return List.of();
        }

        return event.getEventSectors().stream()
                .map(es -> EventSectorResponseDTO.builder()
                        .id(es.getSector().getId())
                        .title(es.getSector().getTitle())
                        .description(es.getSector().getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    private boolean isUserHasSectorForEvent(Event event, Long userId) {
        if (userId == null || event.getEventSectors() == null || event.getEventSectors().isEmpty()) {
            log.debug("User {} or event sectors is null/empty", userId);
            return false;
        }

        // Получаем ID секторов, в которых состоит пользователь (без фильтра по статусу)
        List<Long> userSectorIds = sectorParticipantRepository.findSectorIdsByUserIdAndStatus(userId, SectorParticipantStatuses.Активный);

        log.debug("User {} sectors: {}", userId, userSectorIds);

        if (userSectorIds.isEmpty()) {
            log.debug("User {} has no sectors", userId);
            return false;
        }

        Set<Long> userSectorIdSet = new HashSet<>(userSectorIds);

        // Получаем ID секторов мероприятия
        List<Long> eventSectorIds = event.getEventSectors().stream()
                .map(es -> es.getSector().getId())
                .collect(Collectors.toList());

        log.debug("Event sectors: {}", eventSectorIds);

        // Проверяем, есть ли совпадение
        boolean hasSector = eventSectorIds.stream().anyMatch(userSectorIdSet::contains);
        log.debug("User {} has sector for event {}: {}", userId, event.getId(), hasSector);

        return hasSector;
    }

        @Transactional
    public EventResponseDTO completeEvent(Long eventId, Long userId) {
        return executeWithLogging(() -> {
            validateIsEventCreator(eventId, userId);

            Event event = findEntityOrThrow(eventId, eventRepository::findById,
                    () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

            validateState(!Boolean.TRUE.equals(event.getIsCompleted()),
                    () -> new IllegalStateException("Мероприятие уже завершено"),
                    "Event already completed");

            event.setIsCompleted(true);
            event.setIsActive(false);

            Event savedEvent = eventRepository.save(event);
            EventResponseDTO response = eventMapper.toResponseDto(savedEvent);

            // Добавляем текущее количество участников и организаторов
            response.setCurrentParticipantsCount(eventParticipantsRepository.countByEventId(eventId));
            response.setCurrentOrganizersCount(eventOrganizerRepository.countByEventId(eventId));
            // ДОБАВЛЯЕМ СЕКТОРА
            response.setSectors(getEventSectors(savedEvent));

            return response;
        }, "completeEvent", eventId, userId);
    }

    @Transactional
    public EventResponseDTO addOrganizer(Long eventId, Long organizerId, Long userId) {
        return executeWithLogging(() -> {
            validateIsEventCreator(eventId, userId);

            Event event = findEntityOrThrow(eventId, eventRepository::findById,
                    () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

            User organizer = findEntityOrThrow(organizerId, userRepository::findById,
                    () -> new UserDoesNotExistException("Пользователь не найден"), "User");

            validateState(!eventOrganizerRepository.existsByEventIdAndUserId(eventId, organizerId),
                    () -> new IllegalArgumentException("Пользователь уже является организатором"),
                    "Organizer already exists");

            EventOrganizer eventOrganizer = EventOrganizer.builder()
                    .event(event).user(organizer).build();
            eventOrganizerRepository.save(eventOrganizer);

            EventResponseDTO response = eventMapper.toResponseDto(event);

            // Добавляем текущее количество участников и организаторов
            response.setCurrentParticipantsCount(eventParticipantsRepository.countByEventId(eventId));
            response.setCurrentOrganizersCount(eventOrganizerRepository.countByEventId(eventId));
            // ДОБАВЛЯЕМ СЕКТОРА
            response.setSectors(getEventSectors(event));

            return response;
        }, "addOrganizer", eventId, organizerId, userId);
    }

    @Transactional
    public EventResponseDTO removeOrganizer(Long eventId, Long organizerId, Long userId) {
        return executeWithLogging(() -> {
            validateIsEventCreator(eventId, userId);

            Event event = findEntityOrThrow(eventId, eventRepository::findById,
                    () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

            long organizersCount = eventOrganizerRepository.countByEventId(eventId);
            validateState(organizersCount > 1,
                    () -> new IllegalArgumentException("Нельзя удалить единственного организатора"),
                    "Cannot remove last organizer");

            eventOrganizerRepository.deleteByEventIdAndUserId(eventId, organizerId);

            EventResponseDTO response = eventMapper.toResponseDto(event);

            // Добавляем текущее количество участников и организаторов
            response.setCurrentParticipantsCount(eventParticipantsRepository.countByEventId(eventId));
            response.setCurrentOrganizersCount(eventOrganizerRepository.countByEventId(eventId));
            // ДОБАВЛЯЕМ СЕКТОРА
            response.setSectors(getEventSectors(event));

            return response;
        }, "removeOrganizer", eventId, organizerId, userId);
    }

    @Transactional
    public void deleteEvent(Long eventId, Long userId) {
        executeVoidWithLogging(() -> {
            validateIsEventCreator(eventId, userId);

            Event event = findEntityOrThrow(eventId, eventRepository::findById,
                    () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

            // Мягкое удаление всех ролей мероприятия
            softDeleteEventRoles(eventId);

            // Мягкое удаление самого мероприятия
            event.setIsActive(false);
            event.setIsDeleted(true);
            eventRepository.save(event);

            log.info("Event {} and all its roles soft deleted by user {}", eventId, userId);
        }, "deleteEvent", eventId, userId);
    }

    /**
     * Мягкое удаление всех ролей мероприятия
     */
    private void softDeleteEventRoles(Long eventId) {
        List<EventRole> eventRoles = eventRoleRepository.findByEventIdAndDeletedFalse(eventId);

        if (eventRoles.isEmpty()) {
            log.debug("No active roles found for event {}", eventId);
            return;
        }

        eventRoles.forEach(role -> role.setDeleted(true));
        eventRoleRepository.saveAll(eventRoles);

        log.info("Soft deleted {} roles for event {}", eventRoles.size(), eventId);
    }

    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getEventsByCreator(Long creatorId, Pageable pageable) {
        return eventRepository.findByEventCreatorId(creatorId, pageable)
                .map(event -> {
                    EventResponseDTO dto = eventMapper.toResponseDto(event);
                    dto.setCurrentParticipantsCount(eventParticipantsRepository.countByEventId(event.getId()));
                    dto.setCurrentOrganizersCount(eventOrganizerRepository.countByEventId(event.getId()));
                    dto.setSectors(getEventSectors(event));
                    // Для этого метода userId не передается, можно оставить null или получить из контекста
                    dto.setIsMySector(false);
                    return dto;
                });
    }

    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getEventsWithFilters(EventFilterDTO filter, Pageable pageable) {
        Specification<Event> spec = EventSpecification.withFilter(filter);
        return eventRepository.findAll(spec, pageable)
                .map(event -> {
                    EventResponseDTO dto = eventMapper.toResponseDto(event);
                    dto.setCurrentParticipantsCount(eventParticipantsRepository.countByEventId(event.getId()));
                    dto.setCurrentOrganizersCount(eventOrganizerRepository.countByEventId(event.getId()));
                    dto.setSectors(getEventSectors(event));
                    dto.setIsMySector(isUserHasSectorForEvent(event, filter.getCurrentUserId()));
                    if (event.getPhoto() != null) {
                        dto.setPhoto(eventPhotoService.getPhotoAsBase64(event.getPhoto()));
                    }
                    return dto;
                });
    }

    @Transactional
    public void softDeleteOrganizer(Long organizerId) {
        EventOrganizer organizer = eventOrganizerRepository.findById(organizerId)
                .orElseThrow(() -> new ValidationException("Организатор не найден: " + organizerId));

        organizer.setIsDeleted(true);
        eventOrganizerRepository.save(organizer);
        log.info("Organizer {} soft deleted", organizerId);
    }

    /**
     * Восстановление организатора
     */
    @Transactional
    public void restoreOrganizer(Long organizerId) {
        EventOrganizer organizer = eventOrganizerRepository.findById(organizerId)
                .orElseThrow(() -> new ValidationException("Организатор не найден: " + organizerId));

        organizer.setIsDeleted(false);
        eventOrganizerRepository.save(organizer);
        log.info("Organizer {} restored", organizerId);
    }

    /**
     * Мягкое удаление роли
     */
    @Transactional
    public void softDeleteEventRole(Long roleId) {
        EventRole role = eventRoleRepository.findById(roleId)
                .orElseThrow(() -> new ValidationException("Роль не найдена: " + roleId));

        role.setDeleted(true);
        eventRoleRepository.save(role);
        log.info("Event role {} soft deleted", roleId);
    }

    /**
     * Восстановление роли
     */
    @Transactional
    public void restoreEventRole(Long roleId) {
        EventRole role = eventRoleRepository.findById(roleId)
                .orElseThrow(() -> new ValidationException("Роль не найдена: " + roleId));

        role.setDeleted(false);
        eventRoleRepository.save(role);
        log.info("Event role {} restored", roleId);
    }

    /**
     * Массовое мягкое удаление организаторов
     */
    @Transactional
    public void softDeleteOrganizers(List<Long> organizerIds) {
        for (Long organizerId : organizerIds) {
            try {
                softDeleteOrganizer(organizerId);
            } catch (Exception e) {
                log.error("Failed to delete organizer {}: {}", organizerId, e.getMessage());
            }
        }
    }

    /**
     * Массовое мягкое удаление ролей
     */
    @Transactional
    public void softDeleteEventRoles(List<Long> roleIds) {
        for (Long roleId : roleIds) {
            try {
                softDeleteEventRole(roleId);
            } catch (Exception e) {
                log.error("Failed to delete role {}: {}", roleId, e.getMessage());
            }
        }
    }

        // ==================== Private Helper Methods ====================

    private void validateIsEventCreator(Long eventId, Long userId) {
        validateState(eventRepository.existsByIdAndEventCreatorId(eventId, userId),
                () -> new UnauthorizedException("Вы не являетесь создателем этого мероприятия"),
                "User " + userId + " is not creator of event " + eventId);
    }

    private String savePhoto(String base64Photo) {
        if (base64Photo == null || base64Photo.isEmpty()) return null;
        try {
            return eventPhotoService.savePhotoFromBase64(base64Photo);
        } catch (IOException e) {
            log.error("Failed to save photo", e);
            throw new RuntimeException("Ошибка при сохранении фото", e);
        }
    }

    private void deletePhoto(String photoPath) {
        if (photoPath == null || photoPath.isEmpty()) return;
        try {
            eventPhotoService.deletePhoto(photoPath);
        } catch (IOException e) {
            log.error("Failed to delete photo: {}", photoPath, e);
        }
    }

    private void updateEventPhoto(Event event, String newPhotoBase64) {
        deletePhoto(event.getPhoto());
        event.setPhoto(savePhoto(newPhotoBase64));
    }

    private void updateOrganizers(Event event, List<Long> newOrganizerIds) {
        eventOrganizerRepository.deleteByEventId(event.getId());
        addOrganizersToEvent(event, newOrganizerIds);
    }

    private void addOrganizersToEvent(Event event, List<Long> organizerIds) {
        if (organizerIds == null || organizerIds.isEmpty()) return;

        Set<Long> uniqueOrganizerIds = organizerIds.stream()
                .filter(id -> !id.equals(event.getEventCreator().getId()))
                .distinct()
                .collect(Collectors.toSet());

        for (Long organizerId : uniqueOrganizerIds) {
            User organizer = findEntityOrThrow(organizerId, userRepository::findById,
                    () -> new UserDoesNotExistException("Пользователь не найден"), "Organizer");

            validateState(isAllowedOrganizerRole(organizer),
                    () -> new UnauthorizedException("Пользователь не может быть организатором"),
                    "User not allowed as organizer");

            if (!eventOrganizerRepository.existsByEventIdAndUserId(event.getId(), organizerId)) {
                EventOrganizer eventOrganizer = EventOrganizer.builder()
                        .event(event).user(organizer).build();
                eventOrganizerRepository.save(eventOrganizer);
            }
        }
    }

    private boolean isAllowedOrganizerRole(User user) {
        return ALLOWED_ROLES.contains(user.getRole().getTitle())
                || "Activist".equals(user.getRole().getTitle());
    }

    @Transactional(readOnly = true)
    public long getOrganizersCount(Long eventId) {
        return eventOrganizerRepository.countByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public Event getEventById(Long eventId) {
        return findEntityOrThrow(eventId, eventRepository::findById,
                () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");
    }

    @Transactional(readOnly = true)
    public int getAvailableOrganizerSlots(Long eventId) {
        Event event = getEventById(eventId);

        long currentOrganizers = eventOrganizerRepository.countByEventId(eventId);

        if (event.getMaxOrganizersCount() <= UNLIMITED_ORGANIZERS) {
            return Integer.MAX_VALUE;
        }

        return (int) Math.max(0, event.getMaxOrganizersCount() - currentOrganizers);
    }

    @Transactional(readOnly = true)
    public String getAvailableOrganizerSlotsInfo(Long eventId) {
        Event event = getEventById(eventId);

        long currentOrganizers = eventOrganizerRepository.countByEventId(eventId);

        if (event.getMaxOrganizersCount() <= UNLIMITED_ORGANIZERS) {
            return "Неограниченное количество организаторов (текущее: " + currentOrganizers + ")";
        }

        int available = Math.max(0, event.getMaxOrganizersCount() - (int) currentOrganizers);
        int maxOrganizers = event.getMaxOrganizersCount();

        return "Свободно мест: " + available + " из " + maxOrganizers +
                " (занято: " + currentOrganizers + ")";
    }

    @Transactional(readOnly = true)
    public Integer getMaxOrganizersCount(Long eventId) {
        Event event = getEventById(eventId);
        return event.getMaxOrganizersCount() != null ? event.getMaxOrganizersCount() : 0;
    }
}
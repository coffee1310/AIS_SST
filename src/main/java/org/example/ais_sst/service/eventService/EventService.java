package org.example.ais_sst.service.eventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.config.PointsConfig;
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
import java.util.ArrayList;
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
    private final EventSectorRepository eventSectorRepository;

    private final PointsConfig pointsConfig;

    private static final Set<String> HIGHEST_ROLES = Set.of(
            "Administrator", "Curator", "Deputy_chairman", "Chairman"
    );

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

            response.setCurrentParticipantsCount(eventParticipantsRepository.countByEventIdAndIsDeletedFalseAndIsDeletedFalse(eventId));
            response.setCurrentOrganizersCount(eventOrganizerRepository.countByEventIdAndIsDeletedFalse(eventId));

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
            response.setCurrentOrganizersCount(eventOrganizerRepository.countByEventIdAndIsDeletedFalse(savedEvent.getId()));
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

            User user = userRepository.findUserById(userId)
                    .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден"));
            String user_role = user.getRole().getTitle();

            if (!HIGHEST_ROLES.contains(user_role))
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

            if (dto.getMaxOrganizersCount() != null) {
                event.setMaxOrganizersCount(dto.getMaxOrganizersCount());
            }

            // Обновление секторов
            if (dto.getSectorIds() != null) {
                log.info("Updating sectors: {}", dto.getSectorIds());

                // Удаляем все существующие связи EventSector
                if (event.getEventSectors() != null && !event.getEventSectors().isEmpty()) {
                    // Используем отдельный репозиторий для удаления
                    eventSectorRepository.deleteAll(event.getEventSectors());
                    event.getEventSectors().clear();
                }

                // Добавляем новые сектора
                for (Long sectorId : dto.getSectorIds()) {
                    Sector sector = findEntityOrThrow(sectorId, sectorRepository::findById,
                            () -> new IllegalArgumentException("Сектор не найден: " + sectorId), "Sector");

                    // Проверяем, не существует ли уже такая связь
                    boolean exists = eventSectorRepository.existsByEventIdAndSectorId(eventId, sectorId);
                    if (!exists) {
                        event.addSector(sector);
                    }
                }
            }

            if (event.getMaxOrganizersCount() < event.getOrganizers().size()) {
                throw new OrganizerLimitExceededException("Вы указали некорректное максимальное количество организаторов");
            }

            if (event.getMaxOrganizersCount() == null) {
                event.setMaxOrganizersCount(event.getOrganizers().size());
            }

            log.info("Saving event with title: '{}'", event.getTitle());
            Event updatedEvent = eventRepository.save(event);
            log.info("Event saved successfully with ID: {}", updatedEvent.getId());

            EventResponseDTO response = eventMapper.toResponseDto(updatedEvent);
            response.setCurrentParticipantsCount(eventParticipantsRepository.countByEventIdAndIsDeletedFalseAndIsDeletedFalse(eventId));
            response.setCurrentOrganizersCount(eventOrganizerRepository.countByEventIdAndIsDeletedFalse(eventId));
            response.setSectors(getEventSectors(updatedEvent));
            response.setIsMySector(isUserHasSectorForEvent(updatedEvent, userId));

            return response;
        }, "updateEvent", eventId, userId);
    }

    private void validateSectorsRequired(EventCreateDTO dto) {
        if (Boolean.FALSE.equals(dto.getIsPublic()) &&
                (dto.getSectorIds() == null || dto.getSectorIds().isEmpty()) && dto.getIsFreeEvent()) {
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
            response.setCurrentParticipantsCount(eventParticipantsRepository.countByEventIdAndIsDeletedFalseAndIsDeletedFalse(eventId));
            response.setCurrentOrganizersCount(eventOrganizerRepository.countByEventIdAndIsDeletedFalse(eventId));
            // ДОБАВЛЯЕМ СЕКТОРА
            response.setSectors(getEventSectors(savedEvent));

            return response;
        }, "completeEvent", eventId, userId);
    }

    @Transactional
    public EventResponseDTO addOrganizer(Long eventId, Long organizerId, Long userId) {
        return executeWithLogging(() -> {
            User user = userRepository.findUserById(userId)
                    .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден"));
            String user_role = user.getRole().getTitle();

            if (!HIGHEST_ROLES.contains(user_role))
                validateIsEventCreator(eventId, userId);

            Event event = findEntityOrThrow(eventId, eventRepository::findById,
                    () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

            User organizer = findEntityOrThrow(organizerId, userRepository::findById,
                    () -> new UserDoesNotExistException("Пользователь не найден"), "User");

            // Проверяем, существует ли уже активный организатор
            if (eventOrganizerRepository.existsByEventIdAndUserIdAndIsDeletedFalse(eventId, organizerId)) {
                throw new IllegalArgumentException("Пользователь уже является организатором");
            }

            // Ищем удаленного организатора для восстановления
            EventOrganizer eventOrganizer = eventOrganizerRepository
                    .findByEventIdAndUserIdAndIsDeleted(eventId, organizerId, true)
                    .orElse(null);

            if (eventOrganizer != null) {
                // Восстанавливаем удаленного организатора
                eventOrganizer.setIsDeleted(false);
            } else {
                // Создаем нового организатора
                eventOrganizer = EventOrganizer.builder()
                        .event(event)
                        .user(organizer)
                        .wasPresent(false)
                        .totalPoints(pointsConfig.getDefaultOrganizerPoints())
                        .isDeleted(false)
                        .totalPoints(pointsConfig.getDefaultOrganizerPoints())
                        .build();
            }

            eventOrganizerRepository.save(eventOrganizer);

            EventResponseDTO response = eventMapper.toResponseDto(event);

            // Добавляем текущее количество участников и организаторов
            response.setCurrentParticipantsCount(eventParticipantsRepository.countByEventIdAndIsDeletedFalseAndIsDeletedFalse(eventId));
            response.setCurrentOrganizersCount(eventOrganizerRepository.countByEventIdAndIsDeletedFalse(eventId));
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

            long organizersCount = eventOrganizerRepository.countByEventIdAndIsDeletedFalse(eventId);
            validateState(organizersCount > 1,
                    () -> new IllegalArgumentException("Нельзя удалить единственного организатора"),
                    "Cannot remove last organizer");

            eventOrganizerRepository.deleteByEventIdAndUserId(eventId, organizerId);

            EventResponseDTO response = eventMapper.toResponseDto(event);

            // Добавляем текущее количество участников и организаторов
            response.setCurrentParticipantsCount(eventParticipantsRepository.countByEventIdAndIsDeletedFalseAndIsDeletedFalse(eventId));
            response.setCurrentOrganizersCount(eventOrganizerRepository.countByEventIdAndIsDeletedFalse(eventId));
            // ДОБАВЛЯЕМ СЕКТОРА
            response.setSectors(getEventSectors(event));

            return response;
        }, "removeOrganizer", eventId, organizerId, userId);
    }

    @Transactional
    public void deleteEvent(Long eventId, Long userId) {
        executeVoidWithLogging(() -> {
            User user = userRepository.findUserById(userId)
                    .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден"));
            String user_role = user.getRole().getTitle();
            if (!HIGHEST_ROLES.contains(user_role))
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
                    dto.setCurrentParticipantsCount(eventParticipantsRepository.countByEventIdAndIsDeletedFalseAndIsDeletedFalse(event.getId()));
                    dto.setCurrentOrganizersCount(eventOrganizerRepository.countByEventIdAndIsDeletedFalse(event.getId()));
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
                    dto.setCurrentParticipantsCount(eventParticipantsRepository.countByEventIdAndIsDeletedFalseAndIsDeletedFalse(event.getId()));
                    dto.setCurrentOrganizersCount(eventOrganizerRepository.countByEventIdAndIsDeletedFalse(event.getId()));
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
        if (newOrganizerIds == null) {
            return;
        }

        // Получаем текущих организаторов
        List<EventOrganizer> currentOrganizers = event.getOrganizers();

        // Получаем ID текущих организаторов
        List<Long> currentOrganizerIds = currentOrganizers.stream()
                .map(EventOrganizer::getUser)
                .map(User::getId)
                .collect(Collectors.toList());

        // Находим организаторов для удаления (которые есть в текущих, но нет в новых)
        List<EventOrganizer> toRemove = currentOrganizers.stream()
                .filter(org -> !newOrganizerIds.contains(org.getUser().getId()))
                .collect(Collectors.toList());

        // Находим организаторов для добавления (которые есть в новых, но нет в текущих)
        List<Long> toAdd = newOrganizerIds.stream()
                .filter(id -> !currentOrganizerIds.contains(id))
                .collect(Collectors.toList());

        // Удаляем организаторов, которых нет в новом списке
        if (!toRemove.isEmpty()) {
            log.info("Removing organizers: {}", toRemove.stream()
                    .map(org -> org.getUser().getId())
                    .collect(Collectors.toList()));
            event.getOrganizers().removeAll(toRemove);
        }

        // Добавляем новых организаторов
        if (!toAdd.isEmpty()) {
            log.info("Adding organizers: {}", toAdd);
            for (Long userId : toAdd) {
                User user = findEntityOrThrow(userId, userRepository::findById,
                        () -> new UserDoesNotExistException("Пользователь не найден: " + userId), "User");

                EventOrganizer organizer = EventOrganizer.builder()
                        .event(event)
                        .user(user)
                        .wasPresent(false)
                        .totalPoints(pointsConfig.getDefaultOrganizerPoints())
                        .isDeleted(false)
                        .build();

                event.getOrganizers().add(organizer);
            }
        }

        if (toRemove.isEmpty() && toAdd.isEmpty()) {
            log.info("Organizers list unchanged, no updates needed");
        }
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
        return eventOrganizerRepository.countByEventIdAndIsDeletedFalse(eventId);
    }

    @Transactional(readOnly = true)
    public Event getEventById(Long eventId) {
        return findEntityOrThrow(eventId, eventRepository::findById,
                () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");
    }

    @Transactional(readOnly = true)
    public int getAvailableOrganizerSlots(Long eventId) {
        Event event = getEventById(eventId);

        long currentOrganizers = eventOrganizerRepository.countByEventIdAndIsDeletedFalse(eventId);

        if (event.getMaxOrganizersCount() <= UNLIMITED_ORGANIZERS) {
            return Integer.MAX_VALUE;
        }

        return (int) Math.max(0, event.getMaxOrganizersCount() - currentOrganizers);
    }

    @Transactional(readOnly = true)
    public String getAvailableOrganizerSlotsInfo(Long eventId) {
        Event event = getEventById(eventId);

        long currentOrganizers = eventOrganizerRepository.countByEventIdAndIsDeletedFalse(eventId);

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

    @Transactional
    public EventOrganizer addOrganizerManually(Long eventId, Long userId) {
        log.info("Manually adding organizer: event={}, user={}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException("Мероприятие не найдено"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден"));

        // Проверка на дубликат
        if (eventOrganizerRepository.existsByEventIdAndUserIdAndIsDeletedFalse(eventId, userId)) {
            throw new ValidationException("Пользователь уже является организатором этого мероприятия");
        }

        EventOrganizer organizer = EventOrganizer.builder()
                .event(event)
                .user(user)
                .totalPoints(pointsConfig.getDefaultOrganizerPoints())
                .wasPresent(false)
                .isDeleted(false)
                .build();

        EventOrganizer saved = eventOrganizerRepository.save(organizer);
        log.info("Organizer added manually with id: {}", saved.getId());
        return saved;
    }

    /**
     * Массовое ручное добавление организаторов
     */
    @Transactional
    public List<EventOrganizer> addOrganizersManually(Long eventId, List<Long> userIds) {
        log.info("Manually adding {} organizers to event {}", userIds.size(), eventId);

        List<EventOrganizer> created = new ArrayList<>();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException("Мероприятие не найдено"));

        for (Long userId : userIds) {
            try {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден: " + userId));

                if (!eventOrganizerRepository.existsByEventIdAndUserIdAndIsDeletedFalse(eventId, userId)) {
                    EventOrganizer organizer = EventOrganizer.builder()
                            .event(event)
                            .user(user)
                            .totalPoints(pointsConfig.getDefaultOrganizerPoints())
                            .wasPresent(false)
                            .isDeleted(false)
                            .build();
                    created.add(eventOrganizerRepository.save(organizer));
                } else {
                    log.warn("User {} is already an organizer of event {}", userId, eventId);
                }
            } catch (Exception e) {
                log.warn("Failed to add organizer {}: {}", userId, e.getMessage());
            }
        }

        log.info("Added {} organizers manually", created.size());
        return created;
    }
}
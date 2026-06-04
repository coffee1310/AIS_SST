package org.example.ais_sst.service.eventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.events.EventCreateDTO;
import org.example.ais_sst.dto.events.EventFilterDTO;
import org.example.ais_sst.dto.events.EventResponseDTO;
import org.example.ais_sst.dto.events.EventUpdateDTO;
import org.example.ais_sst.entity.Event;
import org.example.ais_sst.entity.EventOrganizer;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.exception.EventDoesNotExistException;
import org.example.ais_sst.exception.OrganizerLimitExceededException;
import org.example.ais_sst.exception.UnauthorizedException;
import org.example.ais_sst.exception.UserDoesNotExistException;
import org.example.ais_sst.mapper.EventMapper;
import org.example.ais_sst.repository.EventOrganizerRepository;
import org.example.ais_sst.repository.EventParticipantsRepository;
import org.example.ais_sst.repository.EventRepository;
import org.example.ais_sst.repository.UserRepository;
import org.example.ais_sst.service.base.BaseEntityService;
import org.example.ais_sst.specification.EventSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService extends BaseEntityService {

    private final EventRepository eventRepository;
    private final EventOrganizerRepository eventOrganizerRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final EventPhotoService eventPhotoService;
    private final EventParticipantsRepository eventParticipantsRepository;

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "Administrator", "Curator", "Deputy_chairman",
            "Sector_coordinator", "Chairman"
    );

    @Transactional(readOnly = true)
    public EventResponseDTO getEventById(Long eventId) {
        return executeWithLogging(() -> {
            Event event = findEntityOrThrow(eventId, eventRepository::findById,
                    () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

            EventResponseDTO response = eventMapper.toResponseDto(event);

            // Добавляем текущее количество участников и организаторов
            response.setCurrentParticipantsCount(eventParticipantsRepository.countByEventId(eventId));
            response.setCurrentOrganizersCount(eventOrganizerRepository.countByEventId(eventId));

            if (event.getPhoto() != null && !event.getPhoto().isEmpty()) {
                String base64Photo = eventPhotoService.getPhotoAsBase64(event.getPhoto());
                response.setPhoto(base64Photo);
            }

            response.setCurrentParticipantsCount(eventParticipantsRepository.countByEventId(eventId));
            response.setCurrentOrganizersCount(eventOrganizerRepository.countByEventId(eventId));

            return response;
        }, "getEventById", eventId);
    }

    @Transactional
    public EventResponseDTO createEvent(EventCreateDTO dto, Long creatorId) {
        return executeWithLogging(() -> {
            User creator = findEntityOrThrow(creatorId, userRepository::findById,
                    () -> new UserDoesNotExistException("Пользователь не найден"), "User");

            validateState(ALLOWED_ROLES.contains(creator.getRole().getTitle()),
                    () -> new UnauthorizedException("У вас нет прав для создания мероприятий"),
                    "User not allowed to create events");

            Event event = eventMapper.toEntity(dto);
            event.setPhoto(savePhoto(dto.getPhoto()));
            event.setEventCreator(creator);

            if (event.getMaxOrganizersCount() < event.getOrganizers().size()) {throw new OrganizerLimitExceededException("Вы указали некорректное максимальное количество организаторов");}

            if (event.getMaxOrganizersCount() == null) {event.setMaxOrganizersCount(event.getOrganizers().size());}

            Event savedEvent = eventRepository.save(event);
            addOrganizersToEvent(savedEvent, dto.getOrganizerIds());

            EventResponseDTO response = eventMapper.toResponseDto(savedEvent);

            response.setCurrentParticipantsCount(0L);
            response.setCurrentOrganizersCount(eventOrganizerRepository.countByEventId(savedEvent.getId()));

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

            if (event.getMaxOrganizersCount() < event.getOrganizers().size()) {throw new OrganizerLimitExceededException("Вы указали некорректное максимальное количество организаторов");}

            if (event.getMaxOrganizersCount() == null) {event.setMaxOrganizersCount(event.getOrganizers().size());}

            log.info("Saving event with title: '{}'", event.getTitle());
            Event updatedEvent = eventRepository.save(event);
            log.info("Event saved successfully with ID: {}", updatedEvent.getId());

            EventResponseDTO response = eventMapper.toResponseDto(updatedEvent);

            // Добавляем текущее количество участников и организаторов
            response.setCurrentParticipantsCount(eventParticipantsRepository.countByEventId(eventId));
            response.setCurrentOrganizersCount(eventOrganizerRepository.countByEventId(eventId));

            return response;
        }, "updateEvent", eventId, userId);
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

            EventResponseDTO response = eventMapper.toResponseDto(event);

            // Добавляем текущее количество участников и организаторов
            response.setCurrentParticipantsCount(eventParticipantsRepository.countByEventId(eventId));
            response.setCurrentOrganizersCount(eventOrganizerRepository.countByEventId(eventId));

            return response;
        }, "removeOrganizer", eventId, organizerId, userId);
    }

    @Transactional
    public void deleteEvent(Long eventId, Long userId) {
        executeVoidWithLogging(() -> {
            validateIsEventCreator(eventId, userId);

            Event event = findEntityOrThrow(eventId, eventRepository::findById,
                    () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

            event.setIsActive(false);
            eventRepository.save(event);
        }, "deleteEvent", eventId, userId);
    }

    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getAllEvents(Pageable pageable) {
        return eventRepository.findByIsActiveTrue(pageable)
                .map(eventMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getEventsByCreator(Long creatorId, Pageable pageable) {
        return eventRepository.findByEventCreatorId(creatorId, pageable)
                .map(event -> {
                    EventResponseDTO dto = eventMapper.toResponseDto(event);
                    dto.setCurrentParticipantsCount(eventParticipantsRepository.countByEventId(event.getId()));
                    dto.setCurrentOrganizersCount(eventOrganizerRepository.countByEventId(event.getId()));
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
                    if (event.getPhoto() != null) {
                        dto.setPhoto(eventPhotoService.getPhotoAsBase64(event.getPhoto()));
                    }
                    return dto;
                });
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

    private void updateEventFields(Event event, EventUpdateDTO dto) {
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getDateOfEvent() != null) event.setDateOfEvent(dto.getDateOfEvent());
        if (dto.getStartTime() != null) event.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null) event.setEndTime(dto.getEndTime());
        if (dto.getVenue() != null) event.setVenue(dto.getVenue());
        if (dto.getReferenceToPosition() != null) event.setReferenceToPosition(dto.getReferenceToPosition());
        if (dto.getIsPublic() != null) event.setIsPublic(dto.getIsPublic());
        if (dto.getIsDraft() != null) event.setIsDraft(dto.getIsDraft());
        if (dto.getIsActive() != null) event.setIsActive(dto.getIsActive());
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
}
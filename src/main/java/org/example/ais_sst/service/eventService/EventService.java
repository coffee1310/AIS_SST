package org.example.ais_sst.service.eventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.events.EventCreateDTO;
import org.example.ais_sst.dto.events.EventResponseDTO;
import org.example.ais_sst.dto.events.EventUpdateDTO;
import org.example.ais_sst.entity.Event;
import org.example.ais_sst.entity.EventOrganizer;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.exception.EventDoesNotExistException;
import org.example.ais_sst.exception.OrganizerDoesNotExistException;
import org.example.ais_sst.exception.UnauthorizedException;
import org.example.ais_sst.exception.UserDoesNotExistException;
import org.example.ais_sst.mapper.EventMapper;
import org.example.ais_sst.repository.EventOrganizerRepository;
import org.example.ais_sst.repository.EventRepository;
import org.example.ais_sst.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventOrganizerRepository eventOrganizerRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final EventPhotoService eventPhotoService;

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "Administrator", "Curator", "Deputy_chairman",
            "Sector_coordinator", "Chairman"
    );

    private void checkCanCreateEvent(User user) {
        String role = user.getRole().getTitle();
        if (!ALLOWED_ROLES.contains(role)) {
            throw new UnauthorizedException(
                    "У вас нет прав для создания мероприятий. Требуются роли: " + ALLOWED_ROLES
            );
        }
    }

    private void checkIsEventCreator(Long eventId, Long userId) {
        if (!eventRepository.existsByIdAndEventCreatorId(eventId, userId)) {
            throw new UnauthorizedException("Вы не являетесь создателем этого мероприятия");
        }
    }

    @Transactional(readOnly = true)
    public EventResponseDTO getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException("Мероприятие с id " + eventId + " не найдено"));
        return eventMapper.toResponseDto(event);
    }

    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getAllEvents(Pageable pageable) {
        return eventRepository.findByIsActiveTrue(pageable)
                .map(eventMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getEventsByCreator(Long creatorId, Pageable pageable) {
        return eventRepository.findByEventCreatorId(creatorId, pageable)
                .map(eventMapper::toResponseDto);
    }

    /**
     * Создание мероприятия (ОДИН метод)
     */
    @Transactional
    public EventResponseDTO createEvent(EventCreateDTO dto, Long creatorId) {
        log.info("Creating event: {} by user: {}", dto.getTitle(), creatorId);

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден"));
        checkCanCreateEvent(creator);

        Event event = eventMapper.toEntity(dto);

        // Сохраняем фото если есть
        if (dto.getPhoto() != null && !dto.getPhoto().isEmpty()) {
            try {
                String photoPath = eventPhotoService.savePhotoFromBase64(dto.getPhoto());
                event.setPhoto(photoPath);  // Используем setPhoto, а не setPhotoPath
            } catch (IOException e) {
                log.error("Failed to save photo", e);
                throw new RuntimeException("Ошибка при сохранении фото", e);
            }
        }

        event.setEventCreator(creator);
        Event savedEvent = eventRepository.save(event);

        // Добавляем организаторов
        if (dto.getOrganizerIds() != null && !dto.getOrganizerIds().isEmpty()) {
            for (Long organizerId : dto.getOrganizerIds()) {
                User organizer = userRepository.findById(organizerId)
                        .orElseThrow(() -> new OrganizerDoesNotExistException("Организатор с id " + organizerId + " не найден"));

                EventOrganizer eventOrganizer = EventOrganizer.builder()
                        .event(savedEvent)
                        .user(organizer)
                        .build();

                eventOrganizerRepository.save(eventOrganizer);
            }
        }

        log.info("Event created successfully with id: {}", savedEvent.getId());
        return getEventById(savedEvent.getId());
    }

    /**
     * Обновление мероприятия (ОДИН метод)
     */
    @Transactional
    public EventResponseDTO updateEvent(Long eventId, EventUpdateDTO dto, Long userId) {
        log.info("Updating event: {} by user: {}", eventId, userId);

        checkIsEventCreator(eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException("Мероприятие не найдено"));

        // Обновляем поля
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getDateOfEvent() != null) event.setDateOfEvent(dto.getDateOfEvent());
        if (dto.getStartTime() != null) event.setStartTime(LocalDateTime.from(dto.getStartTime()));
        if (dto.getEndTime() != null) event.setEndTime(LocalDateTime.from(dto.getEndTime()));
        if (dto.getVenue() != null) event.setVenue(dto.getVenue());
        if (dto.getReferenceToPosition() != null) event.setReferenceToPosition(dto.getReferenceToPosition());
        if (dto.getIsPublic() != null) event.setIsPublic(dto.getIsPublic());
        if (dto.getIsDraft() != null) event.setIsDraft(dto.getIsDraft());
        if (dto.getIsActive() != null) event.setIsActive(dto.getIsActive());

        // Обновляем фото если есть
        if (dto.getPhoto() != null && !dto.getPhoto().isEmpty()) {
            try {
                // Удаляем старое фото если есть
                if (event.getPhoto() != null) {
                    eventPhotoService.deletePhoto(event.getPhoto());
                }
                String photoPath = eventPhotoService.savePhotoFromBase64(dto.getPhoto());
                event.setPhoto(photoPath);  // Используем setPhoto
            } catch (IOException e) {
                log.error("Failed to save photo", e);
                throw new RuntimeException("Ошибка при сохранении фото", e);
            }
        }

        // Обновляем организаторов
        if (dto.getOrganizerIds() != null) {
            eventOrganizerRepository.deleteByEventId(eventId);
            for (Long organizerId : dto.getOrganizerIds()) {
                User organizer = userRepository.findById(organizerId)
                        .orElseThrow(() -> new OrganizerDoesNotExistException("Организатор с id " + organizerId + " не найден"));

                EventOrganizer eventOrganizer = EventOrganizer.builder()
                        .event(event)
                        .user(organizer)
                        .build();

                eventOrganizerRepository.save(eventOrganizer);
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Event updated successfully: {}", eventId);

        return getEventById(eventId);
    }

    /**
     * Завершение мероприятия
     */
    @Transactional
    public EventResponseDTO completeEvent(Long eventId, Long userId) {
        log.info("Completing event: {} by user: {}", eventId, userId);

        checkIsEventCreator(eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException("Мероприятие не найдено"));

        if (event.getIsCompleted() != null && event.getIsCompleted()) {
            throw new IllegalStateException("Мероприятие уже завершено");
        }

        event.setIsCompleted(true);
        event.setIsActive(false);

        Event completedEvent = eventRepository.save(event);
        log.info("Event completed successfully: {}", eventId);

        return getEventById(eventId);
    }

    @Transactional
    public EventResponseDTO addOrganizer(Long eventId, Long organizerId, Long userId) {
        log.info("Adding organizer {} to event {}", organizerId, eventId);

        checkIsEventCreator(eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException("Мероприятие не найдено"));

        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден"));

        if (eventOrganizerRepository.existsByEventIdAndUserId(eventId, organizerId)) {
            throw new IllegalArgumentException("Пользователь уже является организатором");
        }

        EventOrganizer eventOrganizer = EventOrganizer.builder()
                .event(event)
                .user(organizer)
                .build();

        eventOrganizerRepository.save(eventOrganizer);
        log.info("Organizer added successfully");

        return getEventById(eventId);
    }

    @Transactional
    public EventResponseDTO removeOrganizer(Long eventId, Long organizerId, Long userId) {
        log.info("Removing organizer {} from event {}", organizerId, eventId);

        checkIsEventCreator(eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException("Мероприятие не найдено"));

        long organizersCount = eventOrganizerRepository.countByEventId(eventId);
        if (organizersCount <= 1) {
            throw new IllegalArgumentException("Нельзя удалить единственного организатора мероприятия");
        }

        eventOrganizerRepository.deleteByEventIdAndUserId(eventId, organizerId);
        log.info("Organizer removed successfully");

        return getEventById(eventId);
    }

    @Transactional
    public void deleteEvent(Long eventId, Long userId) {
        log.info("Deleting event: {} by user: {}", eventId, userId);

        checkIsEventCreator(eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException("Мероприятие не найдено"));

        event.setIsActive(false);
        eventRepository.save(event);
        log.info("Event deactivated successfully: {}", eventId);
    }

    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getEventsWithFilters(
            String title,
            String venue,
            LocalDate dateFrom,
            LocalDate dateTo,
            Boolean isPublic,
            Boolean isDraft,
            Boolean isCompleted,
            Boolean isActive,
            Long creatorId,
            Pageable pageable) {

        log.info("Getting events with filters");

        Page<Event> events = eventRepository.findAllWithFilters(
                title, venue, dateFrom, dateTo, isPublic, isDraft, isCompleted, isActive, creatorId, pageable);

        return events.map(eventMapper::toResponseDto);
    }
}
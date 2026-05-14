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
import org.example.ais_sst.exception.OrganizerDoesNotExistException;
import org.example.ais_sst.exception.UnauthorizedException;
import org.example.ais_sst.exception.UserDoesNotExistException;
import org.example.ais_sst.mapper.EventMapper;
import org.example.ais_sst.repository.EventOrganizerRepository;
import org.example.ais_sst.repository.EventRepository;
import org.example.ais_sst.repository.UserRepository;
import org.example.ais_sst.specification.EventSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден"));
    }

    private Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException("Мероприятие с id " + eventId + " не найдено"));
    }

    private void checkCanCreateEvent(User user) {
        if (!ALLOWED_ROLES.contains(user.getRole().getTitle())) {
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

    private String savePhoto(String base64Photo) {
        if (base64Photo == null || base64Photo.isEmpty()) {
            return null;
        }
        try {
            return eventPhotoService.savePhotoFromBase64(base64Photo);
        } catch (IOException e) {
            log.error("Failed to save photo", e);
            throw new RuntimeException("Ошибка при сохранении фото", e);
        }
    }

    private void deletePhoto(String photoPath) {
        if (photoPath == null || photoPath.isEmpty()) {
            return;
        }
        try {
            eventPhotoService.deletePhoto(photoPath);
        } catch (IOException e) {
            log.error("Failed to delete photo: {}", photoPath, e);
        }
    }

    private void addOrganizersToEvent(Event event, List<Long> organizerIds) {
        if (organizerIds == null || organizerIds.isEmpty()) {
            return;
        }

        // Убираем дубликаты и создателя
        Set<Long> uniqueOrganizerIds = organizerIds.stream()
                .filter(id -> !id.equals(event.getEventCreator().getId())) // Исключаем создателя
                .distinct()
                .collect(Collectors.toSet());

        for (Long organizerId : uniqueOrganizerIds) {
            User organizer = findUserById(organizerId);

            // Проверка прав организатора
            if (!isAllowedOrganizerRole(organizer)) {
                throw new UnauthorizedException(
                        "Пользователь " + organizer.getStudentEmail() +
                                " не может быть организатором мероприятия"
                );
            }

            // Проверка на существующую связь
            if (!eventOrganizerRepository.existsByEventIdAndUserId(event.getId(), organizerId)) {
                EventOrganizer eventOrganizer = EventOrganizer.builder()
                        .event(event)
                        .user(organizer)
                        .build();
                eventOrganizerRepository.save(eventOrganizer);
            }
        }
    }

    private boolean isAllowedOrganizerRole(User user) {
        return ALLOWED_ROLES.contains(user.getRole().getTitle())
                || "Activist".equals(user.getRole().getTitle());
    }

    @Transactional(readOnly = true)
    public EventResponseDTO getEventById(Long eventId) {
        Event event = findEventById(eventId);

        log.info("Getting event with id: {}", eventId);
        log.info("Event photo path: {}", event.getPhoto());

        EventResponseDTO response = eventMapper.toResponseDto(event);

        // Конвертируем фото в Base64
        if (event.getPhoto() != null && !event.getPhoto().isEmpty()) {
            log.info("Photo path exists, attempting to convert to base64");
            String base64Photo = eventPhotoService.getPhotoAsBase64(event.getPhoto());
            if (base64Photo != null) {
                response.setPhoto(base64Photo);
                log.info("Photo converted successfully, base64 length: {}", base64Photo.length());
            } else {
                log.warn("Failed to convert photo to base64");
            }
        } else {
            log.warn("Photo path is null or empty for event: {}", eventId);
        }

        return response;
    }

    @Transactional
    public EventResponseDTO createEvent(EventCreateDTO dto, Long creatorId) {
        log.info("Creating event: {} by user: {}", dto.getTitle(), creatorId);

        User creator = findUserById(creatorId);
        checkCanCreateEvent(creator);

        Event event = eventMapper.toEntity(dto);
        event.setPhoto(savePhoto(dto.getPhoto()));
        event.setEventCreator(creator);

        Event savedEvent = eventRepository.save(event);
        addOrganizersToEvent(savedEvent, dto.getOrganizerIds());

        log.info("Event created successfully with id: {}", savedEvent.getId());
        return eventMapper.toResponseDto(savedEvent);
    }

    @Transactional
    public EventResponseDTO updateEvent(Long eventId, EventUpdateDTO dto, Long userId) {
        log.info("Updating event: {} by user: {}", eventId, userId);

        checkIsEventCreator(eventId, userId);
        Event event = findEventById(eventId);

        // Обновляем простые поля
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

        // Обновляем фото
        if (dto.getPhoto() != null && !dto.getPhoto().isEmpty()) {
            deletePhoto(event.getPhoto());
            event.setPhoto(savePhoto(dto.getPhoto()));
        }

        // Обновляем организаторов
        if (dto.getOrganizerIds() != null) {
            eventOrganizerRepository.deleteByEventId(eventId);
            addOrganizersToEvent(event, dto.getOrganizerIds());
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Event updated successfully: {}", eventId);

        return eventMapper.toResponseDto(updatedEvent);
    }

    @Transactional
    public EventResponseDTO completeEvent(Long eventId, Long userId) {
        log.info("Completing event: {} by user: {}", eventId, userId);

        checkIsEventCreator(eventId, userId);
        Event event = findEventById(eventId);

        if (Boolean.TRUE.equals(event.getIsCompleted())) {
            throw new IllegalStateException("Мероприятие уже завершено");
        }

        event.setIsCompleted(true);
        event.setIsActive(false);

        Event completedEvent = eventRepository.save(event);
        log.info("Event completed successfully: {}", eventId);

        return eventMapper.toResponseDto(completedEvent);
    }

    @Transactional
    public EventResponseDTO addOrganizer(Long eventId, Long organizerId, Long userId) {
        log.info("Adding organizer {} to event {}", organizerId, eventId);

        checkIsEventCreator(eventId, userId);
        Event event = findEventById(eventId);
        User organizer = findUserById(organizerId);

        if (eventOrganizerRepository.existsByEventIdAndUserId(eventId, organizerId)) {
            throw new IllegalArgumentException("Пользователь уже является организатором");
        }

        EventOrganizer eventOrganizer = EventOrganizer.builder()
                .event(event)
                .user(organizer)
                .build();
        eventOrganizerRepository.save(eventOrganizer);

        log.info("Organizer added successfully");
        return eventMapper.toResponseDto(event);
    }

    @Transactional
    public EventResponseDTO removeOrganizer(Long eventId, Long organizerId, Long userId) {
        log.info("Removing organizer {} from event {}", organizerId, eventId);

        checkIsEventCreator(eventId, userId);
        Event event = findEventById(eventId);

        long organizersCount = eventOrganizerRepository.countByEventId(eventId);
        if (organizersCount <= 1) {
            throw new IllegalArgumentException("Нельзя удалить единственного организатора мероприятия");
        }

        eventOrganizerRepository.deleteByEventIdAndUserId(eventId, organizerId);
        log.info("Organizer removed successfully");

        return eventMapper.toResponseDto(event);
    }

    @Transactional
    public void deleteEvent(Long eventId, Long userId) {
        log.info("Deleting event: {} by user: {}", eventId, userId);

        checkIsEventCreator(eventId, userId);
        Event event = findEventById(eventId);

        event.setIsActive(false);
        eventRepository.save(event);

        log.info("Event deactivated successfully: {}", eventId);
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

    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getEventsWithFilters(EventFilterDTO filter, Pageable pageable) {
        log.info("Getting events with filters from DTO");

        // Используем Specification вместо native query
        Specification<Event> spec = EventSpecification.withFilter(filter);

        Page<Event> eventPage = eventRepository.findAll(spec, pageable);

        return eventPage.map(eventMapper::toResponseDto);
    }

    private List<Event> sortEvents(List<Event> events, Sort sort) {
        if (sort == null || !sort.iterator().hasNext()) {
            return events;
        }

        Comparator<Event> comparator = null;
        for (Sort.Order order : sort) {
            Comparator<Event> propertyComparator = getComparatorForProperty(order.getProperty(), order.isAscending());
            if (comparator == null) {
                comparator = propertyComparator;
            } else {
                comparator = comparator.thenComparing(propertyComparator);
            }
        }

        events.sort(comparator);
        return events;
    }

    private Comparator<Event> getComparatorForProperty(String property, boolean ascending) {
        Comparator<Event> comparator;
        switch (property) {
            case "id":
                comparator = Comparator.comparing(Event::getId);
                break;
            case "title":
                comparator = Comparator.comparing(Event::getTitle, Comparator.nullsLast(String::compareTo));
                break;
            case "dateOfEvent":
                comparator = Comparator.comparing(Event::getDateOfEvent, Comparator.nullsLast(LocalDate::compareTo));
                break;
            case "createdAt":
                comparator = Comparator.comparing(Event::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo));
                break;
            default:
                comparator = Comparator.comparing(Event::getId);
        }
        return ascending ? comparator : comparator.reversed();
    }
}
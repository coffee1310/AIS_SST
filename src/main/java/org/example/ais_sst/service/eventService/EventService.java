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

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "Administrator", "Curator", "Deputy_chairman",
            "Sector_coordinator", "Chairman"
    );

    /**
     * Проверка, может ли пользователь создавать мероприятия
     */
    private void checkCanCreateEvent(User user) {
        String role = user.getRole().getTitle();
        if (!ALLOWED_ROLES.contains(role)) {
            throw new UnauthorizedException(
                    "У вас нет прав для создания мероприятий. Требуются роли: " + ALLOWED_ROLES
            );
        }
    }

    /**
     * Проверка, является ли пользователь создателем мероприятия
     */
    private void checkIsEventCreator(Long eventId, Long userId) {
        if (!eventRepository.existsByIdAndEventCreatorId(eventId, userId)) {
            throw new UnauthorizedException("Вы не являетесь создателем этого мероприятия");
        }
    }

    /**
     * Создание мероприятия
     */
    @Transactional
    public EventResponseDTO createEvent(EventCreateDTO dto, Long creatorId) {
        log.info("Creating event: {} by user: {}", dto.getTitle(), creatorId);

        // Проверяем права создателя
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден"));
        checkCanCreateEvent(creator);

        // Проверяем, что время начала не позже времени окончания
        if (dto.getStartTime().isAfter(dto.getEndTime())) {
            throw new IllegalArgumentException("Время начала не может быть позже времени окончания");
        }

        // Проверяем, что есть хотя бы один организатор
        if (dto.getOrganizerIds() == null || dto.getOrganizerIds().isEmpty()) {
            throw new IllegalArgumentException("Должен быть хотя бы один организатор");
        }

        // Создаем мероприятие
        Event event = eventMapper.toEntity(dto);
        event.setEventCreator(creator);

        Event savedEvent = eventRepository.save(event);

        // Добавляем организаторов
        for (Long organizerId : dto.getOrganizerIds()) {
            User organizer = userRepository.findById(organizerId)
                    .orElseThrow(() -> new OrganizerDoesNotExistException("Организатор с id " + organizerId + " не найден"));

            EventOrganizer eventOrganizer = EventOrganizer.builder()
                    .event(savedEvent)
                    .user(organizer)
                    .build();

            eventOrganizerRepository.save(eventOrganizer);
        }

        log.info("Event created successfully with id: {}", savedEvent.getId());
        return getEventById(savedEvent.getId());
    }

    /**
     * Получение мероприятия по ID
     */
    @Transactional(readOnly = true)
    public EventResponseDTO getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException("Мероприятие с id " + eventId + " не найдено"));
        return eventMapper.toResponseDto(event);
    }

    /**
     * Получение всех мероприятий (с пагинацией)
     */
    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getAllEvents(Pageable pageable) {
        return eventRepository.findByIsActiveTrue(pageable)
                .map(eventMapper::toResponseDto);
    }

    /**
     * Получение мероприятий, созданных пользователем
     */
    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getEventsByCreator(Long creatorId, Pageable pageable) {
        return eventRepository.findByEventCreatorId(creatorId, pageable)
                .map(eventMapper::toResponseDto);
    }

    /**
     * Обновление мероприятия
     */
    @Transactional
    public EventResponseDTO updateEvent(Long eventId, EventUpdateDTO dto, Long userId) {
        log.info("Updating event: {} by user: {}", eventId, userId);

        // Проверяем, что пользователь является создателем
        checkIsEventCreator(eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException("Мероприятие не найдено"));

        // Обновляем поля
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getPhoto() != null) event.setPhoto(dto.getPhoto());
        if (dto.getStartTime() != null) event.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null) event.setEndTime(dto.getEndTime());
        if (dto.getLocation() != null) event.setLocation(dto.getLocation());
        if (dto.getIsActive() != null) event.setIsActive(dto.getIsActive());

        // Обновляем организаторов, если передан новый список
        if (dto.getOrganizerIds() != null) {
            // Удаляем старых организаторов
            eventOrganizerRepository.deleteByEventId(eventId);

            // Добавляем новых
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
     * Добавление организатора к мероприятию
     */
    @Transactional
    public EventResponseDTO addOrganizer(Long eventId, Long organizerId, Long userId) {
        log.info("Adding organizer {} to event {}", organizerId, eventId);

        // Проверяем, что пользователь является создателем
        checkIsEventCreator(eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException("Мероприятие не найдено"));

        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден"));

        // Проверяем, не является ли пользователь уже организатором
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

    /**
     * Удаление организатора из мероприятия
     */
    @Transactional
    public EventResponseDTO removeOrganizer(Long eventId, Long organizerId, Long userId) {
        log.info("Removing organizer {} from event {}", organizerId, eventId);

        // Проверяем, что пользователь является создателем
        checkIsEventCreator(eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException("Мероприятие не найдено"));

        // Проверяем, что после удаления останется хотя бы один организатор
        long organizersCount = eventOrganizerRepository.countByEventId(eventId);
        if (organizersCount <= 1) {
            throw new IllegalArgumentException("Нельзя удалить единственного организатора мероприятия");
        }

        eventOrganizerRepository.deleteByEventIdAndUserId(eventId, organizerId);
        log.info("Organizer removed successfully");

        return getEventById(eventId);
    }

    /**
     * Удаление мероприятия
     */
    @Transactional
    public void deleteEvent(Long eventId, Long userId) {
        log.info("Deleting event: {} by user: {}", eventId, userId);

        // Проверяем, что пользователь является создателем
        checkIsEventCreator(eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException("Мероприятие не найдено"));

        // Мягкое удаление (просто деактивируем)
        event.setIsActive(false);
        eventRepository.save(event);
        log.info("Event deactivated successfully: {}", eventId);
    }
}
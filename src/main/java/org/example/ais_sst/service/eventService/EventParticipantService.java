package org.example.ais_sst.service.eventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.event_participant.EventParticipantResponseDTO;
import org.example.ais_sst.entity.Event;
import org.example.ais_sst.entity.EventParticipant;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.exception.*;
import org.example.ais_sst.mapper.EventParticipantMapper;
import org.example.ais_sst.repository.EventParticipantsRepository;
import org.example.ais_sst.repository.EventRepository;
import org.example.ais_sst.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventParticipantService {

    private final EventParticipantsRepository eventParticipantsRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventParticipantMapper eventParticipantMapper;

    private static final int UNLIMITED_PARTICIPANTS = 0;

    @Transactional
    public EventParticipantResponseDTO becomeEventParticipant(Long eventId, Long userId) {
        Event event = getEventById(eventId);
        User user = getUserById(userId);

        validateParticipantNotExists(eventId, userId);
        validateParticipantsAllowed(event);
        validateParticipantLimit(event);
        validateEventActive(event);
        validateEventNotStarted(event);

        EventParticipant participant = EventParticipant.builder()
                .event(event)
                .user(user)
                .build();

        EventParticipant savedParticipant = eventParticipantsRepository.save(participant);
        log.info("User {} joined event {}", userId, eventId);

        return eventParticipantMapper.toResponseDto(savedParticipant);
    }

    @Transactional
    public void cancelParticipation(Long eventId, Long userId) {
        Event event = getEventById(eventId);

        validateParticipantExists(eventId, userId);
        validateEventNotStarted(event);

        eventParticipantsRepository.deleteByEventIdAndUserId(eventId, userId);
        log.info("User {} left event {}", userId, eventId);
    }


    @Transactional(readOnly = true)
    public List<EventParticipantResponseDTO> getEventParticipants(Long eventId) {
        getEventById(eventId);

        return eventParticipantsRepository.findByEventId(eventId)
                .stream()
                .map(eventParticipantMapper::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventParticipantResponseDTO> getUserEvents(Long userId) {
        getUserById(userId);

        return eventParticipantsRepository.findByUserId(userId)
                .stream()
                .map(eventParticipantMapper::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isParticipant(Long eventId, Long userId) {
        return eventParticipantsRepository.existsByEventIdAndUserId(eventId, userId);
    }

    @Transactional(readOnly = true)
    public long getParticipantsCount(Long eventId) {
        return eventParticipantsRepository.countByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public int getAvailableSlots(Long eventId) {
        Event event = getEventById(eventId);

        if (!Boolean.TRUE.equals(event.getIsFreeEvent())) {
            return 0;
        }

        long currentParticipants = eventParticipantsRepository.countByEventId(eventId);

        if (event.getMaxParticipantsCount() <= UNLIMITED_PARTICIPANTS) {
            return Integer.MAX_VALUE;
        }

        return (int) Math.max(0, event.getMaxParticipantsCount() - currentParticipants);
    }

    @Transactional(readOnly = true)
    public String getAvailableSlotsInfo(Long eventId) {
        Event event = getEventById(eventId);

        if (!Boolean.TRUE.equals(event.getIsFreeEvent())) {
            return "Участие в мероприятии запрещено";
        }

        long currentParticipants = eventParticipantsRepository.countByEventId(eventId);

        if (event.getMaxParticipantsCount() <= UNLIMITED_PARTICIPANTS) {
            return "Неограниченное количество участников (текущее: " + currentParticipants + ")";
        }

        int available = Math.max(0, event.getMaxParticipantsCount() - (int) currentParticipants);
        return String.valueOf(available);
    }

    @Transactional(readOnly = true)
    public EventParticipantResponseDTO getParticipationInfo(Long eventId, Long userId) {
        EventParticipant participant = eventParticipantsRepository
                .findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new ParticipantNotFoundException(
                        String.format("User %d is not participant of event %d", userId, eventId)));

        return eventParticipantMapper.toResponseDto(participant);
    }

    @Transactional
    public void softDeleteParticipant(Long participantId) {
        EventParticipant participant = eventParticipantsRepository.findById(participantId)
                .orElseThrow(() -> new ValidationException("Участник не найден: " + participantId));

        participant.setIsDeleted(true);
        eventParticipantsRepository.save(participant);
        log.info("Participant {} soft deleted", participantId);
    }

    /**
     * Восстановление участника
     */
    @Transactional
    public void restoreParticipant(Long participantId) {
        EventParticipant participant = eventParticipantsRepository.findById(participantId)
                .orElseThrow(() -> new ValidationException("Участник не найден: " + participantId));

        participant.setIsDeleted(false);
        eventParticipantsRepository.save(participant);
        log.info("Participant {} restored", participantId);
    }

    /**
     * Массовое мягкое удаление участников
     */
    @Transactional
    public void softDeleteParticipants(List<Long> participantIds) {
        for (Long participantId : participantIds) {
            try {
                softDeleteParticipant(participantId);
            } catch (Exception e) {
                log.error("Failed to delete participant {}: {}", participantId, e.getMessage());
            }
        }
    }

    // ==================== Private Validation Methods ====================

    private Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException(
                        String.format("Event with id=%d not found", eventId)));
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserDoesNotExistException(
                        String.format("User with id=%d not found", userId)));
    }

    private void validateParticipantNotExists(Long eventId, Long userId) {
        if (eventParticipantsRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new DuplicateParticipantException("User is already a participant of this event");
        }
    }

    private void validateParticipantExists(Long eventId, Long userId) {
        if (!eventParticipantsRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new ParticipantNotFoundException("User is not a participant of this event");
        }
    }

    private void validateParticipantsAllowed(Event event) {
        if (!Boolean.TRUE.equals(event.getIsFreeEvent())) {
            throw new ParticipantsForbiddenException(
                    String.format("Участие в мероприятии '%s' запрещено", event.getTitle()));
        }
    }

    private void validateParticipantLimit(Event event) {
        if (event.getMaxParticipantsCount() <= UNLIMITED_PARTICIPANTS) {
            return;
        }

        long currentParticipants = eventParticipantsRepository.countByEventId(event.getId());

        if (currentParticipants >= event.getMaxParticipantsCount()) {
            throw new ParticipantLimitExceededException(
                    String.format("Participant limit reached for event %d. Max: %d",
                            event.getId(), event.getMaxParticipantsCount()));
        }
    }

    private void validateEventActive(Event event) {
        if (!Boolean.TRUE.equals(event.getIsActive())) {
            throw new EventNotActiveException(
                    String.format("Event %d is not active", event.getId()));
        }
    }

    private void validateEventNotStarted(Event event) {
        LocalDate eventDate = event.getDateOfEvent();
        LocalTime eventStartTime = event.getStartTime();

        if (eventDate != null && eventStartTime != null) {
            LocalDateTime eventStartDateTime = LocalDateTime.of(eventDate, eventStartTime);
            if (eventStartDateTime.isBefore(LocalDateTime.now())) {
                throw new EventAlreadyStartedException(
                        String.format("Event %d has already started", event.getId()));
            }
        }
    }
}
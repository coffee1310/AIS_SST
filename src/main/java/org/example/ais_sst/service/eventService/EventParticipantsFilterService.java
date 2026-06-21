package org.example.ais_sst.service.eventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.event_participation.EventParticipantFilterDTO;
import org.example.ais_sst.dto.event_participation.EventParticipantInfoDTO;
import org.example.ais_sst.entity.*;
import org.example.ais_sst.exception.EventDoesNotExistException;
import org.example.ais_sst.repository.*;
import org.example.ais_sst.service.base.BaseEntityService;
import org.example.ais_sst.specification.EventParticipantSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventParticipantsFilterService extends BaseEntityService {

    private final EventRepository eventRepository;
    private final EventOrganizerRepository eventOrganizerRepository;
    private final EventParticipantsRepository eventParticipantsRepository;
    private final EventParticipationRecordRepository participationRecordRepository;

    /**
     * Получить участников мероприятия с фильтрацией и пагинацией
     */
    @Transactional(readOnly = true)
    public Page<EventParticipantInfoDTO> getEventParticipantsWithFilters(
            EventParticipantFilterDTO filter, Pageable pageable) {

        log.info("Getting event participants with filters: {}", filter);

        List<EventParticipantInfoDTO> allResults = new ArrayList<>();

        // Если eventId не указан - получаем все данные
        if (filter.getEventId() == null) {
            log.warn("eventId is null, returning empty page");
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }

        // Проверяем существование мероприятия
        try {
            findEntityOrThrow(filter.getEventId(), eventRepository::findById,
                    () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");
        } catch (Exception e) {
            log.error("Event not found: {}", e.getMessage());
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }

        // 1. Получаем организаторов (если не указан entityType или указан ORGANIZER)
        if (filter.getEntityType() == null || filter.getEntityType().equals("ORGANIZER")) {
            List<EventOrganizer> organizers = getFilteredOrganizers(filter);
            allResults.addAll(organizers.stream()
                    .map(this::convertOrganizerToDTO)
                    .collect(Collectors.toList()));
        }

        // 2. Получаем участников (если не указан entityType или указан PARTICIPANT)
        if (filter.getEntityType() == null || filter.getEntityType().equals("PARTICIPANT")) {
            List<EventParticipant> participants = getFilteredParticipants(filter);
            allResults.addAll(participants.stream()
                    .map(this::convertParticipantToDTO)
                    .collect(Collectors.toList()));
        }

        // 3. Получаем записи об участии (если не указан entityType или указан PARTICIPATION_RECORD)
        if (filter.getEntityType() == null || filter.getEntityType().equals("PARTICIPATION_RECORD")) {
            List<EventParticipationRecord> records = getFilteredParticipationRecords(filter);
            allResults.addAll(records.stream()
                    .map(this::convertParticipationRecordToDTO)
                    .collect(Collectors.toList()));
        }

        // Применяем пагинацию
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allResults.size());

        if (start > allResults.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, allResults.size());
        }

        List<EventParticipantInfoDTO> pageContent = allResults.subList(start, end);

        return new PageImpl<>(pageContent, pageable, allResults.size());
    }

    private List<EventOrganizer> getFilteredOrganizers(EventParticipantFilterDTO filter) {
        Specification<EventOrganizer> spec = EventParticipantSpecification.organizerWithFilter(filter);
        return eventOrganizerRepository.findAll(spec);
    }

    private List<EventParticipant> getFilteredParticipants(EventParticipantFilterDTO filter) {
        Specification<EventParticipant> spec = EventParticipantSpecification.participantWithFilter(filter);
        return eventParticipantsRepository.findAll(spec);
    }

    private List<EventParticipationRecord> getFilteredParticipationRecords(EventParticipantFilterDTO filter) {
        Specification<EventParticipationRecord> spec = EventParticipantSpecification.withFilter(filter);
        return participationRecordRepository.findAll(spec);
    }

    private EventParticipantInfoDTO convertOrganizerToDTO(EventOrganizer organizer) {
        return EventParticipantInfoDTO.builder()
                .id(organizer.getId())
                .role("ОРГАНИЗАТОР")
                .fullName(organizer.getUser().getName() + " " +
                        organizer.getUser().getSurname() +
                        (organizer.getUser().getPatronymic() != null ?
                                " " + organizer.getUser().getPatronymic() : ""))
                .totalPoints(organizer.getTotalPoints())
                .wasPresent(organizer.getWasPresent())
                .entityType("ORGANIZER")
                .build();
    }

    private EventParticipantInfoDTO convertParticipantToDTO(EventParticipant participant) {
        return EventParticipantInfoDTO.builder()
                .id(participant.getId())
                .role("УЧАСТНИК")
                .fullName(participant.getUser().getName() + " " +
                        participant.getUser().getSurname() +
                        (participant.getUser().getPatronymic() != null ?
                                " " + participant.getUser().getPatronymic() : ""))
                .totalPoints(participant.getTotalPoints())
                .wasPresent(participant.getWasPresent())
                .entityType("PARTICIPANT")
                .build();
    }

    private EventParticipantInfoDTO convertParticipationRecordToDTO(EventParticipationRecord record) {
        String roleName = "ИСПОЛНИТЕЛЬ";
        if (record.getEventRole() != null && record.getEventRole().getGlobalEventRole() != null) {
            roleName = record.getEventRole().getGlobalEventRole().getTitle();
        }

        String fullName = "";
        if (record.getSectorParticipant() != null && record.getSectorParticipant().getStudent() != null) {
            User student = record.getSectorParticipant().getStudent();
            fullName = student.getName() + " " + student.getSurname() +
                    (student.getPatronymic() != null ? " " + student.getPatronymic() : "");
        }

        return EventParticipantInfoDTO.builder()
                .id(record.getId())
                .role(roleName)
                .fullName(fullName)
                .totalPoints(record.getTotalPoints())
                .wasPresent(record.getWasPresent())
                .entityType("PARTICIPATION_RECORD")
                .build();
    }
}
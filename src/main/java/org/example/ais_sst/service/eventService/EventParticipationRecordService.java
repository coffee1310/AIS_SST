package org.example.ais_sst.service.eventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.config.PointsConfig;
import org.example.ais_sst.dto.event_participation.CreateParticipationRecordResponse;
import org.example.ais_sst.entity.*;
import org.example.ais_sst.exception.EventDoesNotExistException;
import org.example.ais_sst.exception.ValidationException;
import org.example.ais_sst.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventParticipationRecordService {

    private final EventParticipationRecordRepository participationRecordRepository;
    private final SectorParticipantRepository sectorParticipantRepository;
    private final EventRoleRepository eventRoleRepository;
    private final PointsConfig pointsConfig;
    private final EventRepository eventRepository;

    /**
     * Создать или восстановить запись об участии (исполнителя)
     */
    @Transactional
    public CreateParticipationRecordResponse createOrRestoreParticipationRecord(
            Long eventId, Long sectorParticipantId, Long eventRoleId, String comment) {

        log.info("Creating or restoring participation record: event={}, sectorParticipant={}, eventRole={}",
                eventId, sectorParticipantId, eventRoleId);

        // Проверяем существование мероприятия
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException("Мероприятие не найдено"));

        // Проверяем существование участника сектора
        SectorParticipant sectorParticipant = sectorParticipantRepository.findById(sectorParticipantId)
                .orElseThrow(() -> new ValidationException("Участник сектора не найден"));

        // Проверяем существование роли
        EventRole eventRole = eventRoleRepository.findById(eventRoleId)
                .orElseThrow(() -> new ValidationException("Роль не найдена: " + eventRoleId));

        // Проверяем, что роль принадлежит указанному мероприятию
        if (!eventRole.getEvent().getId().equals(eventId)) {
            throw new ValidationException(
                    String.format("Роль с id %d не принадлежит мероприятию с id %d", eventRoleId, eventId)
            );
        }

        // Проверяем лимит мест
        validateRoleCapacity(eventRoleId, eventRole.getCapacity());

        // Ищем существующую запись
        Optional<EventParticipationRecord> existingRecord = participationRecordRepository
                .findBySectorParticipantIdAndEventRoleId(sectorParticipantId, eventRoleId);

        boolean wasRestored = false;
        EventParticipationRecord record;

        if (existingRecord.isPresent()) {
            record = existingRecord.get();

            if (record.getIsDeleted() != null && record.getIsDeleted()) {
                // Восстанавливаем удаленную запись
                record.setIsDeleted(false);
                wasRestored = true;
                log.info("Restoring deleted participation record with id: {}", record.getId());
            } else {
                // Запись уже существует и активна
                throw new ValidationException(
                        String.format("Запись об участии уже существует: секторный участник %d, роль %d",
                                sectorParticipantId, eventRoleId)
                );
            }
        } else {
            // Создаем новую запись
            Integer pointsFromGlobalRole = getDefaultPointsFromGlobalRole(eventRole);

            record = EventParticipationRecord.builder()
                    .sectorParticipant(sectorParticipant)
                    .eventRole(eventRole)
                    .wasPresent(false)
                    .totalPoints(pointsFromGlobalRole)
                    .comment(comment != null ? comment : "Создана при регистрации")
                    .isDeleted(false)
                    .build();

            log.info("Creating new participation record");
        }

        // Обновляем комментарий, если передан
        if (comment != null && !comment.isEmpty()) {
            record.setComment(comment);
        }

        record = participationRecordRepository.save(record);
        log.info("Participation record saved with id: {}, restored: {}", record.getId(), wasRestored);

        return buildResponse(record, wasRestored);
    }

    /**
     * Создать или восстановить несколько записей об участии
     */
    @Transactional
    public List<CreateParticipationRecordResponse> createOrRestoreParticipationRecords(
            Long eventId, List<Long> sectorParticipantIds, Long eventRoleId, String comment) {

        log.info("Creating or restoring {} participation records for event {} and role {}",
                sectorParticipantIds.size(), eventId, eventRoleId);

        List<CreateParticipationRecordResponse> responses = new ArrayList<>();

        for (Long sectorParticipantId : sectorParticipantIds) {
            try {
                CreateParticipationRecordResponse response = createOrRestoreParticipationRecord(
                        eventId, sectorParticipantId, eventRoleId, comment
                );
                responses.add(response);
            } catch (Exception e) {
                log.error("Failed to create/restore record for sectorParticipant {}: {}",
                        sectorParticipantId, e.getMessage());
                // Продолжаем с остальными
            }
        }

        log.info("Successfully created/restored {} participation records", responses.size());
        return responses;
    }

    /**
     * Мягкое удаление записи об участии
     */
    @Transactional
    public void softDeleteParticipationRecord(Long recordId) {
        EventParticipationRecord record = participationRecordRepository.findById(recordId)
                .orElseThrow(() -> new ValidationException("Запись об участии не найдена: " + recordId));

        record.setIsDeleted(true);
        participationRecordRepository.save(record);
        log.info("Participation record {} soft deleted", recordId);
    }

    /**
     * Восстановление записи об участии
     */
    @Transactional
    public CreateParticipationRecordResponse restoreParticipationRecord(Long recordId) {
        EventParticipationRecord record = participationRecordRepository.findById(recordId)
                .orElseThrow(() -> new ValidationException("Запись об участии не найдена: " + recordId));

        if (record.getIsDeleted() == null || !record.getIsDeleted()) {
            throw new ValidationException("Запись об участии уже активна");
        }

        record.setIsDeleted(false);
        record = participationRecordRepository.save(record);
        log.info("Participation record {} restored", recordId);

        return buildResponse(record, true);
    }

    private void validateRoleCapacity(Long eventRoleId, Integer capacity) {
        long currentCount = participationRecordRepository.countByEventRoleId(eventRoleId);
        int maxCapacity = capacity != null ? capacity : Integer.MAX_VALUE;
        if (currentCount >= maxCapacity) {
            throw new ValidationException("Достигнут лимит мест для этой роли (" + maxCapacity + ")");
        }
    }

    private Integer getDefaultPointsFromGlobalRole(EventRole eventRole) {
        if (eventRole == null || eventRole.getGlobalEventRole() == null) {
            return pointsConfig.getDefaultParticipantPoints();
        }
        Integer points = eventRole.getGlobalEventRole().getDefaultPoints();
        return points != null ? points : 1;
    }

    private CreateParticipationRecordResponse buildResponse(EventParticipationRecord record, boolean wasRestored) {
        String sectorParticipantName = "";
        if (record.getSectorParticipant() != null && record.getSectorParticipant().getStudent() != null) {
            User student = record.getSectorParticipant().getStudent();
            sectorParticipantName = student.getName() + " " + student.getSurname() +
                    (student.getPatronymic() != null ? " " + student.getPatronymic() : "");
        }

        String eventRoleName = "";
        if (record.getEventRole() != null && record.getEventRole().getGlobalEventRole() != null) {
            eventRoleName = record.getEventRole().getGlobalEventRole().getTitle();
        }

        return CreateParticipationRecordResponse.builder()
                .id(record.getId())
                .sectorParticipantId(record.getSectorParticipant() != null ?
                        record.getSectorParticipant().getId() : null)
                .sectorParticipantName(sectorParticipantName)
                .eventRoleId(record.getEventRole() != null ?
                        record.getEventRole().getId() : null)
                .eventRoleName(eventRoleName)
                .comment(record.getComment())
                .totalPoints(record.getTotalPoints())
                .wasPresent(record.getWasPresent())
                .wasRestored(wasRestored)
                .build();
    }
}
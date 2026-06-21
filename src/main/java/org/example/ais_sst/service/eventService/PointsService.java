package org.example.ais_sst.service.eventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.config.PointsConfig;
import org.example.ais_sst.dto.event_participation.BulkUpdatePointsRequestDTO;
import org.example.ais_sst.dto.event_participation.UpdatePointsRequestDTO;
import org.example.ais_sst.dto.event_participation.UpdatePointsResponseDTO;
import org.example.ais_sst.entity.*;
import org.example.ais_sst.exception.ValidationException;
import org.example.ais_sst.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointsService {

    private final PointsConfig pointsConfig;
    private final EventParticipantsRepository eventParticipantsRepository;
    private final EventOrganizerRepository eventOrganizerRepository;
    private final EventParticipationRecordRepository participationRecordRepository;
    private final EventRepository eventRepository;
    private final EventRoleRepository eventRoleRepository;

    /**
     * Получить баллы из глобальной роли
     */
    public Integer getDefaultPointsFromGlobalRole(EventRole eventRole) {
        if (eventRole == null || eventRole.getGlobalEventRole() == null) {
            return pointsConfig.getDefaultParticipantPoints();
        }
        Integer points = eventRole.getGlobalEventRole().getDefaultPoints();
        return points != null ? points : 1;
    }

    /**
     * Получить баллы для участника в зависимости от присутствия
     */
    public Integer getPointsForParticipant(Boolean present) {
        return present ? pointsConfig.getDefaultParticipantPoints() : 0;
    }

    /**
     * Получить баллы для организатора в зависимости от присутствия
     */
    public Integer getPointsForOrganizer(Boolean present) {
        return present ? pointsConfig.getDefaultOrganizerPoints() : 0;
    }

    /**
     * Получить баллы для роли в зависимости от присутствия
     */
    public Integer getPointsForRole(EventRole eventRole, Boolean present) {
        if (!present) {
            return 0;
        }
        return getDefaultPointsFromGlobalRole(eventRole);
    }

    /**
     * Обновить баллы участника
     */
    @Transactional
    public UpdatePointsResponseDTO updateParticipantPoints(Long participantId, Integer points, String reason) {
        EventParticipant participant = eventParticipantsRepository.findById(participantId)
                .orElseThrow(() -> new ValidationException("Участник не найден: " + participantId));

        Integer oldPoints = participant.getTotalPoints();
        participant.setTotalPoints(points);
        participant = eventParticipantsRepository.save(participant);

        return UpdatePointsResponseDTO.builder()
                .entityId(participantId)
                .entityType("PARTICIPANT")
                .entityName(participant.getUser().getName() + " " + participant.getUser().getSurname())
                .oldPoints(oldPoints)
                .newPoints(points)
                .reason(reason)
                .success(true)
                .message("Баллы участника успешно обновлены")
                .build();
    }

    /**
     * Обновить баллы организатора
     */
    @Transactional
    public UpdatePointsResponseDTO updateOrganizerPoints(Long organizerId, Integer points, String reason) {
        EventOrganizer organizer = eventOrganizerRepository.findById(organizerId)
                .orElseThrow(() -> new ValidationException("Организатор не найден: " + organizerId));

        Integer oldPoints = organizer.getTotalPoints();
        organizer.setTotalPoints(points);
        organizer = eventOrganizerRepository.save(organizer);

        return UpdatePointsResponseDTO.builder()
                .entityId(organizerId)
                .entityType("ORGANIZER")
                .entityName(organizer.getUser().getName() + " " + organizer.getUser().getSurname())
                .oldPoints(oldPoints)
                .newPoints(points)
                .reason(reason)
                .success(true)
                .message("Баллы организатора успешно обновлены")
                .build();
    }

    /**
     * Обновить баллы для записи об участии
     */
    @Transactional
    public UpdatePointsResponseDTO updateParticipationRecordPoints(Long recordId, Integer points, String reason) {
        EventParticipationRecord record = participationRecordRepository.findById(recordId)
                .orElseThrow(() -> new ValidationException("Запись об участии не найдена: " + recordId));

        Integer oldPoints = record.getTotalPoints();
        record.setTotalPoints(points);
        record = participationRecordRepository.save(record);

        String userName = getParticipantName(record);

        return UpdatePointsResponseDTO.builder()
                .entityId(recordId)
                .entityType("PARTICIPATION_RECORD")
                .entityName(userName)
                .oldPoints(oldPoints)
                .newPoints(points)
                .reason(reason)
                .success(true)
                .message("Баллы записи об участии успешно обновлены")
                .build();
    }

    /**
     * Обновить баллы через универсальный интерфейс
     */
    @Transactional
    public UpdatePointsResponseDTO updatePoints(UpdatePointsRequestDTO request) {
        log.info("Updating points for entity: {}, type: {}, new points: {}",
                request.getEntityId(), request.getEntityType(), request.getPoints());

        try {
            String type = request.getEntityType().name();
            switch (type) {
                case "PARTICIPANT":
                    return updateParticipantPoints(request.getEntityId(), request.getPoints(), request.getReason());
                case "ORGANIZER":
                    return updateOrganizerPoints(request.getEntityId(), request.getPoints(), request.getReason());
                case "PARTICIPATION_RECORD":
                    return updateParticipationRecordPoints(request.getEntityId(), request.getPoints(), request.getReason());
                default:
                    throw new IllegalArgumentException("Неизвестный тип сущности: " + type);
            }
        } catch (Exception e) {
            log.error("Failed to update points: {}", e.getMessage());
            return UpdatePointsResponseDTO.builder()
                    .entityId(request.getEntityId())
                    .entityType(request.getEntityType().name())
                    .success(false)
                    .message("Ошибка при обновлении баллов: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Массовое обновление баллов для участников
     */
    @Transactional
    public UpdatePointsResponseDTO.BulkUpdateResponse bulkUpdateParticipantPoints(
            BulkUpdatePointsRequestDTO request) {

        log.info("Bulk updating participant points for event: {}, points: {}",
                request.getEventId(), request.getPoints());

        validateEventExists(request.getEventId());

        List<UpdatePointsResponseDTO> details = new ArrayList<>();
        int updatedCount = 0;

        if (request.getParticipantIds() != null && !request.getParticipantIds().isEmpty()) {
            for (Long participantId : request.getParticipantIds()) {
                EventParticipant participant = eventParticipantsRepository.findById(participantId)
                        .orElseThrow(() -> new ValidationException("Участник не найден: " + participantId));

                validateEntityBelongsToEvent(participant.getEvent().getId(), request.getEventId(), "Участник", participantId);

                Integer oldPoints = participant.getTotalPoints();
                participant.setTotalPoints(request.getPoints());
                participant = eventParticipantsRepository.save(participant);
                updatedCount++;

                details.add(UpdatePointsResponseDTO.builder()
                        .entityId(participantId)
                        .entityType("PARTICIPANT")
                        .entityName(participant.getUser().getName() + " " + participant.getUser().getSurname())
                        .oldPoints(oldPoints)
                        .newPoints(request.getPoints())
                        .reason(request.getReason())
                        .success(true)
                        .build());
            }
        }

        Long totalPointsAfter = eventParticipantsRepository.sumTotalPointsByEventId(request.getEventId());

        return UpdatePointsResponseDTO.BulkUpdateResponse.builder()
                .eventId(request.getEventId())
                .updatedCount(updatedCount)
                .totalPointsAfterUpdate(totalPointsAfter != null ? totalPointsAfter.intValue() : 0)
                .details(details)
                .message("Баллы для " + updatedCount + " участников успешно обновлены")
                .build();
    }

    /**
     * Массовое обновление баллов для организаторов
     */
    @Transactional
    public UpdatePointsResponseDTO.BulkUpdateResponse bulkUpdateOrganizerPoints(
            BulkUpdatePointsRequestDTO request) {

        log.info("Bulk updating organizer points for event: {}, points: {}",
                request.getEventId(), request.getPoints());

        validateEventExists(request.getEventId());

        List<UpdatePointsResponseDTO> details = new ArrayList<>();
        int updatedCount = 0;

        if (request.getOrganizerIds() != null && !request.getOrganizerIds().isEmpty()) {
            for (Long organizerId : request.getOrganizerIds()) {
                EventOrganizer organizer = eventOrganizerRepository.findById(organizerId)
                        .orElseThrow(() -> new ValidationException("Организатор не найден: " + organizerId));

                validateEntityBelongsToEvent(organizer.getEvent().getId(), request.getEventId(), "Организатор", organizerId);

                Integer oldPoints = organizer.getTotalPoints();
                organizer.setTotalPoints(request.getPoints());
                organizer = eventOrganizerRepository.save(organizer);
                updatedCount++;

                details.add(UpdatePointsResponseDTO.builder()
                        .entityId(organizerId)
                        .entityType("ORGANIZER")
                        .entityName(organizer.getUser().getName() + " " + organizer.getUser().getSurname())
                        .oldPoints(oldPoints)
                        .newPoints(request.getPoints())
                        .reason(request.getReason())
                        .success(true)
                        .build());
            }
        }

        Long totalPointsAfter = eventOrganizerRepository.sumTotalPointsByEventId(request.getEventId());

        return UpdatePointsResponseDTO.BulkUpdateResponse.builder()
                .eventId(request.getEventId())
                .updatedCount(updatedCount)
                .totalPointsAfterUpdate(totalPointsAfter != null ? totalPointsAfter.intValue() : 0)
                .details(details)
                .message("Баллы для " + updatedCount + " организаторов успешно обновлены")
                .build();
    }

    /**
     * Массовое обновление баллов для записей об участии (ролей)
     */
    @Transactional
    public UpdatePointsResponseDTO.BulkUpdateResponse bulkUpdateEventRolePoints(
            BulkUpdatePointsRequestDTO request) {

        log.info("Bulk updating event role points for event: {}, points: {}",
                request.getEventId(), request.getPoints());

        validateEventExists(request.getEventId());

        List<UpdatePointsResponseDTO> details = new ArrayList<>();
        int updatedCount = 0;

        if (request.getParticipationRecordIds() != null && !request.getParticipationRecordIds().isEmpty()) {
            for (Long eventRoleId : request.getParticipationRecordIds()) {
                // Получаем все записи об участии для этой роли
                List<EventParticipationRecord> records = participationRecordRepository
                        .findByEventRoleId(eventRoleId);

                for (EventParticipationRecord record : records) {
                    // Проверяем, что запись принадлежит указанному мероприятию
                    if (!record.getEventRole().getEvent().getId().equals(request.getEventId())) {
                        throw new ValidationException(
                                String.format("Запись об участии %d не принадлежит мероприятию %d",
                                        record.getId(), request.getEventId()));
                    }

                    Integer oldPoints = record.getTotalPoints();
                    record.setTotalPoints(request.getPoints());
                    record = participationRecordRepository.save(record);
                    updatedCount++;

                    String userName = getParticipantName(record);
                    String roleName = getRoleName(record);

                    details.add(UpdatePointsResponseDTO.builder()
                            .entityId(record.getId())
                            .entityType("PARTICIPATION_RECORD")
                            .entityName(userName + " (" + roleName + ")")
                            .oldPoints(oldPoints)
                            .newPoints(request.getPoints())
                            .reason(request.getReason())
                            .success(true)
                            .build());
                }
            }
        }

        return UpdatePointsResponseDTO.BulkUpdateResponse.builder()
                .eventId(request.getEventId())
                .updatedCount(updatedCount)
                .totalPointsAfterUpdate(0) // Суммировать баллы сложно, т.к. они в разных таблицах
                .details(details)
                .message("Баллы для " + updatedCount + " ролей успешно обновлены")
                .build();
    }

    /**
     * Сбросить баллы для всех участников
     */
    @Transactional
    public UpdatePointsResponseDTO.BulkUpdateResponse resetAllParticipantPoints(Long eventId) {
        log.info("Resetting all participant points for event: {}", eventId);

        List<EventParticipant> participants = eventParticipantsRepository.findByEventId(eventId);
        List<Long> participantIds = participants.stream()
                .map(EventParticipant::getId)
                .collect(java.util.stream.Collectors.toList());

        BulkUpdatePointsRequestDTO request = BulkUpdatePointsRequestDTO.builder()
                .eventId(eventId)
                .participantIds(participantIds)
                .points(pointsConfig.getDefaultParticipantPoints())
                .reason("Сброс к значению по умолчанию")
                .build();

        return bulkUpdateParticipantPoints(request);
    }

    /**
     * Сбросить баллы для всех организаторов
     */
    @Transactional
    public UpdatePointsResponseDTO.BulkUpdateResponse resetAllOrganizerPoints(Long eventId) {
        log.info("Resetting all organizer points for event: {}", eventId);

        List<EventOrganizer> organizers = eventOrganizerRepository.findByEventId(eventId);
        List<Long> organizerIds = organizers.stream()
                .map(EventOrganizer::getId)
                .collect(java.util.stream.Collectors.toList());

        BulkUpdatePointsRequestDTO request = BulkUpdatePointsRequestDTO.builder()
                .eventId(eventId)
                .organizerIds(organizerIds)
                .points(pointsConfig.getDefaultOrganizerPoints())
                .reason("Сброс к значению по умолчанию")
                .build();

        return bulkUpdateOrganizerPoints(request);
    }

    /**
     * Сбросить баллы для всех записей об участии
     */
    @Transactional
    public UpdatePointsResponseDTO.BulkUpdateResponse resetAllParticipationRecordsPoints(Long eventId) {
        log.info("Resetting all participation records points for event: {}", eventId);

        List<EventParticipationRecord> records = participationRecordRepository.findByEventId(eventId);
        List<Long> eventRoleIds = records.stream()
                .map(record -> record.getEventRole().getId())
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        BulkUpdatePointsRequestDTO request = BulkUpdatePointsRequestDTO.builder()
                .eventId(eventId)
                .participationRecordIds(eventRoleIds)
                .points(1) // Значение по умолчанию для ролей
                .reason("Сброс к значению по умолчанию")
                .build();

        return bulkUpdateEventRolePoints(request);
    }

    private void validateEventExists(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ValidationException("Мероприятие не найдено: " + eventId);
        }
    }

    private void validateEntityBelongsToEvent(Long entityEventId, Long requestEventId,
                                              String entityType, Long entityId) {
        if (!entityEventId.equals(requestEventId)) {
            throw new ValidationException(
                    String.format("%s с id %d не принадлежит мероприятию с id %d",
                            entityType, entityId, requestEventId)
            );
        }
    }

    private String getParticipantName(EventParticipationRecord record) {
        if (record.getSectorParticipant() != null && record.getSectorParticipant().getStudent() != null) {
            return record.getSectorParticipant().getStudent().getName() + " " +
                    record.getSectorParticipant().getStudent().getSurname();
        }
        return "";
    }

    private String getRoleName(EventParticipationRecord record) {
        if (record.getEventRole() != null && record.getEventRole().getGlobalEventRole() != null) {
            return record.getEventRole().getGlobalEventRole().getTitle();
        }
        return "";
    }
}
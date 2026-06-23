package org.example.ais_sst.service.eventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.config.PointsConfig;
import org.example.ais_sst.dto.event_participation.*;
import org.example.ais_sst.entity.*;
import org.example.ais_sst.exception.EventDoesNotExistException;
import org.example.ais_sst.exception.ValidationException;
import org.example.ais_sst.repository.*;
import org.example.ais_sst.service.base.BaseEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationMarkService extends BaseEntityService {

    private final EventRepository eventRepository;
    private final EventParticipantsRepository eventParticipantsRepository;
    private final EventOrganizerRepository eventOrganizerRepository;
    private final EventRoleRepository eventRoleRepository;
    private final PointsConfig pointsConfig;
    private final EventParticipationRecordRepository participationRecordRepository;
    private final SectorParticipantRepository sectorParticipantRepository;

    /**
     * Получить баллы из глобальной роли
     */
    private Integer getDefaultPointsFromGlobalRole(EventRole eventRole) {
        if (eventRole == null || eventRole.getGlobalEventRole() == null) {
            return pointsConfig.getDefaultParticipantPoints();
        }
        Integer points = eventRole.getGlobalEventRole().getDefaultPoints();
        return points != null ? points : 1;
    }

    /**
     * ОСНОВНОЙ МЕТОД - обновляет event_participants, event_organizers и event_participation_records
     */
    @Transactional
    public ParticipationMarkResponseDTO markParticipation(ParticipationMarkRequestDTO request) {
        log.info("Marking participation for event: {}, present: {}", request.getEventId(), request.getPresent());

        Event event = findEntityOrThrow(request.getEventId(), eventRepository::findById,
                () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

        List<ParticipationMarkResponseDTO.MarkedEntityDTO> details = new ArrayList<>();
        List<Long> createdRecordIds = new ArrayList<>();
        int totalPointsAwarded = 0;

        // 1. Обрабатываем participantIds (обновляем event_participants)
        if (request.getParticipantIds() != null && !request.getParticipantIds().isEmpty()) {
            for (Long participantId : request.getParticipantIds()) {
                EventParticipant participant = eventParticipantsRepository.findById(participantId)
                        .orElseThrow(() -> new ValidationException("Участник не найден: " + participantId));

                if (!participant.getEvent().getId().equals(request.getEventId())) {
                    throw new ValidationException(
                            String.format("Участник с id %d не принадлежит мероприятию с id %d",
                                    participantId, request.getEventId())
                    );
                }

                participant.setWasPresent(request.getPresent());
                if (request.getPresent()) {
                    participant.setTotalPoints(pointsConfig.getDefaultParticipantPoints());
                } else {
                    participant.setTotalPoints(0);
                }
                eventParticipantsRepository.save(participant);

                details.add(ParticipationMarkResponseDTO.MarkedEntityDTO.builder()
                        .id(participantId)
                        .type("PARTICIPANT")
                        .name(participant.getUser().getName() + " " + participant.getUser().getSurname())
                        .pointsAwarded(request.getPresent() ? participant.getTotalPoints() : 0)
                        .wasPresent(request.getPresent())
                        .build());

                totalPointsAwarded += request.getPresent() ? participant.getTotalPoints() : 0;
            }
        }

        // 2. Обрабатываем organizerIds (обновляем event_organizers)
        if (request.getOrganizerIds() != null && !request.getOrganizerIds().isEmpty()) {
            for (Long organizerId : request.getOrganizerIds()) {
                EventOrganizer organizer = eventOrganizerRepository.findById(organizerId)
                        .orElseThrow(() -> new ValidationException("Организатор не найден: " + organizerId));

                if (!organizer.getEvent().getId().equals(request.getEventId())) {
                    throw new ValidationException(
                            String.format("Организатор с id %d не принадлежит мероприятию с id %d",
                                    organizerId, request.getEventId())
                    );
                }

                organizer.setWasPresent(request.getPresent());
                if (request.getPresent()) {
                    organizer.setTotalPoints(pointsConfig.getDefaultOrganizerPoints());
                } else {
                    organizer.setTotalPoints(0);
                }
                eventOrganizerRepository.save(organizer);

                details.add(ParticipationMarkResponseDTO.MarkedEntityDTO.builder()
                        .id(organizerId)
                        .type("ORGANIZER")
                        .name(organizer.getUser().getName() + " " + organizer.getUser().getSurname())
                        .pointsAwarded(request.getPresent() ? organizer.getTotalPoints() : 0)
                        .wasPresent(request.getPresent())
                        .build());

                totalPointsAwarded += request.getPresent() ? organizer.getTotalPoints() : 0;
            }
        }

        // ⭐ 3. Обрабатываем participationRecordIds (массив ID event_participation_records)
        if (request.getParticipationRecordIds() != null && !request.getParticipationRecordIds().isEmpty()) {
            for (Long recordId : request.getParticipationRecordIds()) {
                EventParticipationRecord record = participationRecordRepository.findById(recordId)
                        .orElseThrow(() -> new ValidationException("Запись об участии не найдена: " + recordId));

                // Проверяем, что запись принадлежит мероприятию
                if (!record.getEventRole().getEvent().getId().equals(request.getEventId())) {
                    throw new ValidationException(
                            String.format("Запись об участии с id %d не принадлежит мероприятию с id %d",
                                    recordId, request.getEventId())
                    );
                }

                // Обновляем статус присутствия
                record.setWasPresent(request.getPresent());

                // Если комментарий передан, обновляем его
                if (request.getComment() != null) {
                    record.setComment(request.getComment());
                }

                // Если отмечаем как присутствовавшего, обновляем баллы из глобальной роли
                if (request.getPresent()) {
                    Integer points = getDefaultPointsFromGlobalRole(record.getEventRole());
                    record.setTotalPoints(points);
                } else {
                    record.setTotalPoints(0);
                }

                record = participationRecordRepository.save(record);
                createdRecordIds.add(record.getId());

                String userName = "";
                if (record.getSectorParticipant() != null && record.getSectorParticipant().getStudent() != null) {
                    userName = record.getSectorParticipant().getStudent().getName() + " " +
                            record.getSectorParticipant().getStudent().getSurname();
                }

                String roleName = "";
                if (record.getEventRole() != null && record.getEventRole().getGlobalEventRole() != null) {
                    roleName = record.getEventRole().getGlobalEventRole().getTitle();
                }

                details.add(ParticipationMarkResponseDTO.MarkedEntityDTO.builder()
                        .id(record.getId())
                        .type("PARTICIPATION_RECORD")
                        .name(userName + " (" + roleName + ")")
                        .pointsAwarded(request.getPresent() ? record.getTotalPoints() : 0)
                        .wasPresent(request.getPresent())
                        .build());

                totalPointsAwarded += request.getPresent() ? record.getTotalPoints() : 0;
            }
        }

        String message = request.getPresent()
                ? "Участники успешно отмечены как присутствовавшие"
                : "Отметка о присутствии снята";

        return ParticipationMarkResponseDTO.builder()
                .eventId(request.getEventId())
                .markedParticipants(
                        (request.getParticipantIds() != null ? request.getParticipantIds().size() : 0) +
                                (request.getOrganizerIds() != null ? request.getOrganizerIds().size() : 0) +
                                (request.getParticipationRecordIds() != null ? request.getParticipationRecordIds().size() : 0)
                )
                .markedOrganizers(request.getOrganizerIds() != null ? request.getOrganizerIds().size() : 0)
                .participationRecordIds(createdRecordIds)
                .totalPointsAwarded(totalPointsAwarded)
                .message(message)
                .details(details)
                .build();
    }

    /**
     * Отметить всех участников мероприятия
     */
    @Transactional
    public ParticipationMarkResponseDTO markAllParticipants(Long eventId, Boolean present) {
        log.info("Marking all participants for event: {}, present: {}", eventId, present);

        Event event = findEntityOrThrow(eventId, eventRepository::findById,
                () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

        List<EventParticipant> participants = eventParticipantsRepository.findByEventId(eventId).stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .collect(Collectors.toList());

        if (participants.isEmpty()) {
            throw new ValidationException("Нет участников для этого мероприятия");
        }

        List<Long> participantIds = participants.stream()
                .map(EventParticipant::getId)
                .collect(Collectors.toList());

        ParticipationMarkRequestDTO request = ParticipationMarkRequestDTO.builder()
                .eventId(eventId)
                .participantIds(participantIds)
                .present(present)
                .comment("Автоматическая отметка всех участников")
                .build();

        return markParticipation(request);
    }

    /**
     * Отметить всех организаторов мероприятия
     */
    @Transactional
    public ParticipationMarkResponseDTO markAllOrganizers(Long eventId, Boolean present) {
        log.info("Marking all organizers for event: {}, present: {}", eventId, present);

        Event event = findEntityOrThrow(eventId, eventRepository::findById,
                () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

        List<EventOrganizer> organizers = eventOrganizerRepository.findByEventId(eventId).stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .collect(Collectors.toList());

        if (organizers.isEmpty()) {
            throw new ValidationException("Нет организаторов для этого мероприятия");
        }

        List<Long> organizerIds = organizers.stream()
                .map(EventOrganizer::getId)
                .collect(Collectors.toList());

        ParticipationMarkRequestDTO request = ParticipationMarkRequestDTO.builder()
                .eventId(eventId)
                .organizerIds(organizerIds)
                .present(present)
                .comment("Автоматическая отметка всех организаторов")
                .build();

        return markParticipation(request);
    }

    /**
     * Получить статистику по отметкам для мероприятия
     */
    @Transactional(readOnly = true)
    public ParticipationStatsDTO getParticipationStats(Long eventId) {
        Event event = findEntityOrThrow(eventId, eventRepository::findById,
                () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

        long totalParticipants = eventParticipantsRepository.countByEventIdAndIsDeletedFalse(eventId);
        long presentParticipants = eventParticipantsRepository.findByEventIdAndWasPresentTrueAndIsDeletedFalse(eventId).size();

        long totalOrganizers = eventOrganizerRepository.countByEventIdAndIsDeletedFalse(eventId);
        long presentOrganizers = eventOrganizerRepository.findByEventIdAndWasPresentTrueAndIsDeletedFalse(eventId).size();

        List<EventParticipationRecord> records = participationRecordRepository.findByEventId(eventId).stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .collect(Collectors.toList());

        long totalRecords = records.size();
        long presentRecords = records.stream()
                .filter(EventParticipationRecord::getWasPresent)
                .count();

        Long participantPoints = eventParticipantsRepository.sumTotalPointsByEventId(eventId);
        Long organizerPoints = eventOrganizerRepository.sumTotalPointsByEventId(eventId);
        int totalPoints = (participantPoints != null ? participantPoints.intValue() : 0) +
                (organizerPoints != null ? organizerPoints.intValue() : 0);

        long totalRoles = eventRoleRepository.countByEventId(eventId);

        return ParticipationStatsDTO.builder()
                .eventId(eventId)
                .eventTitle(event.getTitle())
                .totalParticipants((int) totalParticipants)
                .presentParticipants((int) presentParticipants)
                .absentParticipants((int) (totalParticipants - presentParticipants))
                .totalOrganizers((int) totalOrganizers)
                .presentOrganizers((int) presentOrganizers)
                .absentOrganizers((int) (totalOrganizers - presentOrganizers))
                .totalRoles((int) totalRoles)
                .presentRoles((int) presentRecords)
                .absentRoles((int) (totalRoles - presentRecords))
                .totalParticipantPoints(participantPoints != null ? participantPoints.intValue() : 0)
                .totalOrganizerPoints(organizerPoints != null ? organizerPoints.intValue() : 0)
                .totalRolePoints(0)
                .totalPoints(totalPoints)
                .build();
    }

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

        String userName = "";
        if (record.getSectorParticipant() != null && record.getSectorParticipant().getStudent() != null) {
            userName = record.getSectorParticipant().getStudent().getName() + " " +
                    record.getSectorParticipant().getStudent().getSurname();
        }

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
     * Сбросить баллы всех участников мероприятия к значению по умолчанию
     */
    @Transactional
    public UpdatePointsResponseDTO.BulkUpdateResponse resetAllParticipantPoints(Long eventId) {
        log.info("Resetting all participant points for event: {}", eventId);

        Event event = findEntityOrThrow(eventId, eventRepository::findById,
                () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

        List<EventParticipant> participants = eventParticipantsRepository.findByEventId(eventId).stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .collect(Collectors.toList());

        if (participants.isEmpty()) {
            throw new ValidationException("Нет участников для этого мероприятия");
        }

        List<Long> participantIds = participants.stream()
                .map(EventParticipant::getId)
                .collect(Collectors.toList());

        BulkUpdatePointsRequestDTO request = BulkUpdatePointsRequestDTO.builder()
                .eventId(eventId)
                .participantIds(participantIds)
                .points(pointsConfig.getDefaultParticipantPoints())
                .reason("Сброс к значению по умолчанию")
                .build();

        return bulkUpdateParticipantPoints(request);
    }

    /**
     * Сбросить баллы всех организаторов мероприятия к значению по умолчанию
     */
    @Transactional
    public UpdatePointsResponseDTO.BulkUpdateResponse resetAllOrganizerPoints(Long eventId) {
        log.info("Resetting all organizer points for event: {}", eventId);

        Event event = findEntityOrThrow(eventId, eventRepository::findById,
                () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

        List<EventOrganizer> organizers = eventOrganizerRepository.findByEventId(eventId).stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .collect(Collectors.toList());

        if (organizers.isEmpty()) {
            throw new ValidationException("Нет организаторов для этого мероприятия");
        }

        List<Long> organizerIds = organizers.stream()
                .map(EventOrganizer::getId)
                .collect(Collectors.toList());

        BulkUpdatePointsRequestDTO request = BulkUpdatePointsRequestDTO.builder()
                .eventId(eventId)
                .organizerIds(organizerIds)
                .points(pointsConfig.getDefaultOrganizerPoints())
                .reason("Сброс к значению по умолчанию")
                .build();

        return bulkUpdateOrganizerPoints(request);
    }

    /**
     * Массовое обновление баллов для участников
     */
    @Transactional
    public UpdatePointsResponseDTO.BulkUpdateResponse bulkUpdateParticipantPoints(
            BulkUpdatePointsRequestDTO request) {
        log.info("Bulk updating participant points for event: {}, points: {}",
                request.getEventId(), request.getPoints());

        Event event = findEntityOrThrow(request.getEventId(), eventRepository::findById,
                () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

        List<UpdatePointsResponseDTO> details = new ArrayList<>();
        int updatedCount = 0;

        if (request.getParticipantIds() != null && !request.getParticipantIds().isEmpty()) {
            for (Long participantId : request.getParticipantIds()) {
                EventParticipant participant = eventParticipantsRepository.findById(participantId)
                        .orElseThrow(() -> new ValidationException("Участник не найден: " + participantId));

                if (!participant.getEvent().getId().equals(request.getEventId())) {
                    throw new ValidationException(
                            String.format("Участник %d не принадлежит мероприятию %d",
                                    participantId, request.getEventId()));
                }

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

        Event event = findEntityOrThrow(request.getEventId(), eventRepository::findById,
                () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

        List<UpdatePointsResponseDTO> details = new ArrayList<>();
        int updatedCount = 0;

        if (request.getOrganizerIds() != null && !request.getOrganizerIds().isEmpty()) {
            for (Long organizerId : request.getOrganizerIds()) {
                EventOrganizer organizer = eventOrganizerRepository.findById(organizerId)
                        .orElseThrow(() -> new ValidationException("Организатор не найден: " + organizerId));

                if (!organizer.getEvent().getId().equals(request.getEventId())) {
                    throw new ValidationException(
                            String.format("Организатор %d не принадлежит мероприятию %d",
                                    organizerId, request.getEventId()));
                }

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
}
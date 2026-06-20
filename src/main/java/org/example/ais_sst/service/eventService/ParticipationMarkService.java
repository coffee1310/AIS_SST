package org.example.ais_sst.service.eventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.config.PointsConfig;
import org.example.ais_sst.dto.event_participation.*;
import org.example.ais_sst.entity.Event;
import org.example.ais_sst.entity.EventOrganizer;
import org.example.ais_sst.entity.EventParticipant;
import org.example.ais_sst.entity.EventRole;
import org.example.ais_sst.exception.EventDoesNotExistException;
import org.example.ais_sst.exception.UserDoesNotExistException;
import org.example.ais_sst.exception.ValidationException;
import org.example.ais_sst.repository.EventOrganizerRepository;
import org.example.ais_sst.repository.EventParticipantsRepository;
import org.example.ais_sst.repository.EventRepository;
import org.example.ais_sst.repository.EventRoleRepository;
import org.example.ais_sst.service.base.BaseEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationMarkService extends BaseEntityService {

    private final EventRepository eventRepository;
    private final EventParticipantsRepository eventParticipantsRepository;
    private final EventOrganizerRepository eventOrganizerRepository;
    private final EventRoleRepository eventRoleRepository;
    private final PointsConfig pointsConfig;

    /**
     * Отметить участников, организаторов и роли как присутствовавших или отсутствовавших
     */
    @Transactional
    public ParticipationMarkResponseDTO markParticipation(ParticipationMarkRequestDTO request) {
        log.info("Marking participation for event: {}, present: {}", request.getEventId(), request.getPresent());

        Event event = findEntityOrThrow(request.getEventId(), eventRepository::findById,
                () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

        List<ParticipationMarkResponseDTO.MarkedEntityDTO> details = new ArrayList<>();
        int totalPointsAwarded = 0;
        int markedParticipants = 0;
        int markedOrganizers = 0;
        int markedEventRoles = 0;

        // Отметка участников
        if (request.getParticipantIds() != null && !request.getParticipantIds().isEmpty()) {
            markedParticipants = markParticipants(request.getParticipantIds(), request.getPresent(), details);
        }

        // Отметка организаторов
        if (request.getOrganizerIds() != null && !request.getOrganizerIds().isEmpty()) {
            markedOrganizers = markOrganizers(request.getOrganizerIds(), request.getPresent(), details);
        }

        // Отметка ролей
        if (request.getEventRoleIds() != null && !request.getEventRoleIds().isEmpty()) {
            markedEventRoles = markEventRoles(request.getEventRoleIds(), request.getPresent(), details);
        }

        // Подсчет общих баллов
        totalPointsAwarded = details.stream()
                .filter(ParticipationMarkResponseDTO.MarkedEntityDTO::getWasPresent)
                .mapToInt(ParticipationMarkResponseDTO.MarkedEntityDTO::getPointsAwarded)
                .sum();

        String message = request.getPresent()
                ? "Участники успешно отмечены как присутствовавшие"
                : "Отметка о присутствии снята";

        return ParticipationMarkResponseDTO.builder()
                .eventId(request.getEventId())
                .markedParticipants(markedParticipants)
                .markedOrganizers(markedOrganizers)
                .markedEventRoles(markedEventRoles)
                .totalPointsAwarded(totalPointsAwarded)
                .message(message)
                .details(details)
                .build();
    }

    /**
     * Отметить участников
     */
    private int markParticipants(List<Long> participantIds, Boolean present,
                                 List<ParticipationMarkResponseDTO.MarkedEntityDTO> details) {
        int count = 0;
        for (Long participantId : participantIds) {
            EventParticipant participant = eventParticipantsRepository.findById(participantId)
                    .orElseThrow(() -> new UserDoesNotExistException("Участник не найден: " + participantId));

            participant.setWasPresent(present);

            // Если присутствует, начисляем баллы
            if (present) {
                participant.setTotalPoints(pointsConfig.getDefaultParticipantPoints());
            } else {
                participant.setTotalPoints(0);
            }

            eventParticipantsRepository.save(participant);
            count++;

            details.add(ParticipationMarkResponseDTO.MarkedEntityDTO.builder()
                    .id(participantId)
                    .type("PARTICIPANT")
                    .name(participant.getUser().getName() + " " + participant.getUser().getSurname())
                    .pointsAwarded(present ? pointsConfig.getDefaultParticipantPoints() : 0)
                    .wasPresent(present)
                    .build());
        }
        return count;
    }

    /**
     * Отметить организаторов
     */
    private int markOrganizers(List<Long> organizerIds, Boolean present,
                               List<ParticipationMarkResponseDTO.MarkedEntityDTO> details) {
        int count = 0;
        for (Long organizerId : organizerIds) {
            EventOrganizer organizer = eventOrganizerRepository.findById(organizerId)
                    .orElseThrow(() -> new UserDoesNotExistException("Организатор не найден: " + organizerId));

            organizer.setWasPresent(present);

            if (present) {
                organizer.setTotalPoints(pointsConfig.getDefaultOrganizerPoints());
            } else {
                organizer.setTotalPoints(0);
            }

            eventOrganizerRepository.save(organizer);
            count++;

            details.add(ParticipationMarkResponseDTO.MarkedEntityDTO.builder()
                    .id(organizerId)
                    .type("ORGANIZER")
                    .name(organizer.getUser().getName() + " " + organizer.getUser().getSurname())
                    .pointsAwarded(present ? pointsConfig.getDefaultOrganizerPoints() : 0)
                    .wasPresent(present)
                    .build());
        }
        return count;
    }

    /**
     * Отметить роли
     */
    private int markEventRoles(List<Long> eventRoleIds, Boolean present,
                               List<ParticipationMarkResponseDTO.MarkedEntityDTO> details) {
        int count = 0;
        for (Long roleId : eventRoleIds) {
            EventRole role = eventRoleRepository.findById(roleId)
                    .orElseThrow(() -> new UserDoesNotExistException("Роль не найдена: " + roleId));

            role.setWasPresent(present);

            if (present) {
                // Баллы для роли берутся из глобальной роли
                Integer points = role.getGlobalEventRole().getDefaultPoints();
                role.setTotalPoints(points != null ? points : 1);
            } else {
                role.setTotalPoints(0);
            }

            eventRoleRepository.save(role);
            count++;

            details.add(ParticipationMarkResponseDTO.MarkedEntityDTO.builder()
                    .id(roleId)
                    .type("EVENT_ROLE")
                    .name(role.getGlobalEventRole().getTitle())
                    .pointsAwarded(present ? role.getTotalPoints() : 0)
                    .wasPresent(present)
                    .build());
        }
        return count;
    }

    /**
     * Отметить всех участников мероприятия как присутствовавших
     */
    @Transactional
    public ParticipationMarkResponseDTO markAllParticipants(Long eventId, Boolean present) {
        log.info("Marking all participants for event: {}, present: {}", eventId, present);

        Event event = findEntityOrThrow(eventId, eventRepository::findById,
                () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

        List<EventParticipant> participants = eventParticipantsRepository.findByEventId(eventId);
        List<Long> participantIds = participants.stream()
                .map(EventParticipant::getId)
                .toList();

        ParticipationMarkRequestDTO request = ParticipationMarkRequestDTO.builder()
                .eventId(eventId)
                .participantIds(participantIds)
                .present(present)
                .build();

        return markParticipation(request);
    }

    /**
     * Отметить всех организаторов мероприятия как присутствовавших
     */
    @Transactional
    public ParticipationMarkResponseDTO markAllOrganizers(Long eventId, Boolean present) {
        log.info("Marking all organizers for event: {}, present: {}", eventId, present);

        Event event = findEntityOrThrow(eventId, eventRepository::findById,
                () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

        List<EventOrganizer> organizers = eventOrganizerRepository.findByEventId(eventId);
        List<Long> organizerIds = organizers.stream()
                .map(EventOrganizer::getId)
                .toList();

        ParticipationMarkRequestDTO request = ParticipationMarkRequestDTO.builder()
                .eventId(eventId)
                .organizerIds(organizerIds)
                .present(present)
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

        long totalParticipants = eventParticipantsRepository.countByEventId(eventId);
        long presentParticipants = eventParticipantsRepository.findByEventIdAndWasPresentTrue(eventId).size();

        long totalOrganizers = eventOrganizerRepository.countByEventId(eventId);
        long presentOrganizers = eventOrganizerRepository.findByEventIdAndWasPresentTrue(eventId).size();

        long totalRoles = eventRoleRepository.findByEventId(eventId).size();
        long presentRoles = eventRoleRepository.findByEventIdAndWasPresentTrue(eventId).size();

        Long participantPoints = eventParticipantsRepository.sumTotalPointsByEventId(eventId);
        Long organizerPoints = eventOrganizerRepository.sumTotalPointsByEventId(eventId);
        Long rolePoints = eventRoleRepository.sumTotalPointsByEventId(eventId);

        // Исправляем: конвертируем Long в Integer и суммируем
        int totalParticipantPoints = participantPoints != null ? participantPoints.intValue() : 0;
        int totalOrganizerPoints = organizerPoints != null ? organizerPoints.intValue() : 0;
        int totalRolePoints = rolePoints != null ? rolePoints.intValue() : 0;

        int totalPoints = totalParticipantPoints + totalOrganizerPoints + totalRolePoints;

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
                .presentRoles((int) presentRoles)
                .absentRoles((int) (totalRoles - presentRoles))
                .totalParticipantPoints(totalParticipantPoints)
                .totalOrganizerPoints(totalOrganizerPoints)
                .totalRolePoints(totalRolePoints)
                .totalPoints(totalPoints)
                .build();
    }

    @Transactional
    public UpdatePointsResponseDTO updatePoints(UpdatePointsRequestDTO request) {
        log.info("Updating points for entity: {}, type: {}, new points: {}",
                request.getEntityId(), request.getEntityType(), request.getPoints());

        try {
            switch (request.getEntityType()) {
                case PARTICIPANT:
                    return updateParticipantPoints(request.getEntityId(), request.getPoints(), request.getReason());
                case ORGANIZER:
                    return updateOrganizerPoints(request.getEntityId(), request.getPoints(), request.getReason());
                case EVENT_ROLE:
                    return updateEventRolePoints(request.getEntityId(), request.getPoints(), request.getReason());
                default:
                    throw new IllegalArgumentException("Неизвестный тип сущности: " + request.getEntityType());
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
        eventParticipantsRepository.save(participant);

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
        eventOrganizerRepository.save(organizer);

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
     * Обновить баллы роли
     */
    @Transactional
    public UpdatePointsResponseDTO updateEventRolePoints(Long roleId, Integer points, String reason) {
        EventRole role = eventRoleRepository.findById(roleId)
                .orElseThrow(() -> new ValidationException("Роль не найдена: " + roleId));

        Integer oldPoints = role.getTotalPoints();
        role.setTotalPoints(points);
        eventRoleRepository.save(role);

        return UpdatePointsResponseDTO.builder()
                .entityId(roleId)
                .entityType("EVENT_ROLE")
                .entityName(role.getGlobalEventRole().getTitle())
                .oldPoints(oldPoints)
                .newPoints(points)
                .reason(reason)
                .success(true)
                .message("Баллы роли успешно обновлены")
                .build();
    }

    /**
     * Массовое обновление баллов для участников
     */
    @Transactional
    public UpdatePointsResponseDTO.BulkUpdateResponse bulkUpdateParticipantPoints(
            BulkUpdatePointsRequestDTO request) {
        log.info("Bulk updating participant points for event: {}, points: {}",
                request.getEventId(), request.getPoints());

        // Проверяем существование мероприятия
        Event event = findEntityOrThrow(request.getEventId(), eventRepository::findById,
                () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

        List<UpdatePointsResponseDTO> details = new ArrayList<>();
        int updatedCount = 0;

        if (request.getParticipantIds() != null && !request.getParticipantIds().isEmpty()) {
            for (Long participantId : request.getParticipantIds()) {
                EventParticipant participant = eventParticipantsRepository.findById(participantId)
                        .orElseThrow(() -> new ValidationException("Участник не найден: " + participantId));

                // Проверяем, что участник принадлежит указанному мероприятию
                if (!participant.getEvent().getId().equals(request.getEventId())) {
                    throw new ValidationException(
                            String.format("Участник %d не принадлежит мероприятию %d",
                                    participantId, request.getEventId()));
                }

                Integer oldPoints = participant.getTotalPoints();
                participant.setTotalPoints(request.getPoints());
                eventParticipantsRepository.save(participant);
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

                // Проверяем, что организатор принадлежит указанному мероприятию
                if (!organizer.getEvent().getId().equals(request.getEventId())) {
                    throw new ValidationException(
                            String.format("Организатор %d не принадлежит мероприятию %d",
                                    organizerId, request.getEventId()));
                }

                Integer oldPoints = organizer.getTotalPoints();
                organizer.setTotalPoints(request.getPoints());
                eventOrganizerRepository.save(organizer);
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
     * Массовое обновление баллов для ролей
     */
    @Transactional
    public UpdatePointsResponseDTO.BulkUpdateResponse bulkUpdateEventRolePoints(
            BulkUpdatePointsRequestDTO request) {
        log.info("Bulk updating role points for event: {}, points: {}",
                request.getEventId(), request.getPoints());

        Event event = findEntityOrThrow(request.getEventId(), eventRepository::findById,
                () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

        List<UpdatePointsResponseDTO> details = new ArrayList<>();
        int updatedCount = 0;

        if (request.getEventRoleIds() != null && !request.getEventRoleIds().isEmpty()) {
            for (Long roleId : request.getEventRoleIds()) {
                EventRole role = eventRoleRepository.findById(roleId)
                        .orElseThrow(() -> new ValidationException("Роль не найдена: " + roleId));

                // Проверяем, что роль принадлежит указанному мероприятию
                if (!role.getEvent().getId().equals(request.getEventId())) {
                    throw new ValidationException(
                            String.format("Роль %d не принадлежит мероприятию %d",
                                    roleId, request.getEventId()));
                }

                Integer oldPoints = role.getTotalPoints();
                role.setTotalPoints(request.getPoints());
                eventRoleRepository.save(role);
                updatedCount++;

                details.add(UpdatePointsResponseDTO.builder()
                        .entityId(roleId)
                        .entityType("EVENT_ROLE")
                        .entityName(role.getGlobalEventRole().getTitle())
                        .oldPoints(oldPoints)
                        .newPoints(request.getPoints())
                        .reason(request.getReason())
                        .success(true)
                        .build());
            }
        }

        Long totalPointsAfter = eventRoleRepository.sumTotalPointsByEventId(request.getEventId());

        return UpdatePointsResponseDTO.BulkUpdateResponse.builder()
                .eventId(request.getEventId())
                .updatedCount(updatedCount)
                .totalPointsAfterUpdate(totalPointsAfter != null ? totalPointsAfter.intValue() : 0)
                .details(details)
                .message("Баллы для " + updatedCount + " ролей успешно обновлены")
                .build();
    }

    /**
     * Сбросить баллы для всех участников мероприятия к значениям по умолчанию
     */
    @Transactional
    public UpdatePointsResponseDTO.BulkUpdateResponse resetAllParticipantPoints(Long eventId) {
        log.info("Resetting all participant points for event: {}", eventId);

        Event event = findEntityOrThrow(eventId, eventRepository::findById,
                () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

        List<EventParticipant> participants = eventParticipantsRepository.findByEventId(eventId);
        List<Long> participantIds = participants.stream()
                .map(EventParticipant::getId)
                .toList();

        BulkUpdatePointsRequestDTO request = BulkUpdatePointsRequestDTO.builder()
                .eventId(eventId)
                .participantIds(participantIds)
                .points(pointsConfig.getDefaultParticipantPoints())
                .reason("Сброс к значению по умолчанию")
                .build();

        return bulkUpdateParticipantPoints(request);
    }

    /**
     * Сбросить баллы для всех организаторов мероприятия к значениям по умолчанию
     */
    @Transactional
    public UpdatePointsResponseDTO.BulkUpdateResponse resetAllOrganizerPoints(Long eventId) {
        log.info("Resetting all organizer points for event: {}", eventId);

        Event event = findEntityOrThrow(eventId, eventRepository::findById,
                () -> new EventDoesNotExistException("Мероприятие не найдено"), "Event");

        List<EventOrganizer> organizers = eventOrganizerRepository.findByEventId(eventId);
        List<Long> organizerIds = organizers.stream()
                .map(EventOrganizer::getId)
                .toList();

        BulkUpdatePointsRequestDTO request = BulkUpdatePointsRequestDTO.builder()
                .eventId(eventId)
                .organizerIds(organizerIds)
                .points(pointsConfig.getDefaultOrganizerPoints())
                .reason("Сброс к значению по умолчанию")
                .build();

        return bulkUpdateOrganizerPoints(request);
    }
}
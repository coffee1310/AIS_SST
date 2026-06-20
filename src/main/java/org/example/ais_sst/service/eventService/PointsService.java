package org.example.ais_sst.service.eventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.config.PointsConfig;
import org.example.ais_sst.entity.EventOrganizer;
import org.example.ais_sst.entity.EventParticipant;
import org.example.ais_sst.entity.EventRole;
import org.example.ais_sst.repository.EventOrganizerRepository;
import org.example.ais_sst.repository.EventParticipantsRepository;
import org.example.ais_sst.repository.EventRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointsService {

    private final EventOrganizerRepository eventOrganizerRepository;
    private final EventParticipantsRepository eventParticipantsRepository;
    private final EventRoleRepository eventRoleRepository;
    private final PointsConfig pointsConfig;

    /**
     * Обновление баллов организатора
     */
    @Transactional
    public void updateOrganizerPoints(Long organizerId, Integer newPoints) {
        EventOrganizer organizer = eventOrganizerRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("Организатор не найден"));
        organizer.setTotalPoints(newPoints);
        eventOrganizerRepository.save(organizer);
        log.info("Updated organizer {} points to {}", organizerId, newPoints);
    }

    /**
     * Обновление баллов участника
     */
    @Transactional
    public void updateParticipantPoints(Long participantId, Integer newPoints) {
        EventParticipant participant = eventParticipantsRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Участник не найден"));
        participant.setTotalPoints(newPoints);
        eventParticipantsRepository.save(participant);
        log.info("Updated participant {} points to {}", participantId, newPoints);
    }

    /**
     * Обновление баллов роли
     */
    @Transactional
    public void updateRolePoints(Long roleId, Integer newPoints) {
        EventRole role = eventRoleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Роль не найдена"));
        role.setTotalPoints(newPoints);
        eventRoleRepository.save(role);
        log.info("Updated role {} points to {}", roleId, newPoints);
    }

    /**
     * Подтверждение присутствия организатора
     */
    @Transactional
    public void confirmOrganizerPresence(Long organizerId) {
        EventOrganizer organizer = eventOrganizerRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("Организатор не найден"));
        organizer.setWasPresent(true);
        eventOrganizerRepository.save(organizer);
        log.info("Confirmed presence for organizer {}", organizerId);
    }

    /**
     * Подтверждение присутствия участника
     */
    @Transactional
    public void confirmParticipantPresence(Long participantId) {
        EventParticipant participant = eventParticipantsRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Участник не найден"));
        participant.setWasPresent(true);
        eventParticipantsRepository.save(participant);
        log.info("Confirmed presence for participant {}", participantId);
    }

    /**
     * Подтверждение присутствия для роли
     */
    @Transactional
    public void confirmRolePresence(Long roleId) {
        EventRole role = eventRoleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Роль не найдена"));
        role.setWasPresent(true);
        eventRoleRepository.save(role);
        log.info("Confirmed presence for role {}", roleId);
    }

    /**
     * Получение общей суммы баллов организаторов на мероприятии
     */
    @Transactional(readOnly = true)
    public Long getTotalOrganizerPoints(Long eventId) {
        return eventOrganizerRepository.sumTotalPointsByEventId(eventId);
    }

    /**
     * Получение общей суммы баллов участников на мероприятии
     */
    @Transactional(readOnly = true)
    public Long getTotalParticipantPoints(Long eventId) {
        return eventParticipantsRepository.sumTotalPointsByEventId(eventId);
    }

    /**
     * Получение общей суммы баллов ролей на мероприятии
     */
    @Transactional(readOnly = true)
    public Long getTotalRolePoints(Long eventId) {
        return eventRoleRepository.sumTotalPointsByEventId(eventId);
    }

}

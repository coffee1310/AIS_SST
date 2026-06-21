package org.example.ais_sst.service.eventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.config.PointsConfig;
import org.example.ais_sst.entity.*;
import org.example.ais_sst.exception.ValidationException;
import org.example.ais_sst.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointsService {

    private final EventParticipationRecordRepository participationRecordRepository;
    private final EventOrganizerRepository eventOrganizerRepository;
    private final EventParticipantsRepository eventParticipantsRepository;
    private final EventRoleRepository eventRoleRepository;
    private final SectorParticipantRepository sectorParticipantRepository;
    private final PointsConfig pointsConfig;

    /**
     * Обновление баллов организатора (через запись об участии)
     */
    @Transactional
    public void updateOrganizerPoints(Long organizerId, Integer newPoints) {
        EventOrganizer organizer = eventOrganizerRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("Организатор не найден"));

        // Находим запись об участии для этого организатора
        EventParticipationRecord record = findParticipationRecordByUser(organizer.getUser(), organizer.getEvent().getId());

        record.setTotalPoints(newPoints);
        participationRecordRepository.save(record);
        log.info("Updated organizer {} points to {} (record {})", organizerId, newPoints, record.getId());
    }

    /**
     * Обновление баллов участника (через запись об участии)
     */
    @Transactional
    public void updateParticipantPoints(Long participantId, Integer newPoints) {
        EventParticipant participant = eventParticipantsRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Участник не найден"));

        // Находим запись об участии для этого участника
        EventParticipationRecord record = findParticipationRecordByUser(participant.getUser(), participant.getEvent().getId());

        record.setTotalPoints(newPoints);
        participationRecordRepository.save(record);
        log.info("Updated participant {} points to {} (record {})", participantId, newPoints, record.getId());
    }

    /**
     * Обновление баллов роли (НЕ ИСПОЛЬЗУЕТСЯ - баллы хранятся в записях об участии)
     */
    @Transactional
    public void updateRolePoints(Long roleId, Integer newPoints) {
        throw new UnsupportedOperationException("Баллы не хранятся в ролях. Используйте обновление записей об участии.");
    }

    /**
     * Подтверждение присутствия организатора (через запись об участии)
     */
    @Transactional
    public void confirmOrganizerPresence(Long organizerId) {
        EventOrganizer organizer = eventOrganizerRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("Организатор не найден"));

        // Находим запись об участии для этого организатора
        EventParticipationRecord record = findParticipationRecordByUser(organizer.getUser(), organizer.getEvent().getId());

        record.setWasPresent(true);
        // Если присутствует, начисляем баллы
        if (record.getTotalPoints() == 0) {
            record.setTotalPoints(pointsConfig.getDefaultOrganizerPoints());
        }
        participationRecordRepository.save(record);
        log.info("Confirmed presence for organizer {} (record {})", organizerId, record.getId());
    }

    /**
     * Подтверждение присутствия участника (через запись об участии)
     */
    @Transactional
    public void confirmParticipantPresence(Long participantId) {
        EventParticipant participant = eventParticipantsRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Участник не найден"));

        // Находим запись об участии для этого участника
        EventParticipationRecord record = findParticipationRecordByUser(participant.getUser(), participant.getEvent().getId());

        record.setWasPresent(true);
        // Если присутствует, начисляем баллы
        if (record.getTotalPoints() == 0) {
            record.setTotalPoints(pointsConfig.getDefaultParticipantPoints());
        }
        participationRecordRepository.save(record);
        log.info("Confirmed presence for participant {} (record {})", participantId, record.getId());
    }

    /**
     * Подтверждение присутствия для роли (НЕ ИСПОЛЬЗУЕТСЯ)
     */
    @Transactional
    public void confirmRolePresence(Long roleId) {
        throw new UnsupportedOperationException("Присутствие не хранится в ролях. Используйте подтверждение через записи об участии.");
    }

    /**
     * Получение общей суммы баллов организаторов на мероприятии (через записи об участии)
     */
    @Transactional(readOnly = true)
    public Long getTotalOrganizerPoints(Long eventId) {
        // Получаем всех организаторов мероприятия
        List<EventOrganizer> organizers = eventOrganizerRepository.findByEventId(eventId);
        if (organizers.isEmpty()) {
            return 0L;
        }

        // Собираем ID пользователей-организаторов
        List<Long> userIds = organizers.stream()
                .map(o -> o.getUser().getId())
                .collect(java.util.stream.Collectors.toList());

        // Получаем все записи об участии для мероприятия
        List<EventParticipationRecord> records = participationRecordRepository.findByEventId(eventId);

        // Суммируем баллы только для организаторов
        return records.stream()
                .filter(r -> r.getSectorParticipant() != null &&
                        r.getSectorParticipant().getStudent() != null &&
                        userIds.contains(r.getSectorParticipant().getStudent().getId()))
                .filter(EventParticipationRecord::getWasPresent)
                .mapToLong(EventParticipationRecord::getTotalPoints)
                .sum();
    }

    /**
     * Получение общей суммы баллов участников на мероприятии (через записи об участии)
     */
    @Transactional(readOnly = true)
    public Long getTotalParticipantPoints(Long eventId) {
        // Получаем всех участников мероприятия
        List<EventParticipant> participants = eventParticipantsRepository.findByEventId(eventId);
        if (participants.isEmpty()) {
            return 0L;
        }

        // Собираем ID пользователей-участников
        List<Long> userIds = participants.stream()
                .map(p -> p.getUser().getId())
                .collect(java.util.stream.Collectors.toList());

        // Получаем все записи об участии для мероприятия
        List<EventParticipationRecord> records = participationRecordRepository.findByEventId(eventId);

        // Суммируем баллы только для участников
        return records.stream()
                .filter(r -> r.getSectorParticipant() != null &&
                        r.getSectorParticipant().getStudent() != null &&
                        userIds.contains(r.getSectorParticipant().getStudent().getId()))
                .filter(EventParticipationRecord::getWasPresent)
                .mapToLong(EventParticipationRecord::getTotalPoints)
                .sum();
    }

    /**
     * Получение общей суммы баллов ролей на мероприятии (НЕ ИСПОЛЬЗУЕТСЯ)
     */
    @Transactional(readOnly = true)
    public Long getTotalRolePoints(Long eventId) {
        throw new UnsupportedOperationException("Баллы не хранятся в ролях. Используйте получение баллов из записей об участии.");
    }

    /**
     * Вспомогательный метод для поиска записи об участии по пользователю и мероприятию
     */
    private EventParticipationRecord findParticipationRecordByUser(User user, Long eventId) {
        // Находим sector_participant для пользователя
        List<SectorParticipant> sectorParticipants = sectorParticipantRepository.findByStudentId(user.getId());
        if (sectorParticipants.isEmpty()) {
            throw new ValidationException("Участник сектора не найден для пользователя: " + user.getId());
        }
        SectorParticipant sectorParticipant = sectorParticipants.get(0);

        // Получаем все записи об участии для мероприятия
        List<EventParticipationRecord> records = participationRecordRepository.findByEventId(eventId);

        // Ищем запись для данного пользователя
        for (EventParticipationRecord record : records) {
            if (record.getSectorParticipant() != null &&
                    record.getSectorParticipant().getId().equals(sectorParticipant.getId())) {
                return record;
            }
        }

        throw new ValidationException("Запись об участии не найдена для пользователя " + user.getId() +
                " в мероприятии " + eventId);
    }
}
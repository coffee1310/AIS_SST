package org.example.ais_sst.service.eventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.event_participant.EventParticipationRecordResponseDTO;
import org.example.ais_sst.dto.event_participant.MoveParticipantDTO;
import org.example.ais_sst.dto.event_roles.RoleOccupancyInfo;
import org.example.ais_sst.entity.EventParticipationRecord;
import org.example.ais_sst.entity.EventRole;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.exception.CapacityExceededException;
import org.example.ais_sst.exception.EventParticipationRecordNotFoundException;
import org.example.ais_sst.exception.EventRoleDoesNotFoundException;
import org.example.ais_sst.repository.EventParticipationRecordRepository;
import org.example.ais_sst.repository.EventRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventParticipantMoveService {

    private final EventParticipationRecordRepository participationRecordRepository;
    private final EventRoleRepository eventRoleRepository;

    /**
     * Переместить участника (в резерв или основной состав)
     */
    @Transactional
    public EventParticipationRecordResponseDTO moveParticipant(MoveParticipantDTO dto) {
        Long recordId = dto.getParticipationRecordId();
        Boolean targetIsReserve = dto.getIsReserve();

        log.info("Moving participant: recordId={}, targetIsReserve={}", recordId, targetIsReserve);

        EventParticipationRecord record = getParticipationRecord(recordId);
        EventRole currentRole = record.getEventRole();

        if (record.getIsReserve().equals(targetIsReserve)) {
            String statusName = targetIsReserve ? "резерве" : "основном составе";
            throw new IllegalStateException("Участник уже находится в " + statusName);
        }

        // Проверяем место
        long currentCount;
        int capacity;
        String positionName;

        if (targetIsReserve) {
            currentCount = participationRecordRepository.countByEventRoleIdAndIsReserveTrueAndIsDeletedFalse(currentRole.getId());
            capacity = currentRole.getReserveCapacity() != null ? currentRole.getReserveCapacity() : Integer.MAX_VALUE;
            positionName = "резерве";
        } else {
            currentCount = participationRecordRepository.countByEventRoleIdAndIsReserveFalseAndIsDeletedFalse(currentRole.getId());
            capacity = currentRole.getCapacity() != null ? currentRole.getCapacity() : Integer.MAX_VALUE;
            positionName = "основном составе";
        }

        if (currentCount >= capacity) {
            throw new CapacityExceededException(
                    String.format("Нет свободных мест в %s. Максимум: %d, занято: %d",
                            positionName, capacity, currentCount)
            );
        }

        record.setIsReserve(targetIsReserve);
        record.setComment(String.format("Перемещен в %s (был в %s)",
                targetIsReserve ? "резерв" : "основной состав",
                targetIsReserve ? "основном составе" : "резерве")
        );

        EventParticipationRecord saved = participationRecordRepository.save(record);
        log.info("Participant moved successfully: recordId={}, isReserve={}",
                saved.getId(), saved.getIsReserve());

        return convertToResponseDTO(saved);
    }

    @Transactional
    public EventParticipationRecordResponseDTO moveToMain(MoveParticipantDTO dto) {
        dto.setIsReserve(false);
        return moveParticipant(dto);
    }

    @Transactional
    public EventParticipationRecordResponseDTO moveToReserve(MoveParticipantDTO dto) {
        dto.setIsReserve(true);
        return moveParticipant(dto);
    }

    public boolean canMoveToStatus(Long participationRecordId, Boolean targetIsReserve) {
        try {
            EventParticipationRecord record = getParticipationRecord(participationRecordId);
            EventRole role = record.getEventRole();

            if (record.getIsReserve().equals(targetIsReserve)) {
                return false;
            }

            long currentCount;
            int capacity;

            if (targetIsReserve) {
                currentCount = participationRecordRepository.countByEventRoleIdAndIsReserveTrueAndIsDeletedFalse(role.getId());
                capacity = role.getReserveCapacity() != null ? role.getReserveCapacity() : Integer.MAX_VALUE;
            } else {
                currentCount = participationRecordRepository.countByEventRoleIdAndIsReserveFalseAndIsDeletedFalse(role.getId());
                capacity = role.getCapacity() != null ? role.getCapacity() : Integer.MAX_VALUE;
            }

            return currentCount < capacity;
        } catch (Exception e) {
            return false;
        }
    }

    public RoleOccupancyInfo getRoleOccupancyInfo(Long eventRoleId) {
        EventRole role = getEventRole(eventRoleId);

        long mainCount = participationRecordRepository.countByEventRoleIdAndIsReserveFalseAndIsDeletedFalse(eventRoleId);
        long reserveCount = participationRecordRepository.countByEventRoleIdAndIsReserveTrueAndIsDeletedFalse(eventRoleId);

        int capacity = role.getCapacity() != null ? role.getCapacity() : Integer.MAX_VALUE;
        int reserveCapacity = role.getReserveCapacity() != null ?
                role.getReserveCapacity() : Integer.MAX_VALUE;

        return RoleOccupancyInfo.builder()
                .eventRoleId(eventRoleId)
                .mainOccupied(mainCount)
                .mainCapacity(capacity)
                .reserveOccupied(reserveCount)
                .reserveCapacity(reserveCapacity)
                .mainAvailable(capacity - (int) mainCount)
                .reserveAvailable(reserveCapacity - (int) reserveCount)
                .isMainFull(mainCount >= capacity)
                .isReserveFull(reserveCount >= reserveCapacity)
                .build();
    }

    /**
     * Конвертация в DTO
     */
    private EventParticipationRecordResponseDTO convertToResponseDTO(EventParticipationRecord record) {
        User user = record.getSectorParticipant().getStudent();
        String fullName = user.getSurname() + " " + user.getName() +
                (user.getPatronymic() != null ? " " + user.getPatronymic() : "");

        return EventParticipationRecordResponseDTO.builder()
                .id(record.getId())
                .sectorParticipantId(record.getSectorParticipant().getId())
                .sectorParticipantStatus(record.getSectorParticipant().getStatus() != null ?
                        record.getSectorParticipant().getStatus().toString() : null)
                .eventRoleId(record.getEventRole().getId())
                .eventRoleTitle(record.getEventRole().getGlobalEventRole().getTitle())
                .eventId(record.getEventRole().getEvent().getId())
                .eventTitle(record.getEventRole().getEvent().getTitle())
                .userId(user.getId())
                .userFullName(fullName)
                .userEmail(user.getStudentEmail())
                .wasPresent(record.getWasPresent())
                .isReserve(record.getIsReserve())
                .totalPoints(record.getTotalPoints())
                .comment(record.getComment())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    private EventParticipationRecord getParticipationRecord(Long id) {
        return participationRecordRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new EventParticipationRecordNotFoundException(
                        "Запись об участии не найдена с id: " + id
                ));
    }

    private EventRole getEventRole(Long id) {
        return eventRoleRepository.findById(id)
                .orElseThrow(() -> new EventRoleDoesNotFoundException(
                        "Роль мероприятия не найдена с id: " + id
                ));
    }
}
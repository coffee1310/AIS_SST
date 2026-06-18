package org.example.ais_sst.repository;

import org.example.ais_sst.entity.EventParticipationRecord;
import org.example.ais_sst.entity.EventRole;
import org.example.ais_sst.entity.SectorParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventParticipationRecordRepository extends JpaRepository<EventParticipationRecord, Long> {

    // Проверка существования записи
    boolean existsBySectorParticipantAndEventRole(SectorParticipant sectorParticipant, EventRole eventRole);

    boolean existsBySectorParticipantIdAndEventRoleId(Long sectorParticipantId, Long eventRoleId);

    // Поиск по участнику сектора
    List<EventParticipationRecord> findBySectorParticipant(SectorParticipant sectorParticipant);

    List<EventParticipationRecord> findBySectorParticipantId(Long sectorParticipantId);

    // Поиск по роли мероприятия
    List<EventParticipationRecord> findByEventRole(EventRole eventRole);

    List<EventParticipationRecord> findByEventRoleId(Long eventRoleId);

    // Поиск по участнику и роли
    Optional<EventParticipationRecord> findBySectorParticipantAndEventRole(SectorParticipant sectorParticipant,
                                                                           EventRole eventRole);

    // Получить все записи с указанным статусом присутствия
    List<EventParticipationRecord> findByWasPresent(Boolean wasPresent);

    // Количество записей по роли
    long countByEventRoleId(Long eventRoleId);

    // Количество записей по роли с присутствием
    long countByEventRoleIdAndWasPresent(Long eventRoleId, Boolean wasPresent);

    /**
     * Подсчет общего количества баллов пользователя
     */
    @Query("SELECT SUM(COALESCE(epr.eventRole.globalEventRole.defaultPoints, 1)) " +
            "FROM EventParticipationRecord epr " +
            "WHERE epr.sectorParticipant.student.id = :userId " +
            "AND epr.wasPresent = true")
    Long sumPointsByUserId(@Param("userId") Long userId);

    /**
     * Подсчет количества мероприятий, на которых пользователь присутствовал
     */
    @Query("SELECT COUNT(DISTINCT epr.eventRole.event.id) " +
            "FROM EventParticipationRecord epr " +
            "WHERE epr.sectorParticipant.student.id = :userId " +
            "AND epr.wasPresent = true")
    Long countEventsByUserId(@Param("userId") Long userId);

    /**
     * Получение общего количества баллов для каждого пользователя
     * (только те, у кого есть записи с присутствием)
     */
    @Query("SELECT epr.sectorParticipant.student.id, " +
            "SUM(COALESCE(epr.eventRole.globalEventRole.defaultPoints, 1)) as totalPoints " +
            "FROM EventParticipationRecord epr " +
            "WHERE epr.wasPresent = true " +
            "GROUP BY epr.sectorParticipant.student.id " +
            "HAVING SUM(COALESCE(epr.eventRole.globalEventRole.defaultPoints, 1)) > 0")
    List<Object[]> findTotalPointsByUserWithParticipation();

    /**
     * Получение топа пользователей по баллам
     */
    @Query("SELECT epr.sectorParticipant.student.id, " +
            "epr.sectorParticipant.student.name, " +
            "epr.sectorParticipant.student.surname, " +
            "SUM(COALESCE(epr.eventRole.globalEventRole.defaultPoints, 1)) as totalPoints, " +
            "COUNT(DISTINCT epr.eventRole.event.id) as eventsCount " +
            "FROM EventParticipationRecord epr " +
            "WHERE epr.wasPresent = true " +
            "GROUP BY epr.sectorParticipant.student.id, " +
            "epr.sectorParticipant.student.name, " +
            "epr.sectorParticipant.student.surname " +
            "ORDER BY totalPoints DESC")
    List<Object[]> findTopUsersByPoints(@Param("limit") int limit);

    @Query("SELECT epr FROM EventParticipationRecord epr " +
            "WHERE epr.sectorParticipant.student.id = :userId")
    List<EventParticipationRecord> findBySectorParticipant_StudentId(@Param("userId") Long userId);

    /**
     * Получить только записи с присутствием
     */
    @Query("SELECT epr FROM EventParticipationRecord epr " +
            "WHERE epr.sectorParticipant.student.id = :userId " +
            "AND epr.wasPresent = true")
    List<EventParticipationRecord> findBySectorParticipant_StudentIdAndWasPresentTrue(@Param("userId") Long userId);

    @Query("SELECT COUNT(epr) FROM EventParticipationRecord epr " +
            "WHERE epr.sectorParticipant.student.id = :userId " +
            "AND epr.wasPresent = true")
    long countPresentRecordsByUserId(@Param("userId") Long userId);
}
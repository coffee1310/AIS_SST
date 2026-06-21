package org.example.ais_sst.repository;

import jakarta.validation.constraints.NotNull;
import org.example.ais_sst.entity.EventParticipationRecord;
import org.example.ais_sst.entity.EventRole;
import org.example.ais_sst.entity.SectorParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventParticipationRecordRepository extends JpaRepository<EventParticipationRecord, Long>,
        JpaSpecificationExecutor<EventParticipationRecord> {

    Optional<EventParticipationRecord> findById(Long id);

    // Поиск по ID роли
    List<EventParticipationRecord> findByEventRoleId(Long eventRoleId);

    // Поиск по ID участника сектора
    List<EventParticipationRecord> findBySectorParticipantId(Long sectorParticipantId);

    // Поиск по ID пользователя (через sector_participant)
    @Query("SELECT epr FROM EventParticipationRecord epr WHERE epr.sectorParticipant.student.id = :userId")
    List<EventParticipationRecord> findByUserId(@Param("userId") Long userId);

    // ИСПРАВЛЕННЫЙ МЕТОД - поиск по ID мероприятия через eventRole
    @Query("SELECT epr FROM EventParticipationRecord epr WHERE epr.eventRole.event.id = :eventId")
    List<EventParticipationRecord> findByEventId(@Param("eventId") Long eventId);

    // Проверка существования записи
    boolean existsBySectorParticipantIdAndEventRoleId(Long sectorParticipantId, Long eventRoleId);

    // Получить только записи с присутствием
    @Query("SELECT epr FROM EventParticipationRecord epr WHERE epr.eventRole.event.id = :eventId AND epr.wasPresent = true")
    List<EventParticipationRecord> findByEventIdAndWasPresentTrue(@Param("eventId") Long eventId);

    // Подсчет суммы баллов по мероприятию
    @Query("SELECT SUM(epr.totalPoints) FROM EventParticipationRecord epr WHERE epr.eventRole.event.id = :eventId AND epr.wasPresent = true")
    Long sumTotalPointsByEventId(@Param("eventId") Long eventId);

    // Подсчет количества записей по мероприятию
    @Query("SELECT COUNT(epr) FROM EventParticipationRecord epr WHERE epr.eventRole.event.id = :eventId")
    long countByEventId(@Param("eventId") Long eventId);

    // Обновление статуса присутствия
    @Modifying
    @Transactional
    @Query("UPDATE EventParticipationRecord epr SET epr.wasPresent = :wasPresent WHERE epr.id = :id")
    void updateWasPresent(@Param("id") Long id, @Param("wasPresent") Boolean wasPresent);

    // Обновление баллов
    @Modifying
    @Transactional
    @Query("UPDATE EventParticipationRecord epr SET epr.totalPoints = :points WHERE epr.id = :id")
    void updateTotalPoints(@Param("id") Long id, @Param("points") Integer points);

    // Подсчет количества записей с присутствием
    @Query("SELECT COUNT(epr) FROM EventParticipationRecord epr WHERE epr.eventRole.event.id = :eventId AND epr.wasPresent = true")
    long countPresentByEventId(@Param("eventId") Long eventId);

    boolean existsBySectorParticipantAndEventRole(@NotNull SectorParticipant sectorParticipant, @NotNull EventRole eventRole);

    Optional<EventParticipationRecord> findBySectorParticipantIdAndEventRoleId(Long id, Long id1);


    // Поиск по участнику сектора
    List<EventParticipationRecord> findBySectorParticipant(SectorParticipant sectorParticipant);

    // Поиск по роли мероприятия
    List<EventParticipationRecord> findByEventRole(EventRole eventRole);

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

    // Метод для фильтрации
    @Query("SELECT epr FROM EventParticipationRecord epr " +
            "JOIN epr.eventRole er " +
            "JOIN er.event e " +
            "JOIN epr.sectorParticipant sp " +
            "JOIN sp.student u " +
            "WHERE e.id = :eventId " +
            "AND (:fullName IS NULL OR LOWER(CONCAT(u.name, ' ', u.surname)) LIKE LOWER(CONCAT('%', :fullName, '%'))) " +
            "AND (:minPoints IS NULL OR epr.totalPoints >= :minPoints) " +
            "AND (:maxPoints IS NULL OR epr.totalPoints <= :maxPoints) " +
            "AND (:wasPresent IS NULL OR epr.wasPresent = :wasPresent) " +
            "AND (:roleTitle IS NULL OR LOWER(er.globalEventRole.title) LIKE LOWER(CONCAT('%', :roleTitle, '%')))")
    List<EventParticipationRecord> findByEventIdWithFilters(@Param("eventId") Long eventId,
                                                            @Param("fullName") String fullName,
                                                            @Param("minPoints") Integer minPoints,
                                                            @Param("maxPoints") Integer maxPoints,
                                                            @Param("wasPresent") Boolean wasPresent,
                                                            @Param("roleTitle") String roleTitle);
}
package org.example.ais_sst.repository;

import jakarta.validation.constraints.NotNull;
import org.example.ais_sst.dto.user.UserParticipationInfoDTO;
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

    // Количество записей по роли с присутствием
    long countByEventRoleIdAndWasPresent(Long eventRoleId, Boolean wasPresent);


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

    // Добавить методы для работы с isDeleted
    Optional<EventParticipationRecord> findBySectorParticipantIdAndEventRoleIdAndIsDeletedTrue(
            Long sectorParticipantId, Long eventRoleId);

    @Query("SELECT epr FROM EventParticipationRecord epr WHERE epr.eventRole.event.id = :eventId AND epr.isDeleted = false")
    List<EventParticipationRecord> findActiveByEventId(@Param("eventId") Long eventId);

    long countByEventRoleIdAndIsDeletedFalse(Long eventRoleId);

    @Query("SELECT COUNT(epr) FROM EventParticipationRecord epr " +
            "WHERE epr.eventRole.id = :eventRoleId " +
            "AND epr.isDeleted = false")
    long countByEventRoleId(@Param("eventRoleId") Long eventRoleId);

    @Query("SELECT COUNT(epr) FROM EventParticipationRecord epr " +
            "WHERE epr.eventRole.id = :eventRoleId " +
            "AND epr.wasPresent = false " +
            "AND epr.isDeleted = false")
    long countByEventRoleIdAndWasPresentFalse(@Param("eventRoleId") Long eventRoleId);

    Optional<EventParticipationRecord> findByIdAndIsDeletedFalse(Long id);

    List<EventParticipationRecord> findBySectorParticipantIdAndIsDeletedFalse(Long sectorParticipantId);

    long countByEventRoleIdAndIsReserveTrueAndIsDeletedFalse(Long id);

    long countByEventRoleIdAndIsReserveFalseAndIsDeletedFalse(Long id);

    List<EventParticipationRecord> findByEventRoleIdInAndIsDeletedFalse(List<Long> eventRoleIds);

    @Query("""
        SELECT epr FROM EventParticipationRecord epr
        WHERE epr.sectorParticipant.student.id = :userId
        AND epr.wasPresent = true
        AND epr.isDeleted = false
        AND epr.eventRole.event.isCompleted = true
        AND epr.eventRole.event.isDeleted = false
    """)
    List<EventParticipationRecord> findActiveParticipationRecordsByUserId(@Param("userId") Long userId);

    /**
     * Подсчет общего количества баллов пользователя
     */
    @Query("""
        SELECT COALESCE(SUM(epr.totalPoints), 0) FROM EventParticipationRecord epr
        WHERE epr.sectorParticipant.student.id = :userId
        AND epr.wasPresent = true
        AND epr.isDeleted = false
        AND epr.eventRole.event.isCompleted = true
        AND epr.eventRole.event.isDeleted = false
    """)
    Integer sumPointsByUserId(@Param("userId") Long userId);

    /**
     * Получить все записи участия пользователя с деталями для подсчета
     */
    @Query("""
        SELECT new org.example.ais_sst.dto.user.UserParticipationInfoDTO(
            epr.id,
            epr.eventRole.event.id,
            epr.eventRole.event.title,
            epr.eventRole.globalEventRole.title,
            epr.totalPoints,
            epr.wasPresent,
            epr.eventRole.event.isCompleted
        )
        FROM EventParticipationRecord epr
        WHERE epr.sectorParticipant.student.id = :userId
        AND epr.isDeleted = false
        AND epr.eventRole.event.isDeleted = false
        AND epr.eventRole.event.isCompleted = true
    """)
    List<UserParticipationInfoDTO> findParticipationInfoByUserId(@Param("userId") Long userId);

    /**
     * Получить топ пользователей по баллам с учетом всех условий
     */
    @Query(value = """
        SELECT 
            u.id,
            u.name,
            u.surname,
            COALESCE(SUM(epr.total_points), 0) as total_points,
            COUNT(DISTINCT epr.event_role_id) as events_count
        FROM users u
        INNER JOIN sector_participants sp ON sp.student_id = u.id
        INNER JOIN event_participation_records epr ON epr.sector_participant_id = sp.id
        INNER JOIN event_roles er ON er.id = epr.event_role_id
        INNER JOIN events e ON e.id = er.event_id
        WHERE epr.was_present = true
        AND epr.is_deleted = false
        AND e.is_completed = true
        AND e.is_deleted = false
        AND u.is_deleted = false
        GROUP BY u.id, u.name, u.surname
        ORDER BY total_points DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> findTopUsersByPointsNative(@Param("limit") int limit);

    /**
     * Получить позицию пользователя в рейтинге
     * Учитываются все источники баллов:
     * - EventParticipationRecord (участие через сектора)
     * - EventOrganizer (организация)
     * - EventParticipant (участие как участник)
     * - TaskUser (задачи)
     *
     * При одинаковых баллах позиция определяется по ID пользователя (меньше ID - выше позиция)
     */
    @Query(value = """
    WITH user_points AS (
        -- 1. EventParticipationRecord (участие через сектора)
        SELECT 
            u.id as user_id,
            COALESCE(SUM(epr.total_points), 0) as points
        FROM users u
        LEFT JOIN sector_participants sp ON sp.student_id = u.id
        LEFT JOIN event_participation_records epr ON epr.sector_participant_id = sp.id
            AND epr.was_present = true
            AND epr.is_deleted = false
        LEFT JOIN event_roles er ON er.id = epr.event_role_id
        LEFT JOIN events e ON e.id = er.event_id
            AND e.is_completed = true
            AND e.is_deleted = false
        WHERE u.is_deleted = false
        GROUP BY u.id
        
        UNION ALL
        
        -- 2. EventOrganizer (организация)
        SELECT 
            u.id as user_id,
            COALESCE(SUM(eo.total_points), 0) as points
        FROM users u
        LEFT JOIN event_organizers eo ON eo.user_id = u.id
            AND eo.was_present = true
            AND eo.is_deleted = false
        LEFT JOIN events e ON e.id = eo.event_id
            AND e.is_completed = true
            AND e.is_deleted = false
        WHERE u.is_deleted = false
        GROUP BY u.id
        
        UNION ALL
        
        -- 3. EventParticipant (участие как участник)
        SELECT 
            u.id as user_id,
            COALESCE(SUM(ep.total_points), 0) as points
        FROM users u
        LEFT JOIN event_participants ep ON ep.user_id = u.id
            AND ep.was_present = true
            AND ep.is_deleted = false
        LEFT JOIN events e ON e.id = ep.event_id
            AND e.is_completed = true
            AND e.is_deleted = false
        WHERE u.is_deleted = false
        GROUP BY u.id
        
        UNION ALL
        
        -- 4. TaskUser (задачи)
        SELECT 
            u.id as user_id,
            COALESCE(SUM(t.count_of_points), 0) as points
        FROM users u
        LEFT JOIN tasks_users tu ON tu.user_id = u.id
            AND tu.is_completed = true
            AND tu.is_deleted = false
        LEFT JOIN tasks t ON t.id = tu.task_id
            AND t.is_completed = true
            AND t.is_deleted = false
        WHERE u.is_deleted = false
        GROUP BY u.id
    ),
    total_points AS (
        SELECT 
            user_id,
            SUM(points) as total_points
        FROM user_points
        GROUP BY user_id
    ),
    ranked_users AS (
        SELECT 
            user_id,
            total_points,
            -- ROW_NUMBER() дает уникальные позиции даже при одинаковых баллах
            -- Сортировка: сначала по баллам DESC, затем по ID ASC (меньше ID - выше)
            ROW_NUMBER() OVER (ORDER BY total_points DESC, user_id ASC) as position
        FROM total_points
    )
    SELECT position
    FROM ranked_users
    WHERE user_id = :userId
""", nativeQuery = true)
    Integer findUserRatingPosition(@Param("userId") Long userId);

    // === ДОБАВЬТЕ ЭТОТ МЕТОД В КЛАСС EventParticipationRecordRepository.java ===
// Вставьте перед закрывающей } класса

    /**
     * Получить ВСЕХ пользователей (не удаленных), отсортированных по полному рейтингу (все источники баллов).
     * Используется для отчета по пользователям в ReportController.
     * Возвращает Object[] : [position, user_id, name, surname, patronymic, role_title, total_points]
     * Позиция рассчитывается с учетом всех баллов (как в findUserRatingPosition).
     */
    @Query(value = """
WITH user_points AS (
    -- 1. EventParticipationRecord (участие через сектора)
    SELECT 
        u.id as user_id,
        u.name,
        u.surname,
        u.patronymic,
        r.title as role_title,
        COALESCE(SUM(epr.total_points), 0) as points
    FROM users u
    LEFT JOIN roles r ON r.id = u.role_id
    LEFT JOIN sector_participants sp ON sp.student_id = u.id
    LEFT JOIN event_participation_records epr ON epr.sector_participant_id = sp.id
        AND epr.was_present = true
        AND epr.is_deleted = false
    LEFT JOIN event_roles er ON er.id = epr.event_role_id
    LEFT JOIN events e ON e.id = er.event_id
        AND e.is_completed = true
        AND e.is_deleted = false
    WHERE u.is_deleted = false
    GROUP BY u.id, u.name, u.surname, u.patronymic, r.title
    
    UNION ALL
    
    -- 2. EventOrganizer (организация мероприятий)
    SELECT 
        u.id as user_id,
        u.name,
        u.surname,
        u.patronymic,
        r.title as role_title,
        COALESCE(SUM(eo.total_points), 0) as points
    FROM users u
    LEFT JOIN roles r ON r.id = u.role_id
    LEFT JOIN event_organizers eo ON eo.user_id = u.id
        AND eo.was_present = true
        AND eo.is_deleted = false
    LEFT JOIN events e ON e.id = eo.event_id
        AND e.is_completed = true
        AND e.is_deleted = false
    WHERE u.is_deleted = false
    GROUP BY u.id, u.name, u.surname, u.patronymic, r.title
    
    UNION ALL
    
    -- 3. EventParticipant (участие как участник)
    SELECT 
        u.id as user_id,
        u.name,
        u.surname,
        u.patronymic,
        r.title as role_title,
        COALESCE(SUM(ep.total_points), 0) as points
    FROM users u
    LEFT JOIN roles r ON r.id = u.role_id
    LEFT JOIN event_participants ep ON ep.user_id = u.id
        AND ep.was_present = true
        AND ep.is_deleted = false
    LEFT JOIN events e ON e.id = ep.event_id
        AND e.is_completed = true
        AND e.is_deleted = false
    WHERE u.is_deleted = false
    GROUP BY u.id, u.name, u.surname, u.patronymic, r.title
    
    UNION ALL
    
    -- 4. TaskUser (выполненные задачи)
    SELECT 
        u.id as user_id,
        u.name,
        u.surname,
        u.patronymic,
        r.title as role_title,
        COALESCE(SUM(t.count_of_points), 0) as points
    FROM users u
    LEFT JOIN roles r ON r.id = u.role_id
    LEFT JOIN tasks_users tu ON tu.user_id = u.id
        AND tu.is_completed = true
        AND tu.is_deleted = false
    LEFT JOIN tasks t ON t.id = tu.task_id
        AND t.is_completed = true
        AND t.is_deleted = false
    WHERE u.is_deleted = false
    GROUP BY u.id, u.name, u.surname, u.patronymic, r.title
),
total_points AS (
    SELECT 
        user_id,
        name,
        surname,
        patronymic,
        role_title,
        SUM(points) as total_points
    FROM user_points
    GROUP BY user_id, name, surname, patronymic, role_title
),
ranked_users AS (
    SELECT 
        user_id,
        name,
        surname,
        patronymic,
        role_title,
        total_points,
        ROW_NUMBER() OVER (ORDER BY total_points DESC, user_id ASC) as position
    FROM total_points
)
SELECT 
    position,
    user_id,
    name,
    surname,
    patronymic,
    role_title,
    total_points
FROM ranked_users
ORDER BY position ASC
""", nativeQuery = true)
    List<Object[]> findAllRankedUsersByPointsNative();

    long countByEventRole_IdAndWasPresentTrueAndIsReserveFalseAndIsDeletedFalse(Long eventRoleId);

    // Резерв
    long countByEventRole_IdAndWasPresentTrueAndIsReserveTrueAndIsDeletedFalse(Long eventRoleId);

    // Fallback (все активные)
    long countByEventRole_IdAndWasPresentTrueAndIsDeletedFalse(Long eventRoleId);

}
package org.example.ais_sst.repository;

import org.example.ais_sst.entity.EventRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRoleRepository extends JpaRepository<EventRole, Long> {

    Optional<EventRole> findById(Long id);


    @Query(value = """
        SELECT * FROM event_roles er WHERE 
        (CAST(:id AS bigint) IS NULL OR er.id = CAST(:id AS bigint)) AND
        (CAST(:eventId AS bigint) IS NULL OR er.event_id = CAST(:eventId AS bigint)) AND
        (CAST(:globalEventRoleId AS bigint) IS NULL OR er.global_event_role_id = CAST(:globalEventRoleId AS bigint)) AND
        (CAST(:deleted AS boolean) IS NULL OR er.is_deleted = CAST(:deleted AS boolean)) AND
        (CAST(:deadlineFrom AS timestamp) IS NULL OR er.deadline >= CAST(:deadlineFrom AS timestamp)) AND
        (CAST(:deadlineTo AS timestamp) IS NULL OR er.deadline <= CAST(:deadlineTo AS timestamp))
        ORDER BY er.id
        OFFSET :offset LIMIT :limit
        """, nativeQuery = true)
    List<EventRole> findAllWithFiltersNative(
            @Param("id") Long id,
            @Param("eventId") Long eventId,
            @Param("globalEventRoleId") Long globalEventRoleId,
            @Param("deleted") Boolean deleted,
            @Param("deadlineFrom") LocalDateTime deadlineFrom,
            @Param("deadlineTo") LocalDateTime deadlineTo,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Query(value = """
        SELECT COUNT(*) FROM event_roles er WHERE 
        (CAST(:id AS bigint) IS NULL OR er.id = CAST(:id AS bigint)) AND
        (CAST(:eventId AS bigint) IS NULL OR er.event_id = CAST(:eventId AS bigint)) AND
        (CAST(:globalEventRoleId AS bigint) IS NULL OR er.global_event_role_id = CAST(:globalEventRoleId AS bigint)) AND
        (CAST(:deleted AS boolean) IS NULL OR er.is_deleted = CAST(:deleted AS boolean)) AND
        (CAST(:deadlineFrom AS timestamp) IS NULL OR er.deadline >= CAST(:deadlineFrom AS timestamp)) AND
        (CAST(:deadlineTo AS timestamp) IS NULL OR er.deadline <= CAST(:deadlineTo AS timestamp))
        """, nativeQuery = true)
    long countAllWithFiltersNative(
            @Param("id") Long id,
            @Param("eventId") Long eventId,
            @Param("globalEventRoleId") Long globalEventRoleId,
            @Param("deleted") Boolean deleted,
            @Param("deadlineFrom") LocalDateTime deadlineFrom,
            @Param("deadlineTo") LocalDateTime deadlineTo);

    @Query(value = """
        SELECT DISTINCT er.* FROM event_roles er
        INNER JOIN global_event_roles ger ON er.global_event_role_id = ger.id
        INNER JOIN sector_participants sp ON ger.sector_id = sp.sector_id
        WHERE sp.student_id = :userId
        AND sp.status = 'Активный'
        AND (CAST(:id AS bigint) IS NULL OR er.id = CAST(:id AS bigint))
        AND (CAST(:eventId AS bigint) IS NULL OR er.event_id = CAST(:eventId AS bigint))
        AND (CAST(:globalEventRoleId AS bigint) IS NULL OR er.global_event_role_id = CAST(:globalEventRoleId AS bigint))
        AND (CAST(:deleted AS boolean) IS NULL OR er.is_deleted = CAST(:deleted AS boolean))
        AND (CAST(:deadlineFrom AS timestamp) IS NULL OR er.deadline >= CAST(:deadlineFrom AS timestamp))
        AND (CAST(:deadlineTo AS timestamp) IS NULL OR er.deadline <= CAST(:deadlineTo AS timestamp))
        ORDER BY er.id
        OFFSET :offset LIMIT :limit
        """, nativeQuery = true)
    List<EventRole> findAllWithFiltersAndMySectorNative(
            @Param("id") Long id,
            @Param("eventId") Long eventId,
            @Param("globalEventRoleId") Long globalEventRoleId,
            @Param("deleted") Boolean deleted,
            @Param("deadlineFrom") LocalDateTime deadlineFrom,
            @Param("deadlineTo") LocalDateTime deadlineTo,
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    // ИСПРАВЛЕННЫЙ МЕТОД - подсчет через global_event_roles.sector_id
    @Query(value = """
        SELECT COUNT(DISTINCT er.id) FROM event_roles er
        INNER JOIN global_event_roles ger ON er.global_event_role_id = ger.id
        INNER JOIN sector_participants sp ON ger.sector_id = sp.sector_id
        WHERE sp.student_id = :userId
        AND sp.status = 'Активный'
        AND (CAST(:id AS bigint) IS NULL OR er.id = CAST(:id AS bigint))
        AND (CAST(:eventId AS bigint) IS NULL OR er.event_id = CAST(:eventId AS bigint))
        AND (CAST(:globalEventRoleId AS bigint) IS NULL OR er.global_event_role_id = CAST(:globalEventRoleId AS bigint))
        AND (CAST(:deleted AS boolean) IS NULL OR er.is_deleted = CAST(:deleted AS boolean))
        AND (CAST(:deadlineFrom AS timestamp) IS NULL OR er.deadline >= CAST(:deadlineFrom AS timestamp))
        AND (CAST(:deadlineTo AS timestamp) IS NULL OR er.deadline <= CAST(:deadlineTo AS timestamp))
        """, nativeQuery = true)
    long countAllWithFiltersAndMySectorNative(
            @Param("id") Long id,
            @Param("eventId") Long eventId,
            @Param("globalEventRoleId") Long globalEventRoleId,
            @Param("deleted") Boolean deleted,
            @Param("deadlineFrom") LocalDateTime deadlineFrom,
            @Param("deadlineTo") LocalDateTime deadlineTo,
            @Param("userId") Long userId);

    @Query(value = """
    SELECT DISTINCT er.* FROM event_roles er
    WHERE er.is_deleted = false
    AND er.id NOT IN (
        SELECT DISTINCT er2.id FROM event_roles er2
        INNER JOIN global_event_roles ger ON er2.global_event_role_id = ger.id
        INNER JOIN sector_participants sp ON ger.sector_id = sp.sector_id
        WHERE sp.student_id = :userId
        AND sp.status = 'Активный'
    )
    AND (CAST(:id AS bigint) IS NULL OR er.id = CAST(:id AS bigint))
    AND (CAST(:eventId AS bigint) IS NULL OR er.event_id = CAST(:eventId AS bigint))
    AND (CAST(:globalEventRoleId AS bigint) IS NULL OR er.global_event_role_id = CAST(:globalEventRoleId AS bigint))
    AND (CAST(:deleted AS boolean) IS NULL OR er.is_deleted = CAST(:deleted AS boolean))
    AND (CAST(:deadlineFrom AS timestamp) IS NULL OR er.deadline >= CAST(:deadlineFrom AS timestamp))
    AND (CAST(:deadlineTo AS timestamp) IS NULL OR er.deadline <= CAST(:deadlineTo AS timestamp))
    ORDER BY er.id
    OFFSET :offset LIMIT :limit
    """, nativeQuery = true)
    List<EventRole> findAllWithFiltersAndNotMySectorNative(
            @Param("id") Long id,
            @Param("eventId") Long eventId,
            @Param("globalEventRoleId") Long globalEventRoleId,
            @Param("deleted") Boolean deleted,
            @Param("deadlineFrom") LocalDateTime deadlineFrom,
            @Param("deadlineTo") LocalDateTime deadlineTo,
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Query(value = """
    SELECT COUNT(DISTINCT er.id) FROM event_roles er
    WHERE er.is_deleted = false
    AND er.id NOT IN (
        SELECT DISTINCT er2.id FROM event_roles er2
        INNER JOIN global_event_roles ger ON er2.global_event_role_id = ger.id
        INNER JOIN sector_participants sp ON ger.sector_id = sp.sector_id
        WHERE sp.student_id = :userId
        AND sp.status = 'Активный'
    )
    AND (CAST(:id AS bigint) IS NULL OR er.id = CAST(:id AS bigint))
    AND (CAST(:eventId AS bigint) IS NULL OR er.event_id = CAST(:eventId AS bigint))
    AND (CAST(:globalEventRoleId AS bigint) IS NULL OR er.global_event_role_id = CAST(:globalEventRoleId AS bigint))
    AND (CAST(:deleted AS boolean) IS NULL OR er.is_deleted = CAST(:deleted AS boolean))
    AND (CAST(:deadlineFrom AS timestamp) IS NULL OR er.deadline >= CAST(:deadlineFrom AS timestamp))
    AND (CAST(:deadlineTo AS timestamp) IS NULL OR er.deadline <= CAST(:deadlineTo AS timestamp))
    """, nativeQuery = true)
    long countAllWithFiltersAndNotMySectorNative(
            @Param("id") Long id,
            @Param("eventId") Long eventId,
            @Param("globalEventRoleId") Long globalEventRoleId,
            @Param("deleted") Boolean deleted,
            @Param("deadlineFrom") LocalDateTime deadlineFrom,
            @Param("deadlineTo") LocalDateTime deadlineTo,
            @Param("userId") Long userId);

    boolean existsByEventIdAndGlobalEventRoleIdAndDeletedFalse(Long eventId, Long globalEventRoleId);

    Optional<EventRole> findByEventIdAndGlobalEventRoleIdAndDeletedFalse(Long eventId, Long globalEventRoleId);


    List<EventRole> findByEventIdAndDeletedFalse(Long eventId);

    List<EventRole> findByEventId(Long eventId);

    long countByEventId(Long eventId);
}
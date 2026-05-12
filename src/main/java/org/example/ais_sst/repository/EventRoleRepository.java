package org.example.ais_sst.repository;

import org.example.ais_sst.entity.EventRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    boolean existsByEventIdAndGlobalEventRoleIdAndDeletedFalse(Long eventId, Long globalEventRoleId);

    Optional<EventRole> findByEventIdAndGlobalEventRoleIdAndDeletedFalse(Long eventId, Long globalEventRoleId);

    @Query("SELECT er FROM EventRole er WHERE er.event.id = :eventId AND er.deleted = false")
    List<EventRole> findActiveByEventId(@Param("eventId") Long eventId);

    @Query("SELECT er FROM EventRole er WHERE er.deadline < :now AND er.deleted = false")
    List<EventRole> findExpiredRoles(@Param("now") LocalDateTime now);
}
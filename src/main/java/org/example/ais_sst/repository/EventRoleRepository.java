package org.example.ais_sst.repository;

import org.example.ais_sst.entity.EventRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventRoleRepository extends JpaRepository<EventRole, Long> {

    Optional<EventRole> findById(Long id);

    @Query("SELECT er FROM EventRole er WHERE " +
            "(:id IS NULL OR er.id = :id) AND " +
            "(:eventId IS NULL OR er.event.id = :eventId) AND " +
            "(:globalEventRoleId IS NULL OR er.globalEventRole.id = :globalEventRoleId) AND " +
            "(:deleted IS NULL OR er.deleted = :deleted)")
    Page<EventRole> findAllWithFilters(
            @Param("id") Long id,
            @Param("eventId") Long eventId,
            @Param("globalEventRoleId") Long globalEventRoleId,
            @Param("deleted") Boolean deleted,
            Pageable pageable);

    boolean existsByEventIdAndGlobalEventRoleIdAndDeletedFalse(Long eventId, Long globalEventRoleId);
}
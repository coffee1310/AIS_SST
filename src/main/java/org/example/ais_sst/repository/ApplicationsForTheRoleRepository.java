package org.example.ais_sst.repository;

import org.example.ais_sst.entity.ApplicationsForTheRole;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.example.ais_sst.entity.ApplicationsForTheRole;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;
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
public interface ApplicationsForTheRoleRepository extends
        JpaRepository<ApplicationsForTheRole, Long>,
        JpaSpecificationExecutor<ApplicationsForTheRole> {

    Optional<ApplicationsForTheRole> findById(Long id);

    Page<ApplicationsForTheRole> findBySectorParticipantId(Long sectorParticipantId, Pageable pageable);

    @Query("SELECT a FROM ApplicationsForTheRole a WHERE " +
            "(:id IS NULL OR a.id = :id) AND " +
            "(:sectorParticipantId IS NULL OR a.sectorParticipant.id = :sectorParticipantId) AND " +
            "(:eventRoleId IS NULL OR a.eventRole.id = :eventRoleId) AND " +
            "(:eventId IS NULL OR a.eventRole.event.id = :eventId) AND " +
            "(:status IS NULL OR a.status = :status) AND " +
            "(:isReserve IS NULL OR a.isReserve = :isReserve) AND " +
            "(:dateFrom IS NULL OR a.createdAt >= :dateFrom) AND " +
            "(:dateTo IS NULL OR a.createdAt <= :dateTo)")
    Page<ApplicationsForTheRole> findAllWithFilters(
            @Param("id") Long id,
            @Param("sectorParticipantId") Long sectorParticipantId,
            @Param("eventRoleId") Long eventRoleId,
            @Param("eventId") Long eventId,
            @Param("status") RoleApplicationStatuses status,
            @Param("isReserve") Boolean isReserve,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);

    @Query("SELECT a FROM ApplicationsForTheRole a WHERE a.sectorParticipant.id IN :sectorParticipantIds")
    Page<ApplicationsForTheRole> findBySectorParticipantIdIn(@Param("sectorParticipantIds") List<Long> sectorParticipantIds, Pageable pageable);

    boolean existsBySectorParticipantIdAndEventRoleId(Long sectorParticipantId, Long eventRoleId);

    @Query("SELECT COUNT(a) FROM ApplicationsForTheRole a WHERE a.eventRole.id = :eventRoleId AND a.status = 'Одобрена'")
    long countApprovedByEventRoleId(@Param("eventRoleId") Long eventRoleId);
}
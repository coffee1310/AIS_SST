package org.example.ais_sst.repository;

import org.example.ais_sst.entity.ApplicationsForTheRole;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ApplicationsForTheRoleRepository extends JpaRepository<ApplicationsForTheRole, Long> {

    Optional<ApplicationsForTheRole> findById(Long id);

    @Query("SELECT COUNT(ra) FROM ApplicationsForTheRole ra WHERE ra.eventRole.id = :eventRoleId AND ra.status = 'APPROVED'")
    long countApprovedByEventRoleId(@Param("eventRoleId") Long eventRoleId);

    boolean existsByStudentIdAndEventRoleId(Long studentId, Long eventRoleId);

    Page<ApplicationsForTheRole> findByStudentId(Long studentId, Pageable pageable);

    @Query("SELECT ra FROM ApplicationsForTheRole ra WHERE " +
            "(:id IS NULL OR ra.id = :id) AND " +
            "(:studentId IS NULL OR ra.student.id = :studentId) AND " +
            "(:eventRoleId IS NULL OR ra.eventRole.id = :eventRoleId) AND " +
            "(:eventId IS NULL OR ra.eventRole.event.id = :eventId) AND " +
            "(:status IS NULL OR ra.status = :status) AND " +
            "(:isReserve IS NULL OR ra.isReserve = :isReserve) AND " +
            "(:dateFrom IS NULL OR ra.createdAt >= :dateFrom) AND " +
            "(:dateTo IS NULL OR ra.createdAt <= :dateTo)")
    Page<ApplicationsForTheRole> findAllWithFilters(
            @Param("id") Long id,
            @Param("studentId") Long studentId,
            @Param("eventRoleId") Long eventRoleId,
            @Param("eventId") Long eventId,
            @Param("status") RoleApplicationStatuses status,
            @Param("isReserve") Boolean isReserve,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);
}
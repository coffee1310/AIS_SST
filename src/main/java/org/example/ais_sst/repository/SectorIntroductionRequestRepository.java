package org.example.ais_sst.repository;

import org.example.ais_sst.entity.SectorIntroductionRequest;
import org.example.ais_sst.entity.enums.SectorIntroductionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectorIntroductionRequestRepository extends JpaRepository<SectorIntroductionRequest, Long> {


    List<SectorIntroductionRequest> getSectorIntroductionRequestsBySector_Id(Long sectorId);

    List<SectorIntroductionRequest> getSectorIntroductionRequestsBySector_IdAndStatus(Long sectorId, SectorIntroductionStatus status);

    @Query("SELECT sir FROM SectorIntroductionRequest sir WHERE sir.sector.id IN " +
            "(SELECT sp.sector.id FROM SectorParticipant sp WHERE sp.student.id = :coordinatorId AND sp.isCoordinator = true)")
    List<SectorIntroductionRequest> findRequestsByCoordinatorId(@Param("coordinatorId") Long coordinatorId);

    List<SectorIntroductionRequest> findByUserId(Long userId);

    List<SectorIntroductionRequest> findByUserIdAndStatus(Long userId, SectorIntroductionStatus status);

    // Получить заявки с фильтром по статусу из секторов, где пользователь является координатором
    @Query("SELECT sir FROM SectorIntroductionRequest sir WHERE sir.sector.id IN " +
            "(SELECT sp.sector.id FROM SectorParticipant sp WHERE sp.student.id = :coordinatorId AND sp.isCoordinator = true) " +
            "AND (:status IS NULL OR sir.status = :status)")
    Page<SectorIntroductionRequest> findRequestsByCoordinatorIdAndStatus(
            @Param("coordinatorId") Long coordinatorId,
            @Param("status") SectorIntroductionStatus status,
            Pageable pageable);


    @Query(value = "SELECT * FROM sector_introduction_request sir WHERE sir.sector_id IN " +
            "(SELECT sp.sector_id FROM sector_participants sp WHERE sp.student_id = :coordinatorId AND sp.is_coordinator = true) " +
            "AND sir.status = CAST(:status AS sector_introduction_statuses)",
            nativeQuery = true)
    List<SectorIntroductionRequest> findRequestsByCoordinatorIdAndStatus(
            @Param("coordinatorId") Long coordinatorId,
            @Param("status") String status);

    List<SectorIntroductionRequest> findByUserIdAndSectorIdAndStatusIn(
            Long userId,
            Long sectorId,
            List<SectorIntroductionStatus> statuses
    );
}

package org.example.ais_sst.repository;

import org.example.ais_sst.entity.SectorIntroductionRequest;
import org.example.ais_sst.entity.enums.SectorIntroductionStatus;
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
}

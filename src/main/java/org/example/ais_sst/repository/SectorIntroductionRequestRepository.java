package org.example.ais_sst.repository;

import org.example.ais_sst.entity.SectorIntroductionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectorIntroductionRequestRepository extends JpaRepository<SectorIntroductionRequest, Long> {


    List<SectorIntroductionRequest> getSectorIntroductionRequestsBySectorCurrentCoordinator_Id(Long sectorId);

    List<SectorIntroductionRequest> getSectorIntroductionRequestsBySector_Id(Long sectorId);
}

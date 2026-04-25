package org.example.ais_sst.repository;

import org.example.ais_sst.entity.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Long> {
    boolean existsByTitle(String title);

    boolean existsByCurrentCoordinator_IdAndId(Long currentCoordinatorId, Long id);
    
    Optional<Sector> findSectorById(Long id);

    Optional<Sector> findSectorsByCurrentCoordinator_Id(Long currentCoordinatorId);
}

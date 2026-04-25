package org.example.ais_sst.repository;

import org.example.ais_sst.entity.SectorParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SectorParticipantRepository extends JpaRepository<SectorParticipant, Long> {
    boolean existsByStudent_IdAndSector_Id(Long studentId, Long sectorId);
}

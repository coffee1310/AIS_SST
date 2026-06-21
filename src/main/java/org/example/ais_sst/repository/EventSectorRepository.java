package org.example.ais_sst.repository;

import org.example.ais_sst.entity.EventSector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventSectorRepository extends JpaRepository<EventSector, Long> {
    boolean existsByEventIdAndSectorId(Long eventId, Long sectorId);

    void deleteByEventId(Long eventId);
}

package org.example.ais_sst.repository;

import org.example.ais_sst.entity.EventSector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventSectorRepository extends JpaRepository<EventSector, Long> {
    boolean existsByEventIdAndSectorId(Long eventId, Long sectorId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM EventSector es WHERE es.event.id = :eventId")
    void deleteByEventId(@Param("eventId") Long eventId);
}

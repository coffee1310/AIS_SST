package org.example.ais_sst.repository;

import org.example.ais_sst.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends
        JpaRepository<Event, Long>,
        JpaSpecificationExecutor<Event> {   // ← добавили это

    Optional<Event> findById(Long id);

    Page<Event> findByIsActiveTrue(Pageable pageable);

    Page<Event> findByEventCreatorId(Long creatorId, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.startTime >= :now ORDER BY e.startTime ASC")
    List<Event> findUpcomingEvents(@Param("now") LocalDateTime now);

    @Query("SELECT e FROM Event e WHERE e.endTime <= :now ORDER BY e.startTime DESC")
    List<Event> findPastEvents(@Param("now") LocalDateTime now);

    boolean existsByIdAndEventCreatorId(Long eventId, Long creatorId);
}
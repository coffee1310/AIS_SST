package org.example.ais_sst.repository;

import org.example.ais_sst.entity.EventOrganizer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventOrganizerRepository extends JpaRepository<EventOrganizer, Long> {

    List<EventOrganizer> findByEventId(Long eventId);

    List<EventOrganizer> findByUserId(Long userId);

    Optional<EventOrganizer> findByEventIdAndUserId(Long eventId, Long userId);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    void deleteByEventIdAndUserId(Long eventId, Long userId);

    void deleteByEventId(Long eventId);

    long countByEventId(Long eventId);
}
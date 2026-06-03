package org.example.ais_sst.repository;

import org.example.ais_sst.entity.EventParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventParticipantsRepository extends JpaRepository<EventParticipant, Long> {
    Optional<EventParticipant> findByEventIdAndUserId(Long eventId, Long userId);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    List<EventParticipant> findByEventId(Long eventId);

    List<EventParticipant> findByUserId(Long userId);

    long countByEventId(Long eventId);

    @Modifying
    @Transactional
    @Query("DELETE FROM EventParticipant ep WHERE ep.event.id = :eventId AND ep.user.id = :userId")
    void deleteByEventIdAndUserId(@Param("eventId") Long eventId, @Param("userId") Long userId);
}

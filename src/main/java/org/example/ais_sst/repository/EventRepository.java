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

    /**
     * Получить данные для отчета по всем мероприятиям (не удаленным).
     * Возвращает Object[] со следующими колонками:
     * [0] eventId (Long)
     * [1] title (String)
     * [2] is_completed (Boolean)
     * [3] is_public (Boolean)
     * [4] is_free_event (Boolean)
     * [5] date_of_event (LocalDate)
     * [6] organizers_count (Integer)
     * [7] participants_count (Integer)
     * [8] performers_count (Integer) - кол-во записей участия через сектора
     * [9] max_participants_count (Integer)
     * [10] max_organizers_count (Integer)
     */
    @Query(value = """
    SELECT 
        e.id as event_id,
        e.title,
        e.is_completed,
        e.is_public,
        e.is_free_event,
        e.date_of_event,
        COALESCE((
            SELECT COUNT(*) 
            FROM event_organizers eo 
            WHERE eo.event_id = e.id 
              AND eo.was_present = true 
              AND eo.is_deleted = false
        ), 0) as organizers_count,
        COALESCE((
            SELECT COUNT(*) 
            FROM event_participants ep 
            WHERE ep.event_id = e.id 
              AND ep.was_present = true 
              AND ep.is_deleted = false
        ), 0) as participants_count,
        COALESCE((
            SELECT COUNT(*) 
            FROM event_participation_records epr
            JOIN event_roles er ON er.id = epr.event_role_id
            WHERE er.event_id = e.id 
              AND epr.was_present = true 
              AND epr.is_deleted = false
        ), 0) as performers_count,
        e.max_participants_count,
        e.max_organizers_count
    FROM events e
    WHERE e.is_deleted = false
    ORDER BY e.date_of_event DESC NULLS LAST, e.created_at DESC
""", nativeQuery = true)
    List<Object[]> findAllEventsForReport();
}
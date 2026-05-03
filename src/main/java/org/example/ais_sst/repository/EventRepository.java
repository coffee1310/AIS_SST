package org.example.ais_sst.repository;

import org.example.ais_sst.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findById(Long id);

    Page<Event> findByIsActiveTrue(Pageable pageable);

    Page<Event> findByEventCreatorId(Long creatorId, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.startTime >= :now ORDER BY e.startTime ASC")
    List<Event> findUpcomingEvents(@Param("now") LocalDateTime now);

    @Query("SELECT e FROM Event e WHERE e.endTime <= :now ORDER BY e.startTime DESC")
    List<Event> findPastEvents(@Param("now") LocalDateTime now);

    boolean existsByIdAndEventCreatorId(Long eventId, Long creatorId);

    // Убираем ORDER BY из запроса - Spring Data JPA добавит сортировку из Pageable
    @Query(value = "SELECT * FROM events e WHERE " +
            "(CAST(:title AS text) IS NULL OR e.title ILIKE CONCAT('%', CAST(:title AS text), '%')) AND " +
            "(CAST(:venue AS text) IS NULL OR e.venue ILIKE CONCAT('%', CAST(:venue AS text), '%')) AND " +
            "(CAST(:dateFrom AS date) IS NULL OR e.date_of_event >= CAST(:dateFrom AS date)) AND " +
            "(CAST(:dateTo AS date) IS NULL OR e.date_of_event <= CAST(:dateTo AS date)) AND " +
            "(CAST(:isPublic AS boolean) IS NULL OR e.is_public = CAST(:isPublic AS boolean)) AND " +
            "(CAST(:isDraft AS boolean) IS NULL OR e.is_draft = CAST(:isDraft AS boolean)) AND " +
            "(CAST(:isCompleted AS boolean) IS NULL OR e.is_completed = CAST(:isCompleted AS boolean)) AND " +
            "(CAST(:isActive AS boolean) IS NULL OR e.is_active = CAST(:isActive AS boolean)) AND " +
            "(CAST(:creatorId AS bigint) IS NULL OR e.event_creator_id = CAST(:creatorId AS bigint))",
            countQuery = "SELECT COUNT(*) FROM events e WHERE " +
                    "(CAST(:title AS text) IS NULL OR e.title ILIKE CONCAT('%', CAST(:title AS text), '%')) AND " +
                    "(CAST(:venue AS text) IS NULL OR e.venue ILIKE CONCAT('%', CAST(:venue AS text), '%')) AND " +
                    "(CAST(:dateFrom AS date) IS NULL OR e.date_of_event >= CAST(:dateFrom AS date)) AND " +
                    "(CAST(:dateTo AS date) IS NULL OR e.date_of_event <= CAST(:dateTo AS date)) AND " +
                    "(CAST(:isPublic AS boolean) IS NULL OR e.is_public = CAST(:isPublic AS boolean)) AND " +
                    "(CAST(:isDraft AS boolean) IS NULL OR e.is_draft = CAST(:isDraft AS boolean)) AND " +
                    "(CAST(:isCompleted AS boolean) IS NULL OR e.is_completed = CAST(:isCompleted AS boolean)) AND " +
                    "(CAST(:isActive AS boolean) IS NULL OR e.is_active = CAST(:isActive AS boolean)) AND " +
                    "(CAST(:creatorId AS bigint) IS NULL OR e.event_creator_id = CAST(:creatorId AS bigint))",
            nativeQuery = true)
    Page<Event> findAllWithFilters(
            @Param("title") String title,
            @Param("venue") String venue,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("isPublic") Boolean isPublic,
            @Param("isDraft") Boolean isDraft,
            @Param("isCompleted") Boolean isCompleted,
            @Param("isActive") Boolean isActive,
            @Param("creatorId") Long creatorId,
            Pageable pageable);
}
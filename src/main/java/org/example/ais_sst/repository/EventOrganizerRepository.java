package org.example.ais_sst.repository;

import org.example.ais_sst.entity.Event;
import org.example.ais_sst.entity.EventOrganizer;
import org.example.ais_sst.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    boolean existsByEventAndUser(Event event, User user);

    boolean existsByUser_IdAndEvent_Id(Long userId, Long eventId);


    List<EventOrganizer> findByEventIdAndWasPresentTrue(Long eventId);

    @Query("SELECT eo FROM EventOrganizer eo WHERE eo.user.id = :userId AND eo.wasPresent = true")
    List<EventOrganizer> findByUserIdAndWasPresentTrue(@Param("userId") Long userId);

    List<EventOrganizer> findByEventIdAndIsDeletedFalse(Long eventId);

    long countByEventIdAndIsDeletedFalse(Long eventId);

    boolean existsByEventIdAndUserIdAndIsDeletedFalse(Long eventId, Long userId);

    Optional<EventOrganizer> findByEventIdAndUserIdAndIsDeletedFalse(Long eventId, Long userId);

    List<EventOrganizer> findByEventIdAndWasPresentTrueAndIsDeletedFalse(Long eventId);

    @Query("SELECT SUM(eo.totalPoints) FROM EventOrganizer eo WHERE eo.event.id = :eventId AND eo.wasPresent = true AND eo.isDeleted = false")
    Long sumTotalPointsByEventId(@Param("eventId") Long eventId);

    @Query("SELECT eo FROM EventOrganizer eo WHERE eo.user.id = :userId AND eo.wasPresent = true AND eo.isDeleted = false")
    List<EventOrganizer> findByUserIdAndWasPresentTrueAndIsDeletedFalse(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE EventOrganizer eo SET eo.isDeleted = true WHERE eo.id = :id")
    void softDeleteById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE EventOrganizer eo SET eo.isDeleted = false WHERE eo.id = :id")
    void restoreById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE EventOrganizer eo SET eo.isDeleted = true WHERE eo.event.id = :eventId")
    void softDeleteAllByEventId(@Param("eventId") Long eventId);

    boolean existsByUserIdAndEventId(Long id, Long eventId);
}
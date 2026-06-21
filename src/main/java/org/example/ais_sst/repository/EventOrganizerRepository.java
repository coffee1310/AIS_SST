package org.example.ais_sst.repository;

import org.example.ais_sst.entity.Event;
import org.example.ais_sst.entity.EventOrganizer;
import org.example.ais_sst.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventOrganizerRepository extends JpaRepository<EventOrganizer, Long>, JpaSpecificationExecutor<EventOrganizer> {

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

    // Метод для фильтрации
    @Query("SELECT eo FROM EventOrganizer eo WHERE eo.event.id = :eventId AND eo.isDeleted = false " +
            "AND (:fullName IS NULL OR LOWER(CONCAT(eo.user.name, ' ', eo.user.surname)) LIKE LOWER(CONCAT('%', :fullName, '%'))) " +
            "AND (:minPoints IS NULL OR eo.totalPoints >= :minPoints) " +
            "AND (:maxPoints IS NULL OR eo.totalPoints <= :maxPoints) " +
            "AND (:wasPresent IS NULL OR eo.wasPresent = :wasPresent)")
    List<EventOrganizer> findByEventIdWithFilters(@Param("eventId") Long eventId,
                                                  @Param("fullName") String fullName,
                                                  @Param("minPoints") Integer minPoints,
                                                  @Param("maxPoints") Integer maxPoints,
                                                  @Param("wasPresent") Boolean wasPresent);

    boolean existsByUser_IdAndEvent_IdAndIsDeleted(Long userId, Long eventId, Boolean isDeleted);

    Optional<EventOrganizer> findByEventIdAndUserIdAndIsDeleted(Long eventId, Long userId, Boolean isDeleted);
}
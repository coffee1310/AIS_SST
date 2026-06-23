package org.example.ais_sst.repository;

import org.example.ais_sst.entity.EventParticipant;
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
public interface EventParticipantsRepository extends JpaRepository<EventParticipant, Long>, JpaSpecificationExecutor<EventParticipant> {
    Optional<EventParticipant> findByEventIdAndUserId(Long eventId, Long userId);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    List<EventParticipant> findByEventId(Long eventId);

    List<EventParticipant> findByUserId(Long userId);

    long countByEventId(Long eventId);

    @Modifying
    @Transactional
    @Query("DELETE FROM EventParticipant ep WHERE ep.event.id = :eventId AND ep.user.id = :userId")
    void deleteByEventIdAndUserId(@Param("eventId") Long eventId, @Param("userId") Long userId);

    List<EventParticipant> findByEventIdAndWasPresentTrue(Long eventId);
    
    @Query("SELECT ep FROM EventParticipant ep WHERE ep.user.id = :userId AND ep.wasPresent = true")
    List<EventParticipant> findByUserIdAndWasPresentTrue(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE EventParticipant ep SET ep.wasPresent = :wasPresent WHERE ep.id = :id")
    void updateWasPresent(@Param("id") Long id, @Param("wasPresent") Boolean wasPresent);

    @Modifying
    @Transactional
    @Query("UPDATE EventParticipant ep SET ep.totalPoints = :points WHERE ep.id = :id")
    void updateTotalPoints(@Param("id") Long id, @Param("points") Integer points);

    List<EventParticipant> findByEventIdAndIsDeletedFalse(Long eventId);

    long countByEventIdAndIsDeletedFalse(Long eventId);

    boolean existsByEventIdAndUserIdAndIsDeletedFalse(Long eventId, Long userId);

    Optional<EventParticipant> findByEventIdAndUserIdAndIsDeletedFalse(Long eventId, Long userId);

    List<EventParticipant> findByEventIdAndWasPresentTrueAndIsDeletedFalse(Long eventId);

    @Query("SELECT SUM(ep.totalPoints) FROM EventParticipant ep WHERE ep.event.id = :eventId AND ep.wasPresent = true AND ep.isDeleted = false")
    Long sumTotalPointsByEventId(@Param("eventId") Long eventId);

    @Query("SELECT ep FROM EventParticipant ep WHERE ep.user.id = :userId AND ep.wasPresent = true AND ep.isDeleted = false")
    List<EventParticipant> findByUserIdAndWasPresentTrueAndIsDeletedFalse(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE EventParticipant ep SET ep.isDeleted = true WHERE ep.id = :id")
    void softDeleteById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE EventParticipant ep SET ep.isDeleted = false WHERE ep.id = :id")
    void restoreById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE EventParticipant ep SET ep.isDeleted = true WHERE ep.event.id = :eventId")
    void softDeleteAllByEventId(@Param("eventId") Long eventId);

    boolean existsByUserIdAndEventId(Long id, Long eventId);

    // Метод для фильтрации
    @Query("SELECT ep FROM EventParticipant ep WHERE ep.event.id = :eventId AND ep.isDeleted = false " +
            "AND (:fullName IS NULL OR LOWER(CONCAT(ep.user.name, ' ', ep.user.surname)) LIKE LOWER(CONCAT('%', :fullName, '%'))) " +
            "AND (:minPoints IS NULL OR ep.totalPoints >= :minPoints) " +
            "AND (:maxPoints IS NULL OR ep.totalPoints <= :maxPoints) " +
            "AND (:wasPresent IS NULL OR ep.wasPresent = :wasPresent)")
    List<EventParticipant> findByEventIdWithFilters(@Param("eventId") Long eventId,
                                                    @Param("fullName") String fullName,
                                                    @Param("minPoints") Integer minPoints,
                                                    @Param("maxPoints") Integer maxPoints,
                                                    @Param("wasPresent") Boolean wasPresent);

    Optional<EventParticipant> findByEventIdAndUserIdAndIsDeletedTrue(Long eventId, Long userId);

    Long countByEventIdAndIsDeletedFalseAndIsDeletedFalse(Long eventId);

    @Query("""
    SELECT ep FROM EventParticipant ep
    WHERE ep.user.id = :userId
    AND ep.wasPresent = true
    AND ep.isDeleted = false
    AND ep.event.isCompleted = true
    AND ep.event.isDeleted = false
""")
    List<EventParticipant> findByUserIdAndWasPresentTrueAndIsDeletedFalseAndEventIsCompletedTrue(@Param("userId") Long userId);
}

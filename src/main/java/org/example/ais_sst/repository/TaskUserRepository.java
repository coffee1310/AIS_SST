package org.example.ais_sst.repository;

import org.example.ais_sst.entity.TaskUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskUserRepository extends JpaRepository<TaskUser, Long> {


    List<TaskUser> findByTaskId(Long taskId);

    List<TaskUser> findByUserId(Long userId);

    Optional<TaskUser> findByTaskIdAndUserId(Long taskId, Long userId);

    @Query("SELECT tu FROM TaskUser tu WHERE tu.task.id = :taskId AND tu.isDeleted = false")
    List<TaskUser> findActiveByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT tu FROM TaskUser tu WHERE tu.task.id = :taskId AND tu.isDeleted = true")
    List<TaskUser> findDeletedByTaskId(@Param("taskId") Long taskId);

    @Modifying
    @Query("UPDATE TaskUser tu SET tu.isDeleted = true WHERE tu.task.id = :taskId")
    void softDeleteByTaskId(@Param("taskId") Long taskId);

    @Modifying
    @Query("UPDATE TaskUser tu SET tu.isDeleted = true WHERE tu.task.id = :taskId AND tu.user.id IN :userIds")
    void softDeleteByTaskIdAndUserIds(@Param("taskId") Long taskId, @Param("userIds") List<Long> userIds);

    boolean existsByTaskIdAndUserIdAndIsDeletedFalse(Long taskId, Long userId);

    @Query("SELECT COUNT(tu) FROM TaskUser tu WHERE tu.task.id = :taskId AND tu.isDeleted = false")
    long countByTaskIdAndIsDeletedFalse(@Param("taskId") Long taskId);

    @Query("SELECT tu FROM TaskUser tu WHERE tu.task.id = :taskId AND tu.user.id = :userId AND tu.isDeleted = false")
    Optional<TaskUser> findActiveByTaskIdAndUserId(@Param("taskId") Long taskId, @Param("userId") Long userId);

    // Метод для поиска всех невыполненных назначений по задаче
    @Query("SELECT tu FROM TaskUser tu WHERE tu.task.id = :taskId AND tu.isCompleted = false AND tu.isDeleted = false")
    List<TaskUser> findActiveIncompleteByTaskId(@Param("taskId") Long taskId);

    @Query("""
    SELECT tu FROM TaskUser tu
    WHERE tu.user.id = :userId
    AND tu.isCompleted = true
    AND tu.isDeleted = false
    AND tu.task.isCompleted = true
    AND tu.task.isDeleted = false
""")
    List<TaskUser> findByUserIdAndIsCompletedTrueAndIsDeletedFalseAndTaskIsCompletedTrue(@Param("userId") Long userId);
}
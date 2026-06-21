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


    List<TaskUser> findByTaskId(Integer taskId);

    List<TaskUser> findByUserId(Long userId);

    Optional<TaskUser> findByTaskIdAndUserId(Integer taskId, Long userId);

    @Query("SELECT tu FROM TaskUser tu WHERE tu.task.id = :taskId AND tu.isDeleted = false")
    List<TaskUser> findActiveByTaskId(@Param("taskId") Integer taskId);

    @Query("SELECT tu FROM TaskUser tu WHERE tu.task.id = :taskId AND tu.isDeleted = true")
    List<TaskUser> findDeletedByTaskId(@Param("taskId") Integer taskId);

    @Modifying
    @Query("UPDATE TaskUser tu SET tu.isDeleted = true WHERE tu.task.id = :taskId")
    void softDeleteByTaskId(@Param("taskId") Integer taskId);

    @Modifying
    @Query("UPDATE TaskUser tu SET tu.isDeleted = true WHERE tu.task.id = :taskId AND tu.user.id IN :userIds")
    void softDeleteByTaskIdAndUserIds(@Param("taskId") Integer taskId, @Param("userIds") List<Long> userIds);

    boolean existsByTaskIdAndUserIdAndIsDeletedFalse(Integer taskId, Long userId);

    @Query("SELECT COUNT(tu) FROM TaskUser tu WHERE tu.task.id = :taskId AND tu.isDeleted = false")
    long countByTaskIdAndIsDeletedFalse(@Param("taskId") Integer taskId);

    @Query("SELECT tu FROM TaskUser tu WHERE tu.task.id = :taskId AND tu.user.id = :userId AND tu.isDeleted = false")
    Optional<TaskUser> findActiveByTaskIdAndUserId(@Param("taskId") Integer taskId, @Param("userId") Long userId);

    // Метод для поиска всех невыполненных назначений по задаче
    @Query("SELECT tu FROM TaskUser tu WHERE tu.task.id = :taskId AND tu.isCompleted = false AND tu.isDeleted = false")
    List<TaskUser> findActiveIncompleteByTaskId(@Param("taskId") Integer taskId);
}
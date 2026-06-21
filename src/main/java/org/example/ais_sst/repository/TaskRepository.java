package org.example.ais_sst.repository;

import org.example.ais_sst.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Integer>, JpaSpecificationExecutor<Task> {

    List<Task> findByIsDeletedFalse();

    List<Task> findByIsDeletedFalseOrderByDeadlineAsc();

    List<Task> findByIsDeletedFalseAndIsCompletedFalse();

    List<Task> findByIsDeletedFalseAndIsCompletedTrue();

    List<Task> findByIsDeletedFalseAndDeadlineBefore(Instant deadline);

    List<Task> findByIsDeletedFalseAndDeadlineIsNull();

    // Методы для поиска по создателю
    List<Task> findByCreatorIdAndIsDeletedFalse(Long creatorId);

    List<Task> findByCreatorIdAndIsDeletedFalseOrderByCreatedAtDesc(Long creatorId);

    @Query("SELECT t FROM Task t JOIN t.taskUsers tu WHERE tu.user.id = :userId AND t.isDeleted = false")
    List<Task> findTasksByUserId(@Param("userId") Long userId);

    @Query("SELECT t FROM Task t JOIN t.taskUsers tu WHERE tu.user.id = :userId AND t.isDeleted = false AND t.isCompleted = false")
    List<Task> findActiveTasksByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(tu) FROM TaskUser tu WHERE tu.task.id = :taskId AND tu.isDeleted = false")
    long countAssignedUsers(@Param("taskId") Integer taskId);

}
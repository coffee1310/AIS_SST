package org.example.ais_sst.repository;

import org.example.ais_sst.entity.TaskRequest;
import org.example.ais_sst.entity.enums.TaskRequestStatus;
import org.example.ais_sst.specification.TaskRequestSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRequestRepository extends JpaRepository<TaskRequest, Integer>, JpaSpecificationExecutor<TaskRequest> {

    List<TaskRequest> findByTaskIdAndIsDeletedFalse(Integer taskId);

    List<TaskRequest> findByStudentIdAndIsDeletedFalse(Long studentId);

    Optional<TaskRequest> findByTaskIdAndStudentIdAndIsDeletedFalse(Integer taskId, Long studentId);

    @Query("SELECT tr FROM TaskRequest tr WHERE tr.task.id = :taskId AND tr.status = :status AND tr.isDeleted = false")
    List<TaskRequest> findByTaskIdAndStatus(@Param("taskId") Integer taskId, @Param("status") TaskRequestStatus status);

    @Query("SELECT tr FROM TaskRequest tr WHERE tr.student.id = :studentId AND tr.status = :status AND tr.isDeleted = false")
    List<TaskRequest> findByStudentIdAndStatus(@Param("studentId") Long studentId, @Param("status") TaskRequestStatus status);

    @Query("SELECT COUNT(tr) FROM TaskRequest tr WHERE tr.task.id = :taskId AND tr.status = :status AND tr.isDeleted = false")
    long countByTaskIdAndStatus(@Param("taskId") Integer taskId, @Param("status") TaskRequestStatus status);

    @Modifying
    @Query("UPDATE TaskRequest tr SET tr.isDeleted = true WHERE tr.id = :requestId")
    void softDelete(@Param("requestId") Integer requestId);

    boolean existsByTaskIdAndStudentIdAndStatusAndIsDeletedFalse(
            Integer taskId, Long studentId, TaskRequestStatus status);
}
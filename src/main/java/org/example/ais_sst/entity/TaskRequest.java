package org.example.ais_sst.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.example.ais_sst.entity.converter.TaskRequestStatusConverter;
import org.example.ais_sst.entity.enums.TaskRequestStatus;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "task_requests",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_task_request_student_task",
                        columnNames = {"task_id", "student_id"})
        })
public class TaskRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Convert(converter = TaskRequestStatusConverter.class)
    @ColumnDefault("'На рассмотрении'")
    @Column(name = "status", columnDefinition = "varchar(50) not null")
    @Builder.Default
    private TaskRequestStatus status = TaskRequestStatus.НА_РАССМОТРЕНИИ;

    @CreationTimestamp
    @Column(name = "filing_date", nullable = false, updatable = false)
    private Instant filingDate;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
}
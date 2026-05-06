package org.example.ais_sst.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "applications_for_the_role", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "event_role_id"}, name = "uk_student_event_role")
})
public class ApplicationsForTheRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_role_id", nullable = false)
    private EventRole eventRole;

    @Column(name = "is_reserve", nullable = false)
    @Builder.Default
    private Boolean isReserve = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "role_application_statuses", nullable = false)
    @Builder.Default
    private RoleApplicationStatuses status = RoleApplicationStatuses.НА_РАССМОТРЕНИИ;

    @Column(name = "rejection_reason", length = Integer.MAX_VALUE)
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
package org.example.ais_sst.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "applications_for_the_role")
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
    @JoinColumn(name = "role_id", nullable = false)
    private GlobalEventRole role;

    @Column(name = "status", columnDefinition = "role_application_statuses not null")
    private RoleApplicationStatuses status;

    @Column(name = "date_of_application")
    private LocalDate dateOfApplication;
}
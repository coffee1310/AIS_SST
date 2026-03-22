package org.example.ais_sst.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "internal_projects")
public class InternalProject {
    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 128)
    @NotNull
    @Column(name = "title", nullable = false, length = 128)
    private String title;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conducting_classes_id", nullable = false)
    private ConductingClass conductingClasses;

    @NotNull
    @Column(name = "\"time\"", nullable = false)
    private LocalTime time;

    @Column(name = "photo")
    private byte[] photo;

    @NotNull
    @Column(name = "reference_to_position", nullable = false, length = Integer.MAX_VALUE)
    private String referenceToPosition;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "is_draft", nullable = false)
    private Boolean isDraft = false;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

}
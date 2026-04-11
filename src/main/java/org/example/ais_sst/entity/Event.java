package org.example.ais_sst.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 128)
    @NotNull
    @Column(name = "title", nullable = false, length = 128)
    private String title;

    @NotNull
    @Column(name = "date_of_event", nullable = false)
    private LocalDate dateOfEvent;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Size(max = 256)
    @NotNull
    @Column(name = "venue", nullable = false, length = 256)
    private String venue;

    @Column(name = "photo")
    private byte[] photo;

    @NotNull
    @Column(name = "reference_to_position", nullable = false, length = Integer.MAX_VALUE)
    private String referenceToPosition;

    @ColumnDefault("true")
    @Column(name = "is_public")
    private Boolean isPublic;

    @ColumnDefault("true")
    @Column(name = "is_draft")
    private Boolean isDraft;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @ColumnDefault("false")
    @Column(name = "is_completed")
    private Boolean isCompleted;

    @ColumnDefault("false")
    @Column(name = "is_deleted")
    private Boolean isDeleted;


}
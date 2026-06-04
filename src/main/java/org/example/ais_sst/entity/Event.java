package org.example.ais_sst.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "photo")
    private String photo;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "venue")  // Изменено с location на venue
    private String venue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_creator_id", nullable = false)
    private User eventCreator;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_public")  // Добавлено поле is_public
    @Builder.Default
    private Boolean isPublic = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "reference_to_position")  // Добавлено поле
    private String referenceToPosition;

    @Column(name = "date_of_event")  // Добавлено поле
    private LocalDate dateOfEvent;

    @Column(name = "max_participants_count", nullable = false)
    @Builder.Default
    private Integer maxParticipantsCount = 0;

    @Column(name = "is_draft")
    @Builder.Default
    private Boolean isDraft = true;

    @Column(name = "is_completed")
    @Builder.Default
    private Boolean isCompleted = false;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EventOrganizer> organizers = new ArrayList<>();

    @OneToMany(mappedBy = "event", fetch = FetchType.LAZY)
    @Builder.Default
    private List<EventRole> eventRoles = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EventParticipant> participants = new ArrayList<>();

    @Column(name = "is_free_event", nullable = false)
    @Builder.Default
    private Boolean isFreeEvent = true;  // true - можно участвовать, false - нельзя участвовать

    @Column(name = "max_organizers_count", nullable = false)
    @Builder.Default
    private Integer maxOrganizersCount = 0;
}
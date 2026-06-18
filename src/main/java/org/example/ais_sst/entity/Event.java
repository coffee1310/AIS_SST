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
import java.util.stream.Collectors;

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

    @Column(name = "venue")
    private String venue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_creator_id", nullable = false)
    private User eventCreator;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "reference_to_position")
    private String referenceToPosition;

    @Column(name = "date_of_event")
    private LocalDate dateOfEvent;

    @Column(name = "max_participants_count", nullable = false)
    @Builder.Default
    private Integer maxParticipantsCount = 0;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

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
    private Boolean isFreeEvent = true;

    @Column(name = "max_organizers_count", nullable = false)
    @Builder.Default
    private Integer maxOrganizersCount = 0;

    // УДАЛЯЕМ поле sector_id
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "sector_id")
    // private Sector sector;

    // ДОБАВЛЯЕМ связь с секторами через связующую таблицу
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EventSector> eventSectors = new ArrayList<>();

    // Вспомогательные методы для работы с секторами
    public void addSector(Sector sector) {
        EventSector eventSector = EventSector.builder()
                .event(this)
                .sector(sector)
                .build();
        eventSectors.add(eventSector);
    }

    public void removeSector(Sector sector) {
        eventSectors.removeIf(es -> es.getSector().equals(sector));
    }

    public List<Sector> getSectors() {
        return eventSectors.stream()
                .map(EventSector::getSector)
                .collect(Collectors.toList());
    }

    public List<Long> getSectorIds() {
        return eventSectors.stream()
                .map(es -> es.getSector().getId())
                .collect(Collectors.toList());
    }
}
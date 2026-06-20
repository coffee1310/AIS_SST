package org.example.ais_sst.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_organizers",
        uniqueConstraints = @UniqueConstraint(name = "uk_event_organizers_event_user",
                columnNames = {"event_id", "user_id"}))
public class EventOrganizer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @Column(name = "total_points", nullable = false)
    @Builder.Default
    private Integer totalPoints = 5;  // По умолчанию 5 баллов

    @Column(name = "was_present", nullable = false)
    @Builder.Default
    private Boolean wasPresent = false;
}
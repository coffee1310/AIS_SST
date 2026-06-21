package org.example.ais_sst.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_participation_records",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_participation_record",
                        columnNames = {"sector_participant_id", "event_role_id"})
        })
@ToString(exclude = {"sectorParticipant", "eventRole"})  // Исключаем циклические ссылки
public class EventParticipationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_participant_id", nullable = false)
    private SectorParticipant sectorParticipant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_role_id", nullable = false)
    private EventRole eventRole;

    @Column(name = "was_present", nullable = false)
    @Builder.Default
    private Boolean wasPresent = false;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints ;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
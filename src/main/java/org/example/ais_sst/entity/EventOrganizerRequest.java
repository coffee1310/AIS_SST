package org.example.ais_sst.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ais_sst.entity.converter.RoleApplicationStatusesConverter;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_organizer_requests",
        uniqueConstraints = @UniqueConstraint(name = "uk_event_organizers_event_user",
                columnNames = {"event_id", "user_id"}))
public class EventOrganizerRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Convert(converter = RoleApplicationStatusesConverter.class)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RoleApplicationStatuses status = RoleApplicationStatuses.НА_РАССМОТРЕНИИ;
}
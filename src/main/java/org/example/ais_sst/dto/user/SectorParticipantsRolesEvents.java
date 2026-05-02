package org.example.ais_sst.dto.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ais_sst.entity.GlobalEventRole;
import org.example.ais_sst.entity.SectorParticipant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sector_participants_roles_events")
public class SectorParticipantsRolesEvents {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_participant_id", nullable = false)
    private SectorParticipant sectorParticipant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roles_as_the_event_id", nullable = false)
    private GlobalEventRole rolesAsTheEvent;

    @Builder.Default
    @Column(name = "is_need_registration", nullable = false)
    private Boolean isNeedRegistration = true;

    @Column(name = "max_applications")
    private Integer maxApplications;

    @Builder.Default
    @Column(name = "reserve_count", nullable = false)
    private Integer reserveCount = 0;
}
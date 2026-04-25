package org.example.ais_sst.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.example.ais_sst.entity.converter.SectorParticipantStatusesConverter;
import org.example.ais_sst.entity.enums.SectorIntroductionStatus;
import org.example.ais_sst.entity.enums.SectorParticipantStatuses;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Builder
@Table(name = "sector_participants")
@AllArgsConstructor
@NoArgsConstructor
public class SectorParticipant {
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
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @Builder.Default
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate = LocalDate.now();

    @Convert(converter = SectorParticipantStatusesConverter.class)
    @ColumnDefault("'Активный'")
    @Column(name = "status", columnDefinition = "sector_participant_statuses not null")
    private SectorParticipantStatuses status = SectorParticipantStatuses.Ожидание;
}
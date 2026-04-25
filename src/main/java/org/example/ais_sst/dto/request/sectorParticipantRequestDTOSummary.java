package org.example.ais_sst.dto.request;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.example.ais_sst.annotation.ValidUserId;
import org.example.ais_sst.entity.Sector;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.entity.enums.SectorParticipantStatuses;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;

public class sectorParticipantRequestDTOSummary {

    private Long id;

    @NotNull
    @NotBlank
    private String student;

    @NotNull
    @NotBlank
    private String sector;

    @PastOrPresent
    private LocalDate entryDate;

    private SectorParticipantStatuses status = SectorParticipantStatuses.Активный;
}

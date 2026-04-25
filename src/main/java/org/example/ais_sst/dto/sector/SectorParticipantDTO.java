package org.example.ais_sst.dto.sector;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Builder;
import lombok.Data;
import org.example.ais_sst.annotation.ValidUserId;
import org.example.ais_sst.entity.Sector;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.entity.enums.SectorParticipantStatuses;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;

@Data
@Builder
public class SectorParticipantDTO {

    private Long id;

    @NotNull
    @ValidUserId
    private Long student_id;

    @NotNull
    private Long sector_id;

    @PastOrPresent
    private LocalDate entryDate;

    @NotNull
    private SectorParticipantStatuses status;
}

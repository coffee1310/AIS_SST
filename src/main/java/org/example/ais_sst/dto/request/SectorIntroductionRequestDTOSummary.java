package org.example.ais_sst.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.example.ais_sst.annotation.ValidUserId;
import org.example.ais_sst.entity.enums.SectorIntroductionStatus;

@Data
@Builder
public class SectorIntroductionRequestDTOSummary {

    private Long id;

    @NotNull
    private String sector;

    @NotNull
    private SectorIntroductionStatus status;
}

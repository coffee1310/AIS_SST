package org.example.ais_sst.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.example.ais_sst.annotation.ValidUserId;
import org.example.ais_sst.entity.enums.SectorIntroductionStatus;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SectorIntroductionRequestDTO {

    private Long id;

    @NotNull
    private Long sector_id;

    @ValidUserId
    private Long user_id;

    private SectorIntroductionStatus status;

}

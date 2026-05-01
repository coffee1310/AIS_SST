package org.example.ais_sst.dto.sector;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ais_sst.annotation.ValidSectorName;
import org.example.ais_sst.annotation.ValidUserId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectorDTO {

    private Long id;

    @Size(max = 128)
    @NotNull
    @ValidSectorName
    private String title;

    private String description;

    @Builder.Default
    private Boolean isActive = true;

    private String photo;
}
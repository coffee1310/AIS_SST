package org.example.ais_sst.dto.sector;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SectorUpdateDTO {

    private Long id;

    @Size(max = 128)
    private String title;

    private String description;

    private Boolean isActive;

    private String photo;

    private List<Long> coordinatorIds;
}
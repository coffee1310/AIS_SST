package org.example.ais_sst.dto.sector;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectorUpdateDTO {

    private Long id;

    @Size(max = 128)
    private String title;

    private String description;

    private Boolean isActive;

    private String photo;

    private List<Long> coordinatorIds;
}
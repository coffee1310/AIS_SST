package org.example.ais_sst.dto.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSectorResponseDTO {
    private Long id;
    private String title;
    private String description;
}
package org.example.ais_sst.dto.events;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventRoleSummaryDTO {
    private Long roleId;
    private String roleName;
    private String responsibleSectorName;
    private Integer mainCount;
    private Integer reserveCount;
}

package org.example.ais_sst.dto.event_roles;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRoleUpdateDTO {
    private Long eventId;
    private Long globalEventRoleId;
    private Integer capacity;
    private Integer reserveCapacity;
    private Boolean deleted;
}
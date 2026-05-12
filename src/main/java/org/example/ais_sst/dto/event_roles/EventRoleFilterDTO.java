package org.example.ais_sst.dto.event_roles;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EventRoleFilterDTO {
    private Long id;
    private Long eventId;
    private Long globalEventRoleId;
    private Boolean deleted;
    private LocalDateTime deadlineFrom;
    private LocalDateTime deadlineTo;
}
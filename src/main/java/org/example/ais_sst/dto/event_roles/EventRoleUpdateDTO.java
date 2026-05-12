package org.example.ais_sst.dto.event_roles;

import jakarta.validation.constraints.Future;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRoleUpdateDTO {
    private Long eventId;
    private Long globalEventRoleId;
    private Integer capacity;
    private Integer reserveCapacity;
    private String description;

    @Future(message = "Дедлайн должен быть в будущем")
    private LocalDateTime deadline;

    private Boolean deleted;
}
package org.example.ais_sst.dto.event_roles;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRoleCreateDTO {

    @NotNull(message = "ID мероприятия обязателен")
    private Long eventId;

    @NotNull(message = "ID глобальной роли обязателен")
    private Long globalEventRoleId;

    private Integer capacity;

    private Integer reserveCapacity;
}
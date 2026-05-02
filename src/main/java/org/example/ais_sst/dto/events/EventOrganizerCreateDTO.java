package org.example.ais_sst.dto.events;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventOrganizerCreateDTO {

    @NotNull(message = "ID мероприятия обязателен")
    private Long eventId;

    @NotNull(message = "ID пользователя обязателен")
    private Long userId;
}
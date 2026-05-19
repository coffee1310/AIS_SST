package org.example.ais_sst.dto.event_roles_application;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO для обновления статуса заявки на организатора")
public class EventOrganizerRequestUpdateDTO {

    @Schema(description = "Статус заявки", example = "APPROVED", required = true)
    private RoleApplicationStatuses status;
}
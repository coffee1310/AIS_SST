package org.example.ais_sst.dto.event_roles_application;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleApplicationCreateDTO {

    private String description;  // Пожелания пользователя (опционально)
}
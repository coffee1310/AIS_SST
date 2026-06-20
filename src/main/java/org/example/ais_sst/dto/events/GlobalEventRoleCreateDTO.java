package org.example.ais_sst.dto.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalEventRoleCreateDTO {

    @NotBlank(message = "Название роли не может быть пустым")
    @Size(max = 64, message = "Название роли не должно превышать 64 символа")
    private String title;

    private String description;

    @NotNull
    private Long sector_id;

    @Builder.Default
    private Boolean isDefaultRole = false;

    @NotNull
    private Long defaultPoints;
}
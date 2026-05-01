package org.example.ais_sst.dto.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolesAsTheEventCreateDTO {

    @NotBlank(message = "Название роли не может быть пустым")
    @Size(max = 64, message = "Название роли не должно превышать 64 символа")
    private String title;

    private String description;

    @Builder.Default
    private Boolean isDefaultRole = false;
}
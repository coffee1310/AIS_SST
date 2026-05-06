package org.example.ais_sst.dto.event_roles_application;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleApplicationRejectDTO {

    @NotBlank(message = "Причина отклонения обязательна")
    private String rejectionReason;
}
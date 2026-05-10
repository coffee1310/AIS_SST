package org.example.ais_sst.dto.account_request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountCreatingRequestRejectDTO {
    @NotBlank(message = "Укажите причину отказа")
    @Size(max = 500, message = "Причина отказа не должна превышать 500 символов")
    private String rejectionReason;
}

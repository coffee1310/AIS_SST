package org.example.ais_sst.dto.account_request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import org.example.ais_sst.annotation.ValidUserEmailExist;
import org.example.ais_sst.annotation.ValidUserEmailFormat;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class AccountCreatingRequestsSummaryDTO {
    private Long id;

    @Size(max = 64)
    @NotNull
    private String name;

    @Size(max = 64)
    @NotNull
    private String surname;

    @Size(max = 64)
    private String patronymic;

    @NotNull
    private String gender;

    @NotNull
    private LocalDate dateOfBirth;

    @NotNull
    private Short courseNumber;

    @NotNull
    private Long speciality_id;

    @NotNull
    private Long group_id;

    @NotNull
    private Integer studentIdNumber;

    @Size(max = 32)
    @NotNull
    @ValidUserEmailExist
    @ValidUserEmailFormat
    private String studentEmail;

    @Size(max = 256)
    private String additionalEmail;

    @Size(max = 16)
    private String phoneNumber;

    @Size(max = 256)
    private String vkLink;

    private byte[] photo;

    List<Long> social_statuses_id;

    @Size(max = 256)
    @NotNull
    private String password;

    private String reason_for_refusal;
}

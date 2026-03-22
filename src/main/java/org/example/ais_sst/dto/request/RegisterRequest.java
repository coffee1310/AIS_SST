package org.example.ais_sst.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String surname;

    private String patronymic;

    @NotBlank
    private String gender;

    @NotBlank
    private String dateOfBirth;

    @NotBlank
    @Email
    private String studentEmail;

    @NotBlank
    private String phoneNumber;

    @NotBlank
    private String password;

    private Long roleId = 1L; // ID обычной роли по умолчанию
}
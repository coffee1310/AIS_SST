package org.example.ais_sst.dto.account_request;

import lombok.Builder;
import lombok.Data;
import org.example.ais_sst.entity.enums.AccountCreatingRequestStatus;

import java.time.LocalDate;

@Data
@Builder
public class AccountCreatingRequestFilterDTO {
    private Long id;
    private String name;
    private String surname;
    private String patronymic;
    private String gender;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String studentEmail;
    private String phoneNumber;
    private Integer studentIdNumber;
    private Short courseNumber;
    private AccountCreatingRequestStatus status;
    private Long groupId;
    private Long specialityId;
    private Boolean hasPhoto;  // Есть ли фото
}
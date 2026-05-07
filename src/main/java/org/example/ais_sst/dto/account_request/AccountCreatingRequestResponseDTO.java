package org.example.ais_sst.dto.account_request;


import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Builder;
import lombok.Data;
import org.example.ais_sst.entity.AccountCreatingRequest;
import org.example.ais_sst.entity.enums.AccountCreatingRequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class AccountCreatingRequestResponseDTO {
    private Long id;
    private String name;
    private String surname;
    private String patronymic;
    private String gender;
    private LocalDate dateOfBirth;
    private String studentEmail;
    private String phoneNumber;
    private Integer studentIdNumber;
    private Short courseNumber;
    private AccountCreatingRequestStatus status;
    private String reasonForRefusal;
    private Long groupId;
    private String groupName;
    private Long specialityId;
    private String specialityName;
    private String photo;
    private String vkLink;
    private String additionalEmail;

    private LocalDateTime createdAt;   // Добавлено
    private LocalDateTime updatedAt;   // Добавлено

    private List<String> socialStatuses;  // Добавлено поле для социальных статусов

    public static AccountCreatingRequestResponseDTO from(AccountCreatingRequest request) {
        return AccountCreatingRequestResponseDTO.builder()
                .id(request.getId())
                .name(request.getName())
                .surname(request.getSurname())
                .patronymic(request.getPatronymic())
                .gender(request.getGender() != null ? request.getGender().name() : null)
                .dateOfBirth(request.getDateOfBirth())
                .studentEmail(request.getStudentEmail())
                .phoneNumber(request.getPhoneNumber())
                .studentIdNumber(request.getStudentIdNumber())
                .courseNumber(request.getCourseNumber())
                .status(request.getStatus())
                .reasonForRefusal(request.getReasonForRefusal())
                .groupId(request.getGroup().getId())
                .groupName(request.getGroup() != null ? request.getGroup().getTitle() : null)
                .specialityId(request.getSpeciality() != null ? request.getSpeciality().getId() : null)
                .specialityName(request.getSpeciality() != null ? request.getSpeciality().getTitle() : null)
                .vkLink(request.getVkLink())
                .additionalEmail(request.getAdditionalEmail())
                .build();
    }
}

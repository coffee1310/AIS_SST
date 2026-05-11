package org.example.ais_sst.dto.user;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserResponseDTO {
    private Long id;
    private String name;
    private String surname;
    private String patronymic;
    private String gender;
    private LocalDate dateOfBirth;
    private Short courseNumber;
    private Integer studentIdNumber;
    private String studentEmail;
    private String additionalEmail;
    private String phoneNumber;
    private String vkLink;
    private String photo;
    private String role;
    private Long coordinatorSectorId;      // Добавлено поле
    private String coordinatorSectorTitle; // Добавлено поле
    private Boolean isActive;
    private Boolean isBanned;
    private Long groupId;
    private String groupName;
    private Long specialityId;
    private String specialityName;
    private String specialityShortTitle;
    private List<String> socialStatuses;  // Добавлено поле для социальных статусов
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
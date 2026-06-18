package org.example.ais_sst.dto.user;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import org.example.ais_sst.entity.Group;
import org.example.ais_sst.entity.Role;
import org.example.ais_sst.entity.SocialStatus;
import org.example.ais_sst.entity.Speciality;
import org.example.ais_sst.entity.enums.Gender;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Builder
@Data
public class UserProfileInfoDTO {

    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String surname;

    private String patronymic;

    @Min(0)
    @NotNull
    private Integer eventsCount;

    @Min(0)
    @NotNull
    private Integer pointsCount;

    @Min(1)
    @NotNull
    private Integer ratingPosition;

    @Past
    private LocalDate dateOfBirth;

    @Min(1)
    @Max(4)
    private Short courseNumber;

    private String specialityTitle;

    private String groupTitle;

    @NotBlank
    private String studentEmail;

    private String additionalEmail;

    @NotBlank
    private String phoneNumber;

    private Gender gender;

    private String vkLink;

    private String photo;

    private String roleTitle;

    private String coordinatorSector;

    private List<String> socialStatuses;

    private Long coordinatorSectorId;

    private String coordinatorSectorTitle;

    private String shortSpecialityTitle;

    // НОВОЕ ПОЛЕ: список секторов, в которых состоит пользователь
    private List<String> userSectors;  // Список названий секторов


    public void validate(Validator validator) {
        Set<ConstraintViolation<UserProfileInfoDTO>> violations = validator.validate(this);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
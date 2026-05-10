package org.example.ais_sst.dto.user;

import java.time.LocalDate;

public interface UserProjectionDTO {
    Long getId();
    String getName();
    String getSurname();
    String getPatronymic();
    String getGender();
    LocalDate getDateOfBirth();
    Short getCourseNumber();
    Integer getStudentIdNumber();
    String getStudentEmail();
    String getAdditionalEmail();
    String getPhoneNumber();
    String getVkLink();
    Boolean getIsActive();
    Boolean getIsBanned();
    String getRole();
    Long getGroupId();
    String getGroupName();
    Long getSpecialityId();
    String getSpecialityName();
}
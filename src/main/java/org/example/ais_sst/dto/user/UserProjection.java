package org.example.ais_sst.dto.user;

import java.time.LocalDate;

public interface UserProjection {
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
    Long getRoleId();
    String getRoleTitle();
    Long getGroupId();
    String getGroupTitle();
    Long getSpecialityId();
    String getSpecialityTitle();
    // НЕТ поля photo!
}
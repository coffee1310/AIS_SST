package org.example.ais_sst.dto.sector;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ais_sst.annotation.ValidSectorName;
import org.example.ais_sst.annotation.ValidUserId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectorDTO {

    private Long id;

    @Size(max = 128)
    @NotNull
    @ValidSectorName
    private String title;

    private String description;

    @Builder.Default
    private Boolean isActive = true;

    private String photo;

    // Информация о координаторе
    private Long coordinatorId;
    private String coordinatorFullName;      // ФИО координатора
    private String coordinatorName;
    private String coordinatorSurname;
    private String coordinatorPatronymic;
    private String coordinatorPhoto;          // фото координатора (Base64)
    private Short coordinatorCourseNumber;    // курс координатора
    private String coordinatorGroupTitle;     // группа координатора
    private String coordinatorSpecialityTitle; // специальность координатора
}
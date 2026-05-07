package org.example.ais_sst.dto.sector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectorParticipantResponseDTO {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentSurname;
    private String studentPatronymic;
    private String studentEmail;
    private String studentPhoto;
    private Short studentCourseNumber;
    private String studentGroupTitle;
    private String studentSpecialityTitle;
    private LocalDate entryDate;
    private String status;
    private Boolean isCoordinator;
}
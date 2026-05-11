package org.example.ais_sst.mapper;


import org.example.ais_sst.dto.sector.SectorParticipantDTO;
import org.example.ais_sst.dto.sector.SectorParticipantResponseDTO;
import org.example.ais_sst.entity.Sector;
import org.example.ais_sst.entity.SectorParticipant;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.service.userService.UserPhotoService;
import org.example.ais_sst.utils.ImageUtil;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface SectorParticipantMapper {

    @Mapping(source = "student.id", target = "student_id")
    @Mapping(source = "sector.id", target = "sector_id")
    SectorParticipantDTO toSectorParticipantDTO(SectorParticipant sectorParticipant);

    // Из DTO в Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "student", source = "student_id", qualifiedByName = "mapStudent")
    @Mapping(target = "sector", source = "sector_id", qualifiedByName = "mapSector")
    SectorParticipant toEntity(SectorParticipantDTO sectorParticipantDTO);

    @Named("mapStudent")
    default User mapStudent(Long studentId) {
        if (studentId == null) return null;
        User user = new User();
        user.setId(studentId);
        return user;
    }

    @Named("mapSector")
    default Sector mapSector(Long sectorId) {
        if (sectorId == null) return null;
        Sector sector = new Sector();
        sector.setId(sectorId);
        return sector;
    }

    // Метод с @Context для использования в сервисах
    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentName", source = "student.name")
    @Mapping(target = "studentSurname", source = "student.surname")
    @Mapping(target = "studentPatronymic", source = "student.patronymic")
    @Mapping(target = "studentEmail", source = "student.studentEmail")
    @Mapping(target = "studentPhoto", expression = "java(getPhotoAsBase64(entity.getStudent().getPathToPhoto(), userPhotoService))")
    @Mapping(target = "studentCourseNumber", source = "student.courseNumber")
    @Mapping(target = "studentGroupTitle", source = "student.group.title")
    @Mapping(target = "studentSpecialityTitle", source = "student.speciality.title")
    @Mapping(target = "entryDate", source = "entryDate")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "isCoordinator", source = "isCoordinator")
    SectorParticipantResponseDTO toResponseDto(SectorParticipant entity,
                                               @Context UserPhotoService userPhotoService);

    // Метод без @Context для обратной совместимости (будет использовать null)
    default SectorParticipantResponseDTO toResponseDto(SectorParticipant entity) {
        return toResponseDto(entity, null);
    }

    default String getPhotoAsBase64(String photoPath, UserPhotoService userPhotoService) {
        if (photoPath == null || photoPath.isEmpty() || userPhotoService == null) {
            return null;
        }
        return userPhotoService.getPhotoAsBase64(photoPath);
    }
}
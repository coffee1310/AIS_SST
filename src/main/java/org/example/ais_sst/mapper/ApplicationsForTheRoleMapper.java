package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.event_roles_application.RoleApplicationResponseDTO;
import org.example.ais_sst.entity.ApplicationsForTheRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApplicationsForTheRoleMapper {

//    @Mapping(target = "studentId", source = "student.id")
//    @Mapping(target = "studentName", source = "student.name")
//    @Mapping(target = "studentSurname", source = "student.surname")
//    @Mapping(target = "studentPatronymic", source = "student.patronymic")
//    @Mapping(target = "studentEmail", source = "student.studentEmail")
//    @Mapping(target = "eventRoleId", source = "eventRole.id")
//    @Mapping(target = "eventRoleName", source = "eventRole.globalEventRole.name")
//    @Mapping(target = "eventId", source = "eventRole.event.id")
//    @Mapping(target = "eventTitle", source = "eventRole.event.title")
//    RoleApplicationResponseDTO toResponseDto(ApplicationsForTheRole entity);
}
package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.event_roles_application.RoleApplicationResponseDTO;
import org.example.ais_sst.entity.ApplicationsForTheRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleApplicationMapper {

    @Mapping(target = "sectorParticipantId", source = "sectorParticipant.id")
    @Mapping(target = "studentName", source = "sectorParticipant.student.name")
    @Mapping(target = "studentSurname", source = "sectorParticipant.student.surname")
    @Mapping(target = "studentPatronymic", source = "sectorParticipant.student.patronymic")
    @Mapping(target = "studentEmail", source = "sectorParticipant.student.studentEmail")
    @Mapping(target = "sectorTitle", source = "sectorParticipant.sector.title")  // укажите правильный source
    @Mapping(target = "eventRoleId", source = "eventRole.id")
    @Mapping(target = "eventRoleName", source = "eventRole.globalEventRole.title")
    @Mapping(target = "eventId", source = "eventRole.event.id")
    @Mapping(target = "eventTitle", source = "eventRole.event.title")
    RoleApplicationResponseDTO toResponseDto(ApplicationsForTheRole entity);
}
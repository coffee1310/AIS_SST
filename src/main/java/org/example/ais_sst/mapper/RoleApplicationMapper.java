package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.event_roles_application.EventOrganizerRequestResponseDTO;
import org.example.ais_sst.dto.event_roles_application.RoleApplicationResponseDTO;
import org.example.ais_sst.entity.ApplicationsForTheRole;
import org.example.ais_sst.entity.EventOrganizerRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.example.ais_sst.dto.event_roles_application.EventOrganizerRequestResponseDTO;
import org.example.ais_sst.dto.event_roles_application.RoleApplicationResponseDTO;
import org.example.ais_sst.entity.ApplicationsForTheRole;
import org.example.ais_sst.entity.EventOrganizerRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RoleApplicationMapper {

    @Mapping(target = "sectorParticipantId", source = "sectorParticipant.id")
    @Mapping(target = "studentName", source = "sectorParticipant.student.name")
    @Mapping(target = "studentSurname", source = "sectorParticipant.student.surname")
    @Mapping(target = "studentPatronymic", source = "sectorParticipant.student.patronymic")
    @Mapping(target = "studentEmail", source = "sectorParticipant.student.studentEmail")
    @Mapping(target = "sectorTitle", source = "sectorParticipant.sector.title")
    @Mapping(target = "eventRoleId", source = "eventRole.id")
    @Mapping(target = "eventRoleName", source = "eventRole.globalEventRole.title")
    @Mapping(target = "eventId", source = "eventRole.event.id")
    @Mapping(target = "eventTitle", source = "eventRole.event.title")
    @Mapping(target = "description", source = "description")
    RoleApplicationResponseDTO toResponseDto(ApplicationsForTheRole entity);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.name")
    @Mapping(target = "userSurname", source = "user.surname")
    @Mapping(target = "userEmail", source = "user.studentEmail")
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventTitle", source = "event.title")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "status", source = "status")
    EventOrganizerRequestResponseDTO toResponseDto(EventOrganizerRequest entity);
}
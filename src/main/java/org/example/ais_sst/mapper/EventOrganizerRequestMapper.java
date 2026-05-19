package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.event_roles_application.EventOrganizerRequestResponseDTO;
import org.example.ais_sst.dto.event_roles_application.EventOrganizerRequestUpdateDTO;
import org.example.ais_sst.entity.EventOrganizerRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EventOrganizerRequestMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.name", target = "userName")
    @Mapping(source = "user.surname", target = "userSurname")
    @Mapping(source = "user.studentEmail", target = "userEmail")  // Используем studentEmail
    @Mapping(source = "event.id", target = "eventId")
    @Mapping(source = "event.title", target = "eventTitle")  // Используем title, если есть в сущности Event
    EventOrganizerRequestResponseDTO toResponseDto(EventOrganizerRequest request);

    // Метод для обновления из DTO
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", source = "status")
    void updateEntityFromDto(EventOrganizerRequestUpdateDTO dto, @MappingTarget EventOrganizerRequest entity);
}
package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.events.EventOrganizerCreateDTO;
import org.example.ais_sst.dto.events.EventOrganizerResponseDTO;
import org.example.ais_sst.entity.EventOrganizer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventOrganizerMapper {

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventTitle", source = "event.title")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.name")
    @Mapping(target = "userSurname", source = "user.surname")
    @Mapping(target = "userPatronymic", source = "user.patronymic")
    @Mapping(target = "userEmail", source = "user.studentEmail")
    @Mapping(target = "userPhoto", expression = "java(org.example.ais_sst.utils.ImageUtil.encodeToBase64(entity.getUser().getPhoto()))")
    @Mapping(target = "addedAt", source = "addedAt")
    EventOrganizerResponseDTO toResponseDto(EventOrganizer entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "addedAt", ignore = true)
    EventOrganizer toEntity(EventOrganizerCreateDTO dto);
}
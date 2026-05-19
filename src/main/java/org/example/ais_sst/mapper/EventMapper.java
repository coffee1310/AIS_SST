package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.events.EventCreateDTO;
import org.example.ais_sst.dto.events.EventResponseDTO;
import org.example.ais_sst.dto.events.EventUpdateDTO;
import org.example.ais_sst.entity.Event;
import org.example.ais_sst.service.eventService.EventPhotoService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Mapper(componentModel = "spring", uses = {EventOrganizerMapper.class})
public interface EventMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "dateOfEvent", source = "dateOfEvent")
    @Mapping(target = "startTime", source = "startTime")
    @Mapping(target = "endTime", source = "endTime")
    @Mapping(target = "venue", source = "venue")
    @Mapping(target = "referenceToPosition", source = "referenceToPosition")
    @Mapping(target = "isPublic", source = "isPublic")
    @Mapping(target = "isDraft", source = "isDraft")
    @Mapping(target = "isActive", source = "isActive")
    @Mapping(target = "isCompleted", source = "isCompleted")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "organizers", source = "organizers")
    @Mapping(target = "eventCreatorId", source = "eventCreator.id")
    @Mapping(target = "eventCreatorName", source = "eventCreator.name")
    @Mapping(target = "eventCreatorSurname", source = "eventCreator.surname")
    @Mapping(target = "photo", ignore = true)
    EventResponseDTO toResponseDto(Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventCreator", ignore = true)
    @Mapping(target = "organizers", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "isDraft", source = "isDraft")
    @Mapping(target = "isCompleted", constant = "false")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "dateOfEvent", source = "dateOfEvent")
    @Mapping(target = "startTime", source = "startTime")
    @Mapping(target = "endTime", source = "endTime")
    @Mapping(target = "venue", source = "venue")
    @Mapping(target = "referenceToPosition", source = "referenceToPosition")
    @Mapping(target = "isPublic", source = "isPublic")
    @Mapping(target = "photo", source = "photo")
    Event toEntity(EventCreateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventCreator", ignore = true)
    @Mapping(target = "organizers", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "title", source = "title")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "dateOfEvent", source = "dateOfEvent")
    @Mapping(target = "startTime", source = "startTime")
    @Mapping(target = "endTime", source = "endTime")
    @Mapping(target = "venue", source = "venue")
    @Mapping(target = "referenceToPosition", source = "referenceToPosition")
    @Mapping(target = "isPublic", source = "isPublic")
    @Mapping(target = "isDraft", source = "isDraft")
    @Mapping(target = "isActive", source = "isActive")
    @Mapping(target = "isCompleted", ignore = true)
    @Mapping(target = "photo", source = "photo")
    Event toEntity(EventUpdateDTO dto);
}
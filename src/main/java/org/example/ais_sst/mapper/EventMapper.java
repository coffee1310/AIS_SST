package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.events.EventCreateDTO;
import org.example.ais_sst.dto.events.EventResponseDTO;
import org.example.ais_sst.dto.events.EventUpdateDTO;
import org.example.ais_sst.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {EventOrganizerMapper.class})
public interface EventMapper {

    @Mapping(target = "organizers", source = "organizers")
    @Mapping(target = "eventCreatorId", source = "eventCreator.id")
    @Mapping(target = "eventCreatorName", source = "eventCreator.name")
    @Mapping(target = "eventCreatorSurname", source = "eventCreator.surname")
    @Mapping(target = "venue", source = "venue")
    @Mapping(target = "isPublic", source = "isPublic")
    @Mapping(target = "dateOfEvent", source = "dateOfEvent")  // Добавлено
    @Mapping(target = "referenceToPosition", source = "referenceToPosition")
    EventResponseDTO toResponseDto(Event entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventCreator", ignore = true)
    @Mapping(target = "organizers", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "isPublic", source = "isPublic")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "venue", source = "venue")
    @Mapping(target = "dateOfEvent", source = "dateOfEvent")  // Добавлено
    @Mapping(target = "referenceToPosition", source = "referenceToPosition")
    Event toEntity(EventCreateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventCreator", ignore = true)
    @Mapping(target = "organizers", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "venue", source = "venue")
    @Mapping(target = "isPublic", source = "isPublic")
    @Mapping(target = "dateOfEvent", source = "dateOfEvent")  // Добавлено
    @Mapping(target = "referenceToPosition", source = "referenceToPosition")
    Event toEntity(EventUpdateDTO dto);
}
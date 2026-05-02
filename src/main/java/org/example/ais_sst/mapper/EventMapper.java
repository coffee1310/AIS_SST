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
    EventResponseDTO toResponseDto(Event entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventCreator", ignore = true)
    @Mapping(target = "organizers", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Event toEntity(EventCreateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventCreator", ignore = true)
    @Mapping(target = "organizers", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Event toEntity(EventUpdateDTO dto);
}
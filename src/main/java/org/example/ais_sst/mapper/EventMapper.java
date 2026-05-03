package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.events.EventCreateDTO;
import org.example.ais_sst.dto.events.EventResponseDTO;
import org.example.ais_sst.dto.events.EventUpdateDTO;
import org.example.ais_sst.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Mapper(componentModel = "spring", uses = {EventOrganizerMapper.class})
public interface EventMapper {

    @Mapping(target = "organizers", source = "organizers")
    @Mapping(target = "eventCreatorId", source = "eventCreator.id")
    @Mapping(target = "eventCreatorName", source = "eventCreator.name")
    @Mapping(target = "eventCreatorSurname", source = "eventCreator.surname")
    @Mapping(target = "venue", source = "venue")
    @Mapping(target = "isPublic", source = "isPublic")
    @Mapping(target = "dateOfEvent", source = "dateOfEvent")
    @Mapping(target = "referenceToPosition", source = "referenceToPosition")
    @Mapping(target = "startTime", source = "startTime")
    @Mapping(target = "endTime", source = "endTime")
    EventResponseDTO toResponseDto(Event entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventCreator", ignore = true)
    @Mapping(target = "organizers", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "isPublic", source = "isPublic")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "venue", source = "venue")
    @Mapping(target = "dateOfEvent", source = "dateOfEvent")
    @Mapping(target = "referenceToPosition", source = "referenceToPosition")
    @Mapping(target = "startTime", source = "startTime", qualifiedByName = "localTimeToLocalDateTime")
    @Mapping(target = "endTime", source = "endTime", qualifiedByName = "localTimeToLocalDateTime")
    Event toEntity(EventCreateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventCreator", ignore = true)
    @Mapping(target = "organizers", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "venue", source = "venue")
    @Mapping(target = "isPublic", source = "isPublic")
    @Mapping(target = "dateOfEvent", source = "dateOfEvent")
    @Mapping(target = "referenceToPosition", source = "referenceToPosition")
    @Mapping(target = "startTime", source = "startTime", qualifiedByName = "localTimeToLocalDateTime")
    @Mapping(target = "endTime", source = "endTime", qualifiedByName = "localTimeToLocalDateTime")
    Event toEntity(EventUpdateDTO dto);

    @Named("localTimeToLocalDateTime")
    default LocalDateTime localTimeToLocalDateTime(LocalTime time) {
        if (time == null) {
            return null;
        }
        // Комбинируем с текущей датой или с dateOfEvent
        return time.atDate(LocalDate.now());
    }

    @Named("localDateTimeToLocalTime")
    default LocalTime localDateTimeToLocalTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalTime();
    }
}
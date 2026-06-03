package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.event_participant.EventParticipantResponseDTO;
import org.example.ais_sst.entity.EventParticipant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventParticipantMapper {

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventTitle", source = "event.title")
    @Mapping(target = "eventDescription", source = "event.description")
    @Mapping(target = "eventDateOfEvent", source = "event.dateOfEvent")
    @Mapping(target = "eventStartTime", source = "event.startTime")
    @Mapping(target = "eventEndTime", source = "event.endTime")
    @Mapping(target = "eventVenue", source = "event.venue")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "studentName", source = "user.name")
    @Mapping(target = "studentSurname", source = "user.surname")
    @Mapping(target = "studentPatronymic", source = "user.patronymic")
    @Mapping(target = "studentEmail", source = "user.studentEmail")
    @Mapping(target = "joinedAt", source = "createdAt")
    EventParticipantResponseDTO toResponseDto(EventParticipant entity);
}
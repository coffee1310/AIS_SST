package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.events.EventOrganizerCreateDTO;
import org.example.ais_sst.dto.events.EventOrganizerResponseDTO;
import org.example.ais_sst.entity.EventOrganizer;
import org.example.ais_sst.service.userService.UserPhotoService;
import org.mapstruct.Context;
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
    @Mapping(target = "userPhoto", expression = "java(getPhotoAsBase64(entity.getUser().getPathToPhoto(), userPhotoService))")
    @Mapping(target = "addedAt", source = "addedAt")
    EventOrganizerResponseDTO toResponseDto(EventOrganizer entity,
                                            @Context UserPhotoService userPhotoService);

    // Метод без @Context для обратной совместимости
    default EventOrganizerResponseDTO toResponseDto(EventOrganizer entity) {
        return toResponseDto(entity, null);
    }

    default String getPhotoAsBase64(String photoPath, UserPhotoService userPhotoService) {
        if (photoPath == null || photoPath.isEmpty() || userPhotoService == null) {
            return null;
        }
        return userPhotoService.getPhotoAsBase64(photoPath);
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "addedAt", ignore = true)
    EventOrganizer toEntity(EventOrganizerCreateDTO dto);
}
package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.user.UserProjection;
import org.example.ais_sst.dto.user.UserResponseDTO;
import org.example.ais_sst.dto.user.UserSummaryDTO;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.service.userService.UserPhotoService;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "role.id", target = "role_id")
    @Mapping(source = "group.id", target = "group_id")
    @Mapping(source = "speciality.id", target = "speciality_id")
    @Mapping(target = "accountCreatingRequest_id", source = "accountCreatingRequest.id")
    UserSummaryDTO toDto(User user);

    @Mapping(target = "role", source = "role.title")
    @Mapping(target = "groupId", source = "group.id")
    @Mapping(target = "groupName", source = "group.title")
    @Mapping(target = "specialityId", source = "speciality.id")
    @Mapping(target = "specialityName", source = "speciality.title")
    @Mapping(target = "photo", expression = "java(getPhotoAsBase64(user.getPathToPhoto(), userPhotoService))")
    @Mapping(target = "gender", expression = "java(user.getGender() != null ? user.getGender().name() : null)")
    UserResponseDTO toResponseDto(User user, @Context UserPhotoService userPhotoService);

    default String getPhotoAsBase64(String photoPath, UserPhotoService userPhotoService) {
        if (photoPath == null || photoPath.isEmpty() || userPhotoService == null) {
            return null;
        }
        return userPhotoService.getPhotoAsBase64(photoPath);
    }

    // Метод без @Context для обратной совместимости
    default UserResponseDTO toResponseDto(User user) {
        return toResponseDto(user, null);
    }

    @Mapping(target = "role", source = "roleTitle")
    @Mapping(target = "groupId", source = "groupId")
    @Mapping(target = "groupName", source = "groupTitle")
    @Mapping(target = "specialityId", source = "specialityId")
    @Mapping(target = "specialityName", source = "specialityTitle")
    @Mapping(target = "photo", ignore = true)
    UserResponseDTO fromProjection(UserProjection projection);

}
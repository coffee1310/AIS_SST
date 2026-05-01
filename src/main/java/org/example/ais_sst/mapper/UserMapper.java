package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.user.UserProjection;
import org.example.ais_sst.dto.user.UserResponseDTO;
import org.example.ais_sst.dto.user.UserSummaryDTO;
import org.example.ais_sst.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "role.id", target = "role_id")  // role.id → role_id
    @Mapping(source = "group.id", target = "group_id")  // group.id → group_id
    @Mapping(source = "speciality.id", target = "speciality_id")  // speciality.id → speciality_id
    @Mapping(target = "accountCreatingRequest_id", source = "accountCreatingRequest.id")  // если нужно
        // group.name и speciality.name - их нет в DTO, поэтому убираем
    UserSummaryDTO toDto(User user);

    @Mapping(target = "role", source = "role.title")
    @Mapping(target = "groupId", source = "group.id")
    @Mapping(target = "groupName", source = "group.title")
    @Mapping(target = "specialityId", source = "speciality.id")
    @Mapping(target = "specialityName", source = "speciality.title")
    @Mapping(target = "photo", expression = "java(org.example.ais_sst.utils.ImageUtil.encodeToBase64(user.getPhoto()))")
    @Mapping(target = "gender", expression = "java(user.getGender() != null ? user.getGender().name() : null)")
    UserResponseDTO toResponseDto(User user);

    @Mapping(target = "role", source = "roleTitle")
    @Mapping(target = "groupId", source = "groupId")
    @Mapping(target = "groupName", source = "groupTitle")
    @Mapping(target = "specialityId", source = "specialityId")
    @Mapping(target = "specialityName", source = "specialityTitle")
    @Mapping(target = "photo", ignore = true)  // В проекции нет фото
    UserResponseDTO fromProjection(UserProjection projection);
}
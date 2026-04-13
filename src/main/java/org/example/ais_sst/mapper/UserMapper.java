package org.example.ais_sst.mapper;

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
}
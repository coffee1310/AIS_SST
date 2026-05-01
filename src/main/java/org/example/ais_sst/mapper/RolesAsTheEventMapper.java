package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.events.RolesAsTheEventCreateDTO;
import org.example.ais_sst.dto.events.RolesAsTheEventDTO;
import org.example.ais_sst.dto.events.RolesAsTheEventUpdateDTO;
import org.example.ais_sst.entity.RolesAsTheEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RolesAsTheEventMapper {

    RolesAsTheEventDTO toDto(RolesAsTheEvent entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    RolesAsTheEvent toEntity(RolesAsTheEventCreateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    RolesAsTheEvent toEntity(RolesAsTheEventUpdateDTO dto);
}
package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.events.GlobalEventRoleCreateDTO;
import org.example.ais_sst.dto.events.GlobalEventRoleDTO;
import org.example.ais_sst.dto.events.GlobalEventRoleUpdateDTO;
import org.example.ais_sst.entity.GlobalEventRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GlobalEventRolesMapper {

    @Mapping(target = "sectorId", source = "sector.id")
    @Mapping(target = "sectorTitle", source = "sector.title")
    @Mapping(target = "isDeleted", source = "isDeleted")
    GlobalEventRoleDTO toDto(GlobalEventRole entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "sector", ignore = true)
    GlobalEventRole toEntity(GlobalEventRoleCreateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "sector", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    GlobalEventRole toEntity(GlobalEventRoleUpdateDTO dto);
}
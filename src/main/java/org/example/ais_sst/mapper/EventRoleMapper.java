package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.event_roles.EventRoleCreateDTO;
import org.example.ais_sst.dto.event_roles.EventRoleResponseDTO;
import org.example.ais_sst.dto.event_roles.EventRoleUpdateDTO;
import org.example.ais_sst.entity.EventRole;
import org.example.ais_sst.entity.GlobalEventRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface EventRoleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "globalEventRole", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "capacity", source = "capacity")
    @Mapping(target = "reserveCapacity", source = "reserveCapacity")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "deadline", source = "deadline")
    EventRole toEntity(EventRoleCreateDTO dto);

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventTitle", source = "event.title")
    @Mapping(target = "globalEventRoleId", source = "globalEventRole.id")
    @Mapping(target = "globalEventRoleTitle", source = "globalEventRole.title")
    @Mapping(target = "deleted", source = "deleted")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "deadline", source = "deadline")
    EventRoleResponseDTO toResponseDto(EventRole entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "globalEventRole", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "capacity", source = "capacity")
    @Mapping(target = "reserveCapacity", source = "reserveCapacity")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "deadline", source = "deadline")
    @Mapping(target = "deleted", source = "deleted")
    void updateEntity(EventRoleUpdateDTO dto, @MappingTarget EventRole entity);

    @Named("getGlobalEventRoleTitle")
    default String getGlobalEventRoleTitle(GlobalEventRole globalEventRole) {
        return globalEventRole != null ? globalEventRole.getTitle() : null;
    }
}
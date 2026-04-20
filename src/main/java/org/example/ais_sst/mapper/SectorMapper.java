package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.sector.SectorDTO;
import org.example.ais_sst.entity.Sector;
import org.example.ais_sst.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface SectorMapper {

    @Mapping(source = "currentCoordinator.id", target = "currentCoordinator_id")
    SectorDTO toSectorDTO(Sector sector);

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "currentCoordinator_id", target = "currentCoordinator", qualifiedByName = "mapCurrentCoordinator")
    Sector toEntity(SectorDTO sectorDTO);

    @Named("mapCurrentCoordinator")
    default User mapCurrentCoordinator(Long currentCoordinator_id) {
        if (currentCoordinator_id == null) return null;
        return User.builder().id(currentCoordinator_id).build();
    }
}

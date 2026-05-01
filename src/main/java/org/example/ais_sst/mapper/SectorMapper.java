package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.sector.SectorDTO;
import org.example.ais_sst.dto.sector.SectorWithUserStatusDTO;
import org.example.ais_sst.entity.Sector;
import org.example.ais_sst.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface SectorMapper {

    @Mapping(source = "currentCoordinator.id", target = "currentCoordinator_id")
    @Mapping(target = "photo", expression = "java(org.example.ais_sst.utils.ImageUtil.encodeToBase64(sector.getPhoto()))")
    SectorDTO toSectorDTO(Sector sector);

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "currentCoordinator_id", target = "currentCoordinator", qualifiedByName = "mapCurrentCoordinator")
    @Mapping(target = "photo", expression = "java(org.example.ais_sst.utils.ImageUtil.decodeFromBase64(sectorDTO.getPhoto()))")
    Sector toEntity(SectorDTO sectorDTO);

    @Mapping(target = "id", source = "sector.id")
    @Mapping(target = "title", source = "sector.title")
    @Mapping(target = "description", source = "sector.description")
    @Mapping(target = "isParticipant", source = "isParticipant")
    @Mapping(target = "hasActiveRequest", source = "hasActiveRequest")
    @Mapping(target = "participantCount", source = "participantCount")
    SectorWithUserStatusDTO toDtoWithStatus(
            Sector sector,
            Boolean isParticipant,
            Boolean hasActiveRequest,
            Integer participantCount
    );

    @Named("toDtoWithStatusDefault")
    default SectorWithUserStatusDTO toDtoWithStatusDefault(
            Sector sector,
            Boolean isParticipant,
            Boolean hasActiveRequest) {
        return toDtoWithStatus(sector, isParticipant, hasActiveRequest, 0);
    }

    @Named("mapCurrentCoordinator")
    default User mapCurrentCoordinator(Long currentCoordinator_id) {
        if (currentCoordinator_id == null) return null;
        return User.builder().id(currentCoordinator_id).build();
    }
}

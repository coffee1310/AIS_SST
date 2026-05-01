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

    // Удален @Mapping для currentCoordinator_id
    @Mapping(target = "photo", expression = "java(org.example.ais_sst.utils.ImageUtil.encodeToBase64(sector.getPhoto()))")
    SectorDTO toSectorDTO(Sector sector);

    // Удален @Mapping для currentCoordinator_id
    @Mapping(target = "photo", expression = "java(org.example.ais_sst.utils.ImageUtil.decodeFromBase64(sectorDTO.getPhoto()))")
    Sector toEntity(SectorDTO sectorDTO);

    @Mapping(target = "id", source = "sector.id")
    @Mapping(target = "title", source = "sector.title")
    @Mapping(target = "description", source = "sector.description")
    @Mapping(target = "isParticipant", source = "isParticipant")
    @Mapping(target = "hasActiveRequest", source = "hasActiveRequest")
    @Mapping(target = "participantCount", source = "participantCount")
    SectorWithUserStatusDTO toDtoWithStatus(Sector sector, Boolean isParticipant, Boolean hasActiveRequest, Integer participantCount);
}
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

    @Mapping(target = "photo", expression = "java(org.example.ais_sst.utils.ImageUtil.encodeToBase64(entity.getPhoto()))")
    @Mapping(target = "coordinatorId", ignore = true)
    @Mapping(target = "coordinatorFullName", ignore = true)
    @Mapping(target = "coordinatorName", ignore = true)
    @Mapping(target = "coordinatorSurname", ignore = true)
    @Mapping(target = "coordinatorPatronymic", ignore = true)
    @Mapping(target = "coordinatorPhoto", ignore = true)
    @Mapping(target = "coordinatorCourseNumber", ignore = true)
    @Mapping(target = "coordinatorGroupTitle", ignore = true)
    @Mapping(target = "coordinatorSpecialityTitle", ignore = true)
    SectorDTO toSectorDTO(Sector entity);

    @Mapping(target = "photo", expression = "java(org.example.ais_sst.utils.ImageUtil.decodeFromBase64(dto.getPhoto()))")
    Sector toEntity(SectorDTO dto);

    @Mapping(target = "id", source = "sector.id")
    @Mapping(target = "title", source = "sector.title")
    @Mapping(target = "description", source = "sector.description")
    @Mapping(target = "isParticipant", source = "isParticipant")
    @Mapping(target = "hasActiveRequest", source = "hasActiveRequest")
    @Mapping(target = "participantCount", source = "participantCount")
    @Mapping(target = "photo", expression = "java(org.example.ais_sst.utils.ImageUtil.encodeToBase64(sector.getPhoto()))")
    SectorWithUserStatusDTO toDtoWithStatus(Sector sector, Boolean isParticipant, Boolean hasActiveRequest, Integer participantCount);
}
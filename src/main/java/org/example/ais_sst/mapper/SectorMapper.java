package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.sector.SectorDTO;
import org.example.ais_sst.dto.sector.SectorWithUserStatusDTO;
import org.example.ais_sst.entity.Sector;
import org.example.ais_sst.service.sectorService.SectorPhotoService;
import org.mapstruct.Builder;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface SectorMapper {

    @Mapping(target = "photo", expression = "java(getPhotoAsBase64(entity.getPathToPhoto(), sectorPhotoService))")
    @Mapping(target = "coordinatorId", ignore = true)
    @Mapping(target = "coordinatorFullName", ignore = true)
    @Mapping(target = "coordinatorName", ignore = true)
    @Mapping(target = "coordinatorSurname", ignore = true)
    @Mapping(target = "coordinatorPatronymic", ignore = true)
    @Mapping(target = "coordinatorPhoto", ignore = true)
    @Mapping(target = "coordinatorCourseNumber", ignore = true)
    @Mapping(target = "coordinatorGroupTitle", ignore = true)
    @Mapping(target = "coordinatorSpecialityTitle", ignore = true)
    SectorDTO toSectorDTO(Sector entity, @Context SectorPhotoService sectorPhotoService);

    default String getPhotoAsBase64(String photoPath, SectorPhotoService sectorPhotoService) {
        if (photoPath == null || photoPath.isEmpty() || sectorPhotoService == null) {
            return null;
        }
        return sectorPhotoService.getPhotoAsBase64(photoPath);
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "pathToPhoto", ignore = true)
    Sector toEntity(SectorDTO dto);

    @Mapping(target = "id", source = "sector.id")
    @Mapping(target = "title", source = "sector.title")
    @Mapping(target = "description", source = "sector.description")
    @Mapping(target = "isParticipant", source = "isParticipant")
    @Mapping(target = "hasActiveRequest", source = "hasActiveRequest")
    @Mapping(target = "participantCount", source = "participantCount")
    @Mapping(target = "photo", expression = "java(getPhotoAsBase64(sector.getPathToPhoto(), sectorPhotoService))")
    SectorWithUserStatusDTO toDtoWithStatus(Sector sector, Boolean isParticipant, Boolean hasActiveRequest, Integer participantCount,
                                            @Context SectorPhotoService sectorPhotoService);
}
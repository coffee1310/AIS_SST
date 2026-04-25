package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.request.SectorIntroductionRequestDTO;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTOSummary;
import org.example.ais_sst.entity.Sector;
import org.example.ais_sst.entity.SectorIntroductionRequest;
import org.example.ais_sst.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface SectorIntroductionRequestMapper {

    @Mapping(source = "user.id", target = "user_id")
    @Mapping(source = "sector.id", target = "sector_id")
    SectorIntroductionRequestDTO toSectorIntroductionRequestDTO(SectorIntroductionRequest sectorIntroductionRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "user_id", qualifiedByName = "MapUser")
    @Mapping(target = "sector", source = "sector_id", qualifiedByName = "MapSector")
    SectorIntroductionRequest toEntity(SectorIntroductionRequestDTO sectorIntroductionRequest);

    @Mapping(target = "sector", source = "sector.title")
    @Mapping(target = "status", source = "status")
    SectorIntroductionRequestDTOSummary toSummary(SectorIntroductionRequest sectorIntroductionRequest);


    @Named("MapUser")
    default User MapUser(Long user_id) {
        if (user_id == null) return null;
        return User.builder().id(user_id).build();
    }

    @Named("MapSector")
    default Sector MapSector(Long sector_id) {
        if (sector_id == null) return null;
        return Sector.builder().id(sector_id).build();
    }
}

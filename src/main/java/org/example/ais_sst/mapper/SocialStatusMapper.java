package org.example.ais_sst.mapper;

import org.example.ais_sst.entity.SocialStatus;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SocialStatusMapper {

    SocialStatus toDto(SocialStatus socialStatus);

//    List<SocialStatusDTO> toDtoList(List<SocialStatus> socialStatuses);
//
//    SocialStatus toEntity(SocialStatusDTO socialStatusDTO);

}

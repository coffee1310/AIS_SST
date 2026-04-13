package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.account_request.AccountCreatingRequestResponseDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestsSummaryDTO;
import org.example.ais_sst.entity.AccountCreatingRequest;
import org.example.ais_sst.entity.Group;
import org.example.ais_sst.entity.Speciality;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface AccountCreatingRequestMapper {

    @Mapping(source = "group.id", target = "group_id")
    @Mapping(source = "speciality.id", target = "speciality_id")
    @Mapping(target = "social_statuses_id", ignore = true)
    AccountCreatingRequestsSummaryDTO toSummaryDto(AccountCreatingRequest request);

    @Mapping(source = "group_id", target = "group", qualifiedByName = "mapGroup")
    @Mapping(source = "speciality_id", target = "speciality", qualifiedByName = "mapSpeciality")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "reasonForRefusal", ignore = true)
    AccountCreatingRequest toEntity(AccountCreatingRequestsSummaryDTO dto);

    AccountCreatingRequestResponseDTO toResponseDto(AccountCreatingRequest request);

    @Named("mapGroup")
    default Group mapGroup(Long groupId) {
        if (groupId == null) return null;
        return Group.builder().id(groupId).build();
    }

    @Named("mapSpeciality")
    default Speciality mapSpeciality(Long specialityId) {
        if (specialityId == null) return null;
        return Speciality.builder().id(specialityId).build();
    }
}
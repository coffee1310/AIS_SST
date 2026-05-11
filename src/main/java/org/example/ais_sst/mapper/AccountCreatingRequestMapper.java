package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.account_request.AccountCreatingRequestResponseDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestsSummaryDTO;
import org.example.ais_sst.entity.AccountCreatingRequest;
import org.example.ais_sst.entity.Group;
import org.example.ais_sst.entity.Speciality;
import org.example.ais_sst.service.accountCreatingRequestsService.AccountRequestPhotoService;
import org.example.ais_sst.utils.ImageUtil;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", imports = {ImageUtil.class})
public interface AccountCreatingRequestMapper {

    // Маппинг DTO -> Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "speciality", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "pathToPhoto", ignore = true)  // В DTO нет этого поля
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "reasonForRefusal", ignore = true)
    @Mapping(target = "vkLink", source = "vkLink")
    @Mapping(target = "additionalEmail", source = "additionalEmail")
    AccountCreatingRequest toEntity(AccountCreatingRequestsSummaryDTO dto);

    // Маппинг Entity -> DTO (для ответа при создании)
    @Mapping(target = "group_id", source = "group.id")
    @Mapping(target = "speciality_id", source = "speciality.id")
    @Mapping(target = "social_statuses_id", ignore = true)
    @Mapping(target = "photo", ignore = true)  // В DTO есть photo, но он будет установлен отдельно
    @Mapping(target = "id", source = "id")
    AccountCreatingRequestsSummaryDTO toDto(AccountCreatingRequest entity);

    // Маппинг Entity -> ResponseDTO
    @Mapping(target = "specialityId", source = "speciality.id")
    @Mapping(target = "specialityName", source = "speciality.title")
    @Mapping(target = "groupId", source = "group.id")
    @Mapping(target = "groupName", source = "group.title")
    @Mapping(target = "photo", expression = "java(getPhotoAsBase64(entity.getPathToPhoto(), accountRequestPhotoService))")
    @Mapping(target = "socialStatuses", ignore = true)
    @Mapping(target = "vkLink", source = "vkLink")
    @Mapping(target = "gender", source = "gender")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "reasonForRefusal", source = "reasonForRefusal")
    AccountCreatingRequestResponseDTO toResponseDto(AccountCreatingRequest entity,
                                                    @Context AccountRequestPhotoService accountRequestPhotoService);

    default String getPhotoAsBase64(String photoPath, AccountRequestPhotoService service) {
        if (photoPath == null || photoPath.isEmpty()) {
            return null;
        }
        return service.getPhotoAsBase64(photoPath);
    }

    @Named("decodePhoto")
    default byte[] decodePhoto(String base64Photo) {
        return ImageUtil.decodeFromBase64(base64Photo);
    }

    @Named("encodePhoto")
    default String encodePhoto(byte[] photo) {
        return ImageUtil.encodeToBase64(photo);
    }
}
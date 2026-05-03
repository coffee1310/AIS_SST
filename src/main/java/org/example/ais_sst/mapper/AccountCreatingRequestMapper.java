package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.account_request.AccountCreatingRequestResponseDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestsSummaryDTO;
import org.example.ais_sst.entity.AccountCreatingRequest;
import org.example.ais_sst.entity.Group;
import org.example.ais_sst.entity.Speciality;
import org.example.ais_sst.utils.ImageUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", imports = {ImageUtil.class})
public interface AccountCreatingRequestMapper {

    @Mapping(target = "photo", expression = "java(ImageUtil.decodeFromBase64(dto.getPhoto()))")
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "speciality", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "id", ignore = true)
    AccountCreatingRequest toEntity(AccountCreatingRequestsSummaryDTO dto);

    @Mapping(target = "photo", expression = "java(ImageUtil.encodeToBase64(entity.getPhoto()))")
    AccountCreatingRequestsSummaryDTO toDto(AccountCreatingRequest entity);

    @Mapping(target = "specialityId", source = "speciality.id")
    @Mapping(target = "specialityName", source = "speciality.title")
    @Mapping(target = "groupId", source = "group.id")
    @Mapping(target = "groupName", source = "group.title")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "reasonForRefusal", source = "reasonForRefusal")
    @Mapping(target = "photo", expression = "java(ImageUtil.encodeToBase64(entity.getPhoto()))")  // Добавлено преобразование фото
    @Mapping(target = "gender", source = "gender")
    AccountCreatingRequestResponseDTO toResponseDto(AccountCreatingRequest entity);

    @Named("decodePhoto")
    default byte[] decodePhoto(String base64Photo) {
        return ImageUtil.decodeFromBase64(base64Photo);
    }

    @Named("encodePhoto")
    default String encodePhoto(byte[] photo) {
        return ImageUtil.encodeToBase64(photo);
    }
}
package org.example.ais_sst.service.userService;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.user.UserFilterDTO;
import org.example.ais_sst.dto.user.UserResponseDTO;
import org.example.ais_sst.exception.UserDoesNotExistException;
import lombok.RequiredArgsConstructor;
import org.example.ais_sst.dto.user.UserProfileInfoDTO;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.mapper.UserMapper;
import org.example.ais_sst.repository.UserRepository;
import org.example.ais_sst.utils.ImageUtil;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserServiceImpl {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserProfileInfoDTO getUserBasicInfo(Long userId) {
        UserProfileInfoDTO userProfileInfoDTO;
        Optional<User> user_optional = userRepository.findUserById(userId);

        if (!user_optional.isPresent()) throw new UserDoesNotExistException("Пользователь не найден");

        User user = user_optional.get();

        // Конвертируем фото в Base64
        String photoBase64 = user.getPhoto() != null && user.getPhoto().length > 0
                ? ImageUtil.encodeToBase64(user.getPhoto())
                : null;

        userProfileInfoDTO = UserProfileInfoDTO.builder()
                .id(userId)
                .name(user.getName())
                .surname(user.getSurname())
                .patronymic(user.getPatronymic())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .rank(0)
                .specialityTitle(user.getSpeciality().getTitle())
                .groupTitle(user.getGroup().getTitle())
                .courseNumber(user.getCourseNumber())
                .additionalEmail(user.getAdditionalEmail())
                .studentEmail(user.getStudentEmail())
                .roleTitle(user.getRole().getTitle())
                .vkLink(user.getVkLink())
                .courseNumber(user.getCourseNumber())
                .phoneNumber(user.getPhoneNumber())
                .photo(photoBase64)  // Конвертированное фото
                .build();

        return userProfileInfoDTO;
    }

    @Transactional
    public Page<UserResponseDTO> getAllUsers(int page, int size, String sortBy, String sortDirection, UserFilterDTO filter) {
        log.info("Getting users with pagination: page={}, size={}", page, size);

        String role = (filter.getRole() != null && filter.getRole().isEmpty()) ? null : filter.getRole();
        String search = (filter.getSearch() != null && filter.getSearch().isEmpty()) ? null : filter.getSearch();

        int offset = page * size;

        // Получаем данные через native query
        List<Object[]> results = userRepository.findAllWithFiltersNative(
                role,
                search,
                filter.getIsActive(),
                filter.getIsBanned(),
                filter.getGroupId(),
                filter.getSpecialityId(),
                filter.getSectorId(),
                offset,
                size
        );

        // Получаем общее количество
        long total = userRepository.countAllWithFiltersNative(
                role,
                search,
                filter.getIsActive(),
                filter.getIsBanned(),
                filter.getGroupId(),
                filter.getSpecialityId(),
                filter.getSectorId()
        );

        // Конвертируем результаты в DTO
        List<UserResponseDTO> users = results.stream()
                .map(this::mapRowToUserResponseDTO)
                .collect(Collectors.toList());

        // Создаем Page объект
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        return new PageImpl<>(users, pageable, total);
    }

    private UserResponseDTO mapRowToUserResponseDTO(Object[] row) {
        return UserResponseDTO.builder()
                .id(((Number) row[0]).longValue())
                .name((String) row[1])
                .surname((String) row[2])
                .patronymic((String) row[3])
                .gender((String) row[4])
                .dateOfBirth(row[5] != null ? ((java.sql.Date) row[5]).toLocalDate() : null)
                .courseNumber(row[6] != null ? ((Number) row[6]).shortValue() : null)
                .studentIdNumber(row[7] != null ? ((Number) row[7]).intValue() : null)
                .studentEmail((String) row[8])
                .additionalEmail((String) row[9])
                .phoneNumber((String) row[10])
                .vkLink((String) row[11])
                .isActive((Boolean) row[12])
                .isBanned((Boolean) row[13])
                .role((String) row[14])
                .groupId(row[15] != null ? ((Number) row[15]).longValue() : null)
                .groupName((String) row[16])
                .specialityId(row[17] != null ? ((Number) row[17]).longValue() : null)
                .specialityName((String) row[18])
                .photo(null)
                .build();
    }

    private UserResponseDTO mapToUserResponseDTO(Object[] row) {
        return UserResponseDTO.builder()
                .id(((Number) row[0]).longValue())
                .name((String) row[1])
                .surname((String) row[2])
                .patronymic((String) row[3])
                .gender((String) row[4])
                .dateOfBirth((java.time.LocalDate) row[5])
                .courseNumber(((Number) row[6]).shortValue())
                .studentIdNumber(((Number) row[7]).intValue())
                .studentEmail((String) row[8])
                .additionalEmail((String) row[9])
                .phoneNumber((String) row[10])
                .vkLink((String) row[11])
                .isActive((Boolean) row[12])
                .isBanned((Boolean) row[13])
                .role((String) row[14])
                .groupId(row[15] != null ? ((Number) row[15]).longValue() : null)
                .groupName((String) row[16])
                .specialityId(row[17] != null ? ((Number) row[17]).longValue() : null)
                .specialityName((String) row[18])
                .photo(null)  // Фото не загружаем
                .build();
    }

    @Transactional
    public Page<UserResponseDTO> getAllUsersSimple(int page, int size, String sortBy, String sortDirection) {
        log.info("Getting all users with pagination: page={}, size={}, sortBy={}, sortDirection={}",
                page, size, sortBy, sortDirection);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> usersPage = userRepository.findAll(pageable);

        // Конвертируем фото в Base64
        return usersPage.map(user -> {
            UserResponseDTO dto = userMapper.toResponseDto(user);
            if (user.getPhoto() != null && user.getPhoto().length > 0) {
                dto.setPhoto(ImageUtil.encodeToBase64(user.getPhoto()));
            }
            return dto;
        });
    }

    @Transactional
    public Page<UserResponseDTO> getUsersByRole(String role, int page, int size, String sortBy, String sortDirection) {
        log.info("Getting users by role: {}, page={}, size={}", role, page, size);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> usersPage = userRepository.findByRole(role, pageable);

        // Конвертируем фото в Base64
        return usersPage.map(user -> {
            UserResponseDTO dto = userMapper.toResponseDto(user);
            if (user.getPhoto() != null && user.getPhoto().length > 0) {
                dto.setPhoto(ImageUtil.encodeToBase64(user.getPhoto()));
            }
            return dto;
        });
    }
}
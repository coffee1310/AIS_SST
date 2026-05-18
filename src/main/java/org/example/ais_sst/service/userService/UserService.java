package org.example.ais_sst.service.userService;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.user.UserFilterDTO;
import org.example.ais_sst.dto.user.UserProfileInfoDTO;
import org.example.ais_sst.dto.user.UserResponseDTO;
import org.example.ais_sst.entity.SectorParticipant;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.entity.enums.Gender;
import org.example.ais_sst.entity.enums.SectorParticipantStatuses;
import org.example.ais_sst.exception.UserDoesNotExistException;
import org.example.ais_sst.mapper.UserMapper;
import org.example.ais_sst.repository.SectorParticipantRepository;
import org.example.ais_sst.repository.SocialStatusStudentsRepository;
import org.example.ais_sst.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.text.Collator;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserServiceImpl {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SocialStatusStudentsRepository socialStatusStudentRepository;
    private final SectorParticipantRepository sectorParticipantRepository;
    private final UserPhotoService userPhotoService; // Добавлено

    private final Cache<Long, UserProfileInfoDTO> userProfileCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .recordStats()
            .build();

    @Override
    public UserProfileInfoDTO getUserBasicInfo(Long userId) {
        // Проверяем кэш
        UserProfileInfoDTO cached = userProfileCache.getIfPresent(userId);
        if (cached != null) {
            log.debug("Returning user {} from cache", userId);
            return cached;
        }

        log.debug("Loading user {} from database", userId);

        // Загружаем из БД
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден"));

        UserProfileInfoDTO result = buildUserProfileDTO(user);

        // Сохраняем в кэш
        userProfileCache.put(userId, result);

        return result;
    }

    private UserProfileInfoDTO buildUserProfileDTO(User user) {
        String photoBase64 = user.getPathToPhoto() != null && !user.getPathToPhoto().isEmpty()
                ? userPhotoService.getPhotoAsBase64(user.getPathToPhoto())
                : null;

        List<String> socialStatuses = socialStatusStudentRepository.findSocialStatusTitlesByStudentId(user.getId());

        Long coordinatorSectorId = null;
        String coordinatorSectorTitle = null;

        List<Object[]> coordinatorInfoList = sectorParticipantRepository.findCoordinatorSectorInfoByUserId(user.getId());
        if (!coordinatorInfoList.isEmpty()) {
            Object[] info = coordinatorInfoList.get(0);
            if (info.length > 0 && info[0] != null) {
                coordinatorSectorId = ((Number) info[0]).longValue();
            }
            if (info.length > 1 && info[1] != null) {
                coordinatorSectorTitle = (String) info[1];
            }
        }

        List<String> userSectors = sectorParticipantRepository.findSectorTitlesByUserIdAndStatus(
                user.getId(), SectorParticipantStatuses.Активный);

        log.info("User {} is member of {} sectors: {}", user.getId(), userSectors.size(), userSectors);

        return UserProfileInfoDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .surname(user.getSurname())
                .patronymic(user.getPatronymic())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .rank(0)
                .specialityTitle(user.getSpeciality() != null ? user.getSpeciality().getTitle() : null)
                .groupTitle(user.getGroup() != null ? user.getGroup().getTitle() : null)
                .courseNumber(user.getCourseNumber())
                .additionalEmail(user.getAdditionalEmail())
                .studentEmail(user.getStudentEmail())
                .roleTitle(user.getRole().getTitle())
                .vkLink(user.getVkLink())
                .phoneNumber(user.getPhoneNumber())
                .photo(photoBase64)
                .socialStatuses(socialStatuses)
                .coordinatorSector(coordinatorSectorTitle)
                .coordinatorSectorId(coordinatorSectorId)
                .coordinatorSectorTitle(coordinatorSectorTitle)
                .shortSpecialityTitle(user.getSpeciality() != null ? user.getSpeciality().getShortTitle() : null)
                .events_count(0)
                .points_count(0)
                .userSectors(userSectors)
                .build();
    }

    @Transactional
    public Page<UserResponseDTO> getAllUsers(int page, int size, String sortBy, String sortDirection, UserFilterDTO filter) {
        page = Math.max(0, page);
        size = Math.min(100, Math.max(1, size));
        log.info("Getting users with pagination: page={}, size={}", page, size);

        int offset = page * size;

        List<Object[]> results = userRepository.findAllWithFiltersNative(
                filter.getId(),
                filter.getRole(),
                filter.getSearch(),
                filter.getIsActive(),
                filter.getIsBanned(),
                filter.getGroupId(),
                filter.getSpecialityId(),
                filter.getSectorId(),
                offset,
                size
        );

        long total = userRepository.countAllWithFiltersNative(
                filter.getId(),
                filter.getRole(),
                filter.getSearch(),
                filter.getIsActive(),
                filter.getIsBanned(),
                filter.getGroupId(),
                filter.getSpecialityId(),
                filter.getSectorId()
        );

        List<UserResponseDTO> users = results.stream()
                .map(row -> {
                    UserResponseDTO dto = mapRowToUserResponseDTO(row);
                    if (dto != null) {
                        // Заполняем социальные статусы
                        List<String> socialStatuses = socialStatusStudentRepository
                                .findSocialStatusTitlesByStudentId(dto.getId());
                        dto.setSocialStatuses(socialStatuses);

                        // НОВОЕ: заполняем список секторов пользователя
                        List<String> userSectors = sectorParticipantRepository
                                .findSectorTitlesByUserIdAndStatus(dto.getId(), SectorParticipantStatuses.Активный);
                        dto.setUserSectors(userSectors);

                        log.debug("User {} is member of {} sectors", dto.getId(), userSectors.size());
                    }
                    return dto;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Сортировка в Java
        Comparator<UserResponseDTO> comparator = Comparator.comparing(
                user -> {
                    switch (sortBy) {
                        case "name":
                            return user.getName();
                        case "surname":
                            return user.getSurname();
                        default:
                            return user.getId().toString();
                    }
                },
                Collator.getInstance(new Locale("ru"))
        );

        if ("DESC".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }

        users.sort(comparator);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        return new PageImpl<>(users, pageable, total);
    }

    @Transactional
    public Page<UserResponseDTO> getUsersByRole(String role, int page, int size, String sortBy, String sortDirection) {
        page = Math.max(0, page);
        size = Math.min(100, Math.max(1, size));
        log.info("Getting users by role: {}, page={}, size={}", role, page, size);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> usersPage = userRepository.findByRole(role, pageable);

        return usersPage.map(user -> {
            UserResponseDTO dto = userMapper.toResponseDto(user);

            // Конвертируем путь в Base64
            if (user.getPathToPhoto() != null && !user.getPathToPhoto().isEmpty()) {
                String photoBase64 = userPhotoService.getPhotoAsBase64(user.getPathToPhoto());
                dto.setPhoto(photoBase64);
            }

            List<Object[]> coordinatorInfoList = sectorParticipantRepository.findCoordinatorSectorInfoByUserId(user.getId());
            if (!coordinatorInfoList.isEmpty()) {
                Object[] info = coordinatorInfoList.get(0);
                if (info.length > 0 && info[0] != null) {
                    if (info[0] instanceof Number) {
                        dto.setCoordinatorSectorId(((Number) info[0]).longValue());
                    } else {
                        dto.setCoordinatorSectorId(Long.parseLong(info[0].toString()));
                    }
                }
                if (info.length > 1 && info[1] != null) {
                    dto.setCoordinatorSectorTitle((String) info[1]);
                }
            }

            // НОВОЕ: заполняем список секторов пользователя
            List<String> userSectors = sectorParticipantRepository
                    .findSectorTitlesByUserIdAndStatus(user.getId(), SectorParticipantStatuses.Активный);
            dto.setUserSectors(userSectors);

            // Заполняем социальные статусы
            List<String> socialStatuses = socialStatusStudentRepository
                    .findSocialStatusTitlesByStudentId(user.getId());
            dto.setSocialStatuses(socialStatuses);

            return dto;
        });
    }

    private UserResponseDTO mapRowToUserResponseDTO(Object[] row) {
        Long userId = null;
        try {
            userId = ((Number) row[0]).longValue();
        } catch (Exception e) {
            log.error("Error parsing user ID from row[0]: {}", row[0], e);
            return null;
        }

        // Получаем путь к фото из БД (индекс 19 - path_to_photo)
        String photoPath = row.length > 19 && row[19] != null ? (String) row[19] : null;
        String photoBase64 = userPhotoService.getPhotoAsBase64(photoPath);

        // Получаем информацию о секторе где пользователь координатор
        Long coordinatorSectorId = null;
        String coordinatorSectorTitle = null;

        try {
            Optional<SectorParticipant> coordinatorOpt = sectorParticipantRepository.findCoordinatorByUserId(userId);
            if (coordinatorOpt.isPresent()) {
                SectorParticipant sp = coordinatorOpt.get();
                coordinatorSectorId = sp.getSector().getId();
                coordinatorSectorTitle = sp.getSector().getTitle();
            }
        } catch (Exception e) {
            log.error("Error getting coordinator info for user: {}", userId, e);
        }

        String gender = null;
        if (row[4] != null) {
            if (row[4] instanceof String) {
                gender = (String) row[4];
            } else if (row[4] instanceof Gender) {
                gender = ((Gender) row[4]).toValue();
            } else {
                gender = row[4].toString();
            }
        }

        return UserResponseDTO.builder()
                .id(userId)
                .name(row[1] != null ? row[1].toString() : null)
                .surname(row[2] != null ? row[2].toString() : null)
                .patronymic(row[3] != null ? row[3].toString() : null)
                .gender(gender)
                .dateOfBirth(row[5] != null ? ((Date) row[5]).toLocalDate() : null)
                .courseNumber(row[6] != null ? ((Number) row[6]).shortValue() : null)
                .studentIdNumber(row[7] != null ? ((Number) row[7]).intValue() : null)
                .studentEmail(row[8] != null ? row[8].toString() : null)
                .additionalEmail(row[9] != null ? row[9].toString() : null)
                .phoneNumber(row[10] != null ? row[10].toString() : null)
                .vkLink(row[11] != null ? row[11].toString() : null)
                .isActive(row[12] != null ? (Boolean) row[12] : null)
                .isBanned(row[13] != null ? (Boolean) row[13] : null)
                .role(row[14] != null ? row[14].toString() : null)
                .groupId(row[15] != null ? ((Number) row[15]).longValue() : null)
                .groupName(row[16] != null ? row[16].toString() : null)
                .specialityId(row[17] != null ? ((Number) row[17]).longValue() : null)
                .specialityName(row[18] != null ? row[18].toString() : null)
                .photo(photoBase64)
                .coordinatorSectorId(coordinatorSectorId)
                .coordinatorSectorTitle(coordinatorSectorTitle)
                .specialityShortTitle(row.length > 20 && row[20] != null ? row[20].toString() : null)
                .socialStatuses(null)
                .build();
    }
}
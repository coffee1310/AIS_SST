package org.example.ais_sst.service.userService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.user.UserFilterDTO;
import org.example.ais_sst.dto.user.UserProfileInfoDTO;
import org.example.ais_sst.dto.user.UserProjectionDTO;
import org.example.ais_sst.dto.user.UserResponseDTO;
import org.example.ais_sst.entity.SectorParticipant;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.exception.UserDoesNotExistException;
import org.example.ais_sst.mapper.UserMapper;
import org.example.ais_sst.repository.SectorParticipantRepository;
import org.example.ais_sst.repository.SocialStatusStudentsRepository;
import org.example.ais_sst.repository.UserRepository;
import org.example.ais_sst.utils.ImageUtil;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.sql.Date;           // ← важно
import java.sql.Timestamp;      // ← важно
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserServiceImpl {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SocialStatusStudentsRepository socialStatusStudentRepository;
    private final SectorParticipantRepository sectorParticipantRepository;

    @Override
    public UserProfileInfoDTO getUserBasicInfo(Long userId) {
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден"));

        String photoBase64 = user.getPhoto() != null && user.getPhoto().length > 0
                ? ImageUtil.encodeToBase64(user.getPhoto())
                : null;

        List<String> socialStatuses = socialStatusStudentRepository.findSocialStatusTitlesByStudentId(userId);

        // ИСПРАВЛЕНИЕ: получаем список, а не Optional
        Long coordinatorSectorId = null;
        String coordinatorSectorTitle = null;

        List<Object[]> coordinatorInfoList = sectorParticipantRepository.findCoordinatorSectorInfoByUserId(userId);
        if (!coordinatorInfoList.isEmpty()) {
            Object[] info = coordinatorInfoList.get(0);
            if (info.length > 0 && info[0] != null) {
                coordinatorSectorId = ((Number) info[0]).longValue();
            }
            if (info.length > 1 && info[1] != null) {
                coordinatorSectorTitle = (String) info[1];
            }
        }

        return UserProfileInfoDTO.builder()
                .id(userId)
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
                .coordinatorSector(coordinatorSectorTitle)  // Для обратной совместимости
                .coordinatorSectorId(coordinatorSectorId)    // ID сектора
                .coordinatorSectorTitle(coordinatorSectorTitle) // Название сектора
                .events_count(0)
                .points_count(0)
                .build();
    }

    @Transactional
    public Page<UserResponseDTO> getAllUsers(int page, int size, String sortBy, String sortDirection, UserFilterDTO filter) {
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
                .map(this::mapRowToUserResponseDTO)
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        return new PageImpl<>(users, pageable, total);
    }

    // ==================== Вспомогательные конвертеры ====================

    private Long convertToLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number num) return num.longValue();
        if (value instanceof BigInteger bi) return bi.longValue();
        return Long.parseLong(value.toString());
    }

    private Short convertToShort(Object value) {
        if (value == null) return null;
        if (value instanceof Number num) return num.shortValue();
        return Short.parseShort(value.toString());
    }

    private Integer convertToInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number num) return num.intValue();
        return Integer.parseInt(value.toString());
    }

    private LocalDate convertToLocalDate(Object value) {
        if (value == null) return null;

        if (value instanceof LocalDate ld) return ld;
        if (value instanceof Date sqlDate) return sqlDate.toLocalDate();           // java.sql.Date
        if (value instanceof Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        if (value instanceof java.util.Date utilDate) {
            return utilDate.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }

        // Fallback
        try {
            return LocalDate.parse(value.toString().substring(0, 10)); // на случай строки
        } catch (Exception e) {
            log.warn("Не удалось преобразовать дату: {}", value);
            return null;
        }
    }

    @Transactional
    public Page<UserResponseDTO> getUsersByRole(String role, int page, int size, String sortBy, String sortDirection) {
        log.info("Getting users by role: {}, page={}, size={}", role, page, size);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> usersPage = userRepository.findByRole(role, pageable);

        return usersPage.map(user -> {
            UserResponseDTO dto = userMapper.toResponseDto(user);
            if (user.getPhoto() != null && user.getPhoto().length > 0) {
                dto.setPhoto(ImageUtil.encodeToBase64(user.getPhoto()));
            }

            // Исправление: получаем List, а не Optional
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

        // Получаем фото как byte[]
        byte[] photoBytes = null;
        try {
            photoBytes = (byte[]) row[19];
        } catch (Exception e) {
            log.error("Error parsing photo from row[19]: {}", row[19], e);
        }

        String photoBase64 = photoBytes != null && photoBytes.length > 0
                ? ImageUtil.encodeToBase64(photoBytes)
                : null;

        // Получаем информацию о секторе где пользователь координатор
        Long coordinatorSectorId = null;
        String coordinatorSectorTitle = null;

        try {
            log.info("Looking for coordinator info for user ID: {}", userId);

            // ДИАГНОСТИКА 1: Проверяем через findById
            Optional<SectorParticipant> coordinatorOpt = sectorParticipantRepository.findCoordinatorByUserId(userId);
            if (coordinatorOpt.isPresent()) {
                SectorParticipant sp = coordinatorOpt.get();
                log.info("Found via findCoordinatorByUserId - Sector ID: {}, Title: {}, isCoordinator: {}",
                        sp.getSector().getId(),
                        sp.getSector().getTitle(),
                        sp.getIsCoordinator());
                coordinatorSectorId = sp.getSector().getId();
                coordinatorSectorTitle = sp.getSector().getTitle();
            } else {
                log.warn("No coordinator found via findCoordinatorByUserId for user: {}", userId);

                // ДИАГНОСТИКА 2: Проверяем все записи пользователя
                List<SectorParticipant> allParticipations = sectorParticipantRepository.findByStudentId(userId);
                log.info("Total participations for user {}: {}", userId, allParticipations.size());
                for (SectorParticipant sp : allParticipations) {
                    log.info("Participation - Sector ID: {}, Title: {}, isCoordinator: {}, Status: {}",
                            sp.getSector().getId(),
                            sp.getSector().getTitle(),
                            sp.getIsCoordinator(),
                            sp.getStatus());
                }
            }

        } catch (Exception e) {
            log.error("Error getting coordinator info for user: {}", userId, e);
        }

        String gender = null;
        if (row[4] != null) {
            if (row[4] instanceof String) {
                gender = (String) row[4];
            } else if (row[4] instanceof org.example.ais_sst.entity.enums.Gender) {
                gender = ((org.example.ais_sst.entity.enums.Gender) row[4]).toValue();
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
                .build();
    }
}
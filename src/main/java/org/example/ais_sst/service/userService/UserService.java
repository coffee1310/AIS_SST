package org.example.ais_sst.service.userService;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.user.UserFilterDTO;
import org.example.ais_sst.dto.user.UserProjection;
import org.example.ais_sst.dto.user.UserResponseDTO;
import org.example.ais_sst.exception.UserDoesNotExistException;
import lombok.RequiredArgsConstructor;
import org.example.ais_sst.dto.user.UserProfileInfoDTO;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.mapper.UserMapper;
import org.example.ais_sst.repository.UserRepository;
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
        userProfileInfoDTO = UserProfileInfoDTO.builder()
                .id(userId)
                .name(user.getName())
                .surname(user.getSurname())
                .patronymic(user.getPatronymic())
                .dateOfBirth(user.getDateOfBirth())
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
                .build();

        return userProfileInfoDTO;
    }

    @Transactional
    public Page<UserResponseDTO> getAllUsers(int page, int size, String sortBy, String sortDirection, UserFilterDTO filter) {
        log.info("Getting users with pagination: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));

        // Получаем всех пользователей
        Page<User> usersPage = userRepository.findAll(pageable);

        // Фильтруем в Java
        List<User> filteredUsers = usersPage.getContent().stream()
                .filter(user -> {
                    // Фильтр по роли
                    if (filter.getRole() != null && !filter.getRole().isEmpty()) {
                        if (user.getRole() == null || !filter.getRole().equals(user.getRole().getTitle())) {
                            return false;
                        }
                    }
                    // Фильтр по активности
                    if (filter.getIsActive() != null) {
                        if (!filter.getIsActive().equals(user.getIsActive())) {
                            return false;
                        }
                    }
                    // Фильтр по бану
                    if (filter.getIsBanned() != null) {
                        if (!filter.getIsBanned().equals(user.getIsBanned())) {
                            return false;
                        }
                    }
                    // Фильтр по группе
                    if (filter.getGroupId() != null) {
                        if (user.getGroup() == null || !filter.getGroupId().equals(user.getGroup().getId())) {
                            return false;
                        }
                    }
                    // Фильтр по специальности
                    if (filter.getSpecialityId() != null) {
                        if (user.getSpeciality() == null || !filter.getSpecialityId().equals(user.getSpeciality().getId())) {
                            return false;
                        }
                    }
                    // Поиск по имени, фамилии, email
                    if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                        String searchLower = filter.getSearch().toLowerCase();
                        return (user.getName() != null && user.getName().toLowerCase().contains(searchLower)) ||
                                (user.getSurname() != null && user.getSurname().toLowerCase().contains(searchLower)) ||
                                (user.getStudentEmail() != null && user.getStudentEmail().toLowerCase().contains(searchLower));
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // Создаем новую Page с отфильтрованными данными
        Page<User> filteredPage = new PageImpl<>(filteredUsers, pageable, filteredUsers.size());

        return filteredPage.map(userMapper::toResponseDto);
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


    @Transactional()
    public Page<UserResponseDTO> getAllUsersSimple(int page, int size, String sortBy, String sortDirection) {
        log.info("Getting all users with pagination: page={}, size={}, sortBy={}, sortDirection={}",
                page, size, sortBy, sortDirection);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> usersPage = userRepository.findAll(pageable);
        return usersPage.map(userMapper::toResponseDto);
    }

    @Transactional()
    public Page<UserResponseDTO> getUsersByRole(String role, int page, int size, String sortBy, String sortDirection) {
        log.info("Getting users by role: {}, page={}, size={}", role, page, size);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> usersPage = userRepository.findByRole(role, pageable);
        return usersPage.map(userMapper::toResponseDto);
    }

}

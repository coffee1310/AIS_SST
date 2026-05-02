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

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));

        // Получаем всех пользователей
        Page<User> usersPage = userRepository.findAll(pageable);

        // Фильтруем в Java и конвертируем фото
        List<UserResponseDTO> filteredUsers = usersPage.getContent().stream()
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
                .map(user -> {
                    UserResponseDTO dto = userMapper.toResponseDto(user);
                    // Конвертируем фото в Base64
                    if (user.getPhoto() != null && user.getPhoto().length > 0) {
                        dto.setPhoto(ImageUtil.encodeToBase64(user.getPhoto()));
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        // Создаем новую Page с отфильтрованными данными
        return new PageImpl<>(filteredUsers, pageable, filteredUsers.size());
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
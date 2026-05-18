package org.example.ais_sst.service.accountCreatingRequestsService;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestFilterDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestRejectDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestResponseDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestsSummaryDTO;
import org.example.ais_sst.dto.social_status.UserSocialStatusesDTO;
import org.example.ais_sst.dto.user.UserSummaryDTO;
import org.example.ais_sst.entity.*;
import org.example.ais_sst.entity.enums.AccountCreatingRequestStatus;
import org.example.ais_sst.entity.enums.Gender;
import org.example.ais_sst.exception.*;
import org.example.ais_sst.mapper.AccountCreatingRequestMapper;
import org.example.ais_sst.mapper.UserMapper;
import org.example.ais_sst.repository.*;
import org.example.ais_sst.service.base.BaseEntityService;
import org.example.ais_sst.service.socialStatusService.AccountCreatingRequestsSocialStatusService;
import org.example.ais_sst.service.socialStatusService.SocialStatusService;
import org.example.ais_sst.service.userService.UserPhotoService;
import org.example.ais_sst.utils.ImageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AccountCreatingRequestsService extends BaseEntityService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final SpecialityRepository specialityRepository;
    private final AccountCreatingRequestsSocialStatusService accountCreatingRequestsSocialStatusService;
    private final SocialStatusService socialStatusService;
    private final PasswordEncoder passwordEncoder;
    private final AccountCreatingRequestsRepository accountCreatingRequestsRepository;
    private final RoleRepository roleRepository;
    private final AccountCreatingRequestSocialStatusRepository accountCreatingRequestsSocialStatusesRepository;
    private final AccountRequestPhotoService accountRequestPhotoService;
    private final UserPhotoService userPhotoService;
    private final AccountCreatingRequestMapper requestMapper;
    private final UserMapper userMapper;

    public AccountCreatingRequest createAccountRequest(AccountCreatingRequestsSummaryDTO dto) {
        return executeWithLogging(() -> {
            validateUniqueEmail(dto.getStudentEmail());
            validateUniquePhone(dto.getPhoneNumber());

            Group userGroup = findEntityOrThrow(dto.getGroup_id(), groupRepository::findGroupById,
                    () -> new GroupDoesNotExistException("Группа не найдена"), "Group");

            Speciality userSpeciality = findEntityOrThrow(dto.getSpeciality_id(), specialityRepository::findSpecialityById,
                    () -> new SpecialityDoesNotExistException("Специальность не найдена"), "Speciality");

            AccountCreatingRequest request = requestMapper.toEntity(dto);
            request.setGroup(userGroup);
            request.setSpeciality(userSpeciality);
            request.setPassword(passwordEncoder.encode(dto.getPassword()));
            request.setStatus(AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ);

            AccountCreatingRequest savedRequest = accountCreatingRequestsRepository.save(request);
            log.info("Account request saved with ID: {}", savedRequest.getId());

            savePhotoIfPresent(dto, savedRequest);
            dto.setId(savedRequest.getId());
            accountCreatingRequestsSocialStatusService.createAccountCreatingRequestSocialStatus(dto);

            return savedRequest;
        }, "createAccountRequest", dto.getStudentEmail());
    }

    private void validateUniqueEmail(String email) {
        validateState(!userRepository.existsByStudentEmail(email),
                () -> new EmailAlreadyExistsException("Email уже используется"),
                "Email already exists: " + email);
    }

    private void validateUniquePhone(String phone) {
        validateState(!userRepository.existsByPhoneNumber(phone),
                () -> new PhoneAlreadyExistException("Телефон уже используется"),
                "Phone already exists: " + phone);
    }

    private void savePhotoIfPresent(AccountCreatingRequestsSummaryDTO dto, AccountCreatingRequest request) {
        if (dto.getPhoto() != null && !dto.getPhoto().isEmpty()) {
            try {
                String photoPath = accountRequestPhotoService.savePhotoFromBase64(dto.getPhoto(), request.getId());
                request.setPathToPhoto(photoPath);
                accountCreatingRequestsRepository.save(request);
                log.info("Photo saved for request: {}", request.getId());
            } catch (IOException e) {
                log.error("Failed to save photo for request: {}", request.getId(), e);
            }
        }
    }

    public AccountCreatingRequestResponseDTO rejectAccountRequest(Long id, AccountCreatingRequestRejectDTO rejectDto) {
        return executeWithLogging(() -> {
            AccountCreatingRequest request = findEntityOrThrow(id, accountCreatingRequestsRepository::findAccountCreatingRequestById,
                    () -> new AccountCreatingRequestDoesNotExistException("Заявка не найдена"), "Request");

            validateState(request.getStatus() == AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ,
                    () -> new IllegalStateException("Заявка уже обработана"),
                    "Request already processed, status: " + request.getStatus());

            request.setStatus(AccountCreatingRequestStatus.ОТКЛОНЕНА);
            request.setReasonForRefusal(rejectDto.getRejectionReason());

            AccountCreatingRequest rejectedRequest = accountCreatingRequestsRepository.save(request);
            log.info("Request {} rejected", id);

            return requestMapper.toResponseDto(rejectedRequest, accountRequestPhotoService);
        }, "rejectAccountRequest", id);
    }

    public AccountCreatingRequestResponseDTO acceptAccountRequest(Long id) {
        return executeWithLogging(() -> {
            AccountCreatingRequest request = findEntityOrThrow(id, accountCreatingRequestsRepository::findAccountCreatingRequestById,
                    () -> new AccountCreatingRequestDoesNotExistException("Заявка не найдена"), "Request");

            validateState(request.getStatus() == AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ,
                    () -> new IllegalStateException("Заявка уже обработана"),
                    "Request already processed");

            Role role = findEntityOrThrow("Activist", roleRepository::findByTitle,
                    () -> new RuntimeException("Роль Activist не найдена"), "Role");

            User user = buildUserFromRequest(request, role);
            User savedUser = userRepository.save(user);
            log.info("User created from request {} with ID: {}", id, savedUser.getId());

            copyPhotoFromRequest(request, savedUser);
            assignSocialStatuses(id, savedUser.getId());

            request.setStatus(AccountCreatingRequestStatus.ОДОБРЕНА);
            accountCreatingRequestsRepository.save(request);

            return requestMapper.toResponseDto(request, accountRequestPhotoService);
        }, "acceptAccountRequest", id);
    }

    private User buildUserFromRequest(AccountCreatingRequest request, Role role) {
        return User.builder()
                .name(request.getName())
                .surname(request.getSurname())
                .patronymic(request.getPatronymic())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .studentEmail(request.getStudentEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(request.getPassword())
                .studentIdNumber(request.getStudentIdNumber())
                .courseNumber(request.getCourseNumber())
                .role(role)
                .group(request.getGroup())
                .speciality(request.getSpeciality())
                .isActive(true)
                .isBanned(false)
                .vkLink(request.getVkLink())
                .additionalEmail(request.getAdditionalEmail())
                .build();
    }

    private void copyPhotoFromRequest(AccountCreatingRequest request, User user) {
        if (request.getPathToPhoto() != null && !request.getPathToPhoto().isEmpty()) {
            try {
                String photoBase64 = accountRequestPhotoService.getPhotoAsBase64(request.getPathToPhoto());
                if (photoBase64 != null) {
                    String userPhotoPath = userPhotoService.savePhotoFromBase64(photoBase64, user.getId());
                    user.setPathToPhoto(userPhotoPath);
                    userRepository.save(user);
                    log.info("Photo copied from request {} to user {}", request.getId(), user.getId());
                }
            } catch (IOException e) {
                log.error("Failed to copy photo", e);
            }
        }
    }

    private void assignSocialStatuses(Long requestId, Long userId) {
        List<Long> socialStatusIds = accountCreatingRequestsSocialStatusService.getSocialStatusIdsByRequestId(requestId);
        if (socialStatusIds != null && !socialStatusIds.isEmpty()) {
            UserSocialStatusesDTO dto = UserSocialStatusesDTO.builder()
                    .userId(userId)
                    .social_statuses_id(socialStatusIds)
                    .build();
            socialStatusService.createUserSocialStatuses(dto);
            log.info("Social statuses assigned to user {}", userId);
        }
    }

    public Page<AccountCreatingRequestResponseDTO> getRequests(Pageable pageable) {
        return accountCreatingRequestsRepository.findAll(pageable)
                .map(request -> enhanceDtoWithSocialStatuses(
                        requestMapper.toResponseDto(request, accountRequestPhotoService), request.getId()));
    }

    public Page<AccountCreatingRequestResponseDTO> getPendingRequests(Pageable pageable) {
        log.info("Getting pending account requests");
        return accountCreatingRequestsRepository.findByStatus(AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ, pageable)
                .map(request -> {
                    AccountCreatingRequestResponseDTO dto = requestMapper.toResponseDto(request, accountRequestPhotoService);
                    dto.setSocialStatuses(accountCreatingRequestsSocialStatusesRepository
                            .findSocialStatusTitlesByRequestId(request.getId()));
                    dto.setAdditionalEmail(request.getAdditionalEmail());
                    dto.setVkLink(request.getVkLink());
                    return dto;
                });
    }

    public Page<AccountCreatingRequestResponseDTO> getRequestsWithFilters(
            AccountCreatingRequestFilterDTO filter, int page, int size, String sortBy, String sortDirection) {

        log.info("Getting account requests with filters: {}", filter);

        int offset = page * size;
        String statusStr = convertStatusToDbValue(filter.getStatus());

        List<Object[]> results = accountCreatingRequestsRepository.findAllWithFiltersNative(
                filter.getId(), filter.getName(), filter.getSurname(), filter.getPatronymic(),
                filter.getGender(), filter.getDateFrom(), filter.getDateTo(),
                filter.getStudentEmail(), filter.getPhoneNumber(), filter.getStudentIdNumber(),
                filter.getCourseNumber(), statusStr, filter.getGroupId(), filter.getSpecialityId(),
                offset, size);

        long total = accountCreatingRequestsRepository.countAllWithFiltersNative(
                filter.getId(), filter.getName(), filter.getSurname(), filter.getPatronymic(),
                filter.getGender(), filter.getDateFrom(), filter.getDateTo(),
                filter.getStudentEmail(), filter.getPhoneNumber(), filter.getStudentIdNumber(),
                filter.getCourseNumber(), statusStr, filter.getGroupId(), filter.getSpecialityId());

        List<AccountCreatingRequestResponseDTO> dtoList = results.stream()
                .map(row -> enhanceDtoWithSocialStatuses(
                        mapRowToResponseDTO(row), ((Number) row[0]).longValue()))
                .collect(Collectors.toList());

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return new PageImpl<>(dtoList, pageable, total);
    }

    private AccountCreatingRequestResponseDTO enhanceDtoWithSocialStatuses(AccountCreatingRequestResponseDTO dto, Long requestId) {
        List<String> socialStatuses = accountCreatingRequestsSocialStatusesRepository
                .findSocialStatusTitlesByRequestId(requestId);
        dto.setSocialStatuses(socialStatuses);
        return dto;
    }

    private AccountCreatingRequestResponseDTO mapRowToResponseDTO(Object[] row) {
        String photoPath = row.length > 18 && row[18] != null ? (String) row[18] : null;

        return AccountCreatingRequestResponseDTO.builder()
                .id(((Number) row[0]).longValue())
                .name((String) row[1])
                .surname((String) row[2])
                .patronymic((String) row[3])
                .gender((String) row[4])
                .dateOfBirth(row[5] != null ? ((java.sql.Date) row[5]).toLocalDate() : null)
                .courseNumber(row[6] != null ? ((Number) row[6]).shortValue() : null)
                .studentIdNumber(row[7] != null ? ((Number) row[7]).intValue() : null)
                .studentEmail((String) row[8])
                .phoneNumber((String) row[9])
                .reasonForRefusal((String) row[10])
                .status(convertDbValueToStatus((String) row[11]))
                .createdAt(row[12] != null ? ((java.sql.Timestamp) row[12]).toLocalDateTime() : null)
                .updatedAt(row[13] != null ? ((java.sql.Timestamp) row[13]).toLocalDateTime() : null)
                .groupId(row[14] != null ? ((Number) row[14]).longValue() : null)
                .groupName((String) row[15])
                .specialityId(row[16] != null ? ((Number) row[16]).longValue() : null)
                .specialityName((String) row[17])
                .photo(accountRequestPhotoService.getPhotoAsBase64(photoPath))
                .build();
    }

    private String convertStatusToDbValue(AccountCreatingRequestStatus status) {
        if (status == null) return null;
        return switch (status) {
            case НА_РАССМОТРЕНИИ -> "На рассмотрении";
            case ОДОБРЕНА -> "Одобрена";
            case ОТКЛОНЕНА -> "Отклонена";
            default -> null;
        };
    }

    private AccountCreatingRequestStatus convertDbValueToStatus(String dbValue) {
        if (dbValue == null) return null;
        return switch (dbValue) {
            case "На рассмотрении" -> AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ;
            case "Одобрена" -> AccountCreatingRequestStatus.ОДОБРЕНА;
            case "Отклонена" -> AccountCreatingRequestStatus.ОТКЛОНЕНА;
            default -> {
                log.warn("Unknown status from DB: {}", dbValue);
                yield null;
            }
        };
    }
}
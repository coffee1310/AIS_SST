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
import org.example.ais_sst.service.socialStatusService.AccountCreatingRequestsSocialStatusService;
import org.example.ais_sst.service.socialStatusService.SocialStatusService;
import org.example.ais_sst.utils.ImageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AccountCreatingRequestsService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final SpecialityRepository specialityRepository;
    private final AccountCreatingRequestsSocialStatusService accountCreatingRequestsSocialStatusService;
    private final SocialStatusService socialStatusService;
    private final PasswordEncoder passwordEncoder;
    private final AccountCreatingRequestsRepository accountCreatingRequestsRepository;
    private final RoleRepository roleRepository;

    // Мапперы
    private final AccountCreatingRequestMapper requestMapper;
    private final UserMapper userMapper;

    public AccountCreatingRequest createAccountRequest(AccountCreatingRequestsSummaryDTO dto) {
        log.info("Registration attempt for email: {}", dto.getStudentEmail());

        if (userRepository.existsByStudentEmail(dto.getStudentEmail())) {
            throw new EmailAlreadyExistsException("Ошибка: Email уже используется!");
        }

        if (userRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
            throw new PhoneAlreadyExistException("Ошибка: Телефон уже используется!");
        }

        Group userGroup = groupRepository.findGroupById(dto.getGroup_id())
                .orElseThrow(() -> new GroupDoesNotExistException(
                        String.format("Ошибка: Группа с id: %s не существует", dto.getGroup_id())));

        Speciality userSpeciality = specialityRepository.findSpecialityById(dto.getSpeciality_id())
                .orElseThrow(() -> new SpecialityDoesNotExistException(
                        String.format("Ошибка: Специальность с id: %s не существует", dto.getSpeciality_id())));

        byte[] photoBytes = dto.getPhoto() != null && !dto.getPhoto().isEmpty()
                ? ImageUtil.decodeFromBase64(dto.getPhoto())
                : null;

        AccountCreatingRequest accountCreatingRequest = requestMapper.toEntity(dto);
        accountCreatingRequest.setGroup(userGroup);
        accountCreatingRequest.setSpeciality(userSpeciality);
        accountCreatingRequest.setPassword(passwordEncoder.encode(dto.getPassword()));
        accountCreatingRequest.setStatus(AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ);
        accountCreatingRequest.setPhoto(photoBytes);

        AccountCreatingRequest savedRequest = accountCreatingRequestsRepository.save(accountCreatingRequest);
        log.info("Account request registered successfully with ID: {}", savedRequest.getId());

        dto.setId(savedRequest.getId());
        accountCreatingRequestsSocialStatusService.createAccountCreatingRequestSocialStatus(dto);
        log.info("Social statuses for request registered successfully");

        return savedRequest;
    }

    public AccountCreatingRequestResponseDTO rejectAccountRequest(Long id, AccountCreatingRequestRejectDTO rejectDto) {
        log.info("Rejecting account request with id: {}", id);

        AccountCreatingRequest request = accountCreatingRequestsRepository.findAccountCreatingRequestById(id)
                .orElseThrow(() -> new AccountCreatingRequestDoesNotExistException(
                        String.format("Заявка с id: %s не существует", id)));

        // Проверяем, что заявка еще не обработана
        if (request.getStatus() != AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ) {
            throw new IllegalStateException(
                    String.format("Заявка с id: %s уже обработана. Текущий статус: %s", id, request.getStatus()));
        }

        // Обновляем статус и причину отказа
        request.setStatus(AccountCreatingRequestStatus.ОТКЛОНЕНА);
        request.setReasonForRefusal(rejectDto.getRejectionReason());

        AccountCreatingRequest rejectedRequest = accountCreatingRequestsRepository.save(request);
        log.info("Account request with id: {} rejected. Reason: {}", id, rejectDto.getRejectionReason());

        // Возвращаем DTO через маппер
        return requestMapper.toResponseDto(rejectedRequest);
    }

    public UserSummaryDTO acceptAccountRequest(Long id) {
        AccountCreatingRequest request = accountCreatingRequestsRepository.findAccountCreatingRequestById(id)
                .orElseThrow(() -> new AccountCreatingRequestDoesNotExistException(
                        String.format("Заявка с id: %s не существует", id)));

        if (request.getStatus() != AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ) {
            throw new IllegalStateException(
                    String.format("Заявка с id: %s уже обработана. Статус: %s", id, request.getStatus()));
        }

        Role role = roleRepository.findByTitle("Activist")
                .orElseThrow(() -> new RuntimeException("Роль Активист не найдена"));

        User user = User.builder()
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
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully from request ID: {}, User ID: {}", id, savedUser.getId());

        // Копируем социальные статусы
        List<Long> socialStatusIds = accountCreatingRequestsSocialStatusService.getSocialStatusIdsByRequestId(id);

        if (socialStatusIds != null && !socialStatusIds.isEmpty()) {
            UserSocialStatusesDTO socialStatusesDTO = UserSocialStatusesDTO.builder()
                    .userId(savedUser.getId())
                    .social_statuses_id(socialStatusIds)
                    .build();

            socialStatusService.createUserSocialStatuses(socialStatusesDTO);
            log.info("Social statuses assigned to user ID: {}", savedUser.getId());
        }

        request.setStatus(AccountCreatingRequestStatus.ОДОБРЕНА);
        accountCreatingRequestsRepository.save(request);
        log.info("Account request ID: {} approved", id);

        // Возвращаем DTO через маппер
        return userMapper.toDto(savedUser);
    }

    public Page<AccountCreatingRequestResponseDTO> getRequests(Pageable pageable) {
        Page<AccountCreatingRequest> requests = accountCreatingRequestsRepository.findAll(pageable);
        return requests.map(requestMapper::toResponseDto);
    }

    public Page<AccountCreatingRequestResponseDTO> getPendingRequests(Pageable pageable) {
        log.info("Getting pending account requests");
        Page<AccountCreatingRequest> requests = accountCreatingRequestsRepository
                .findByStatus(AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ, pageable);
        return requests.map(requestMapper::toResponseDto);
    }

    /**
     * Универсальный метод получения заявок с фильтрами
     */
    public Page<AccountCreatingRequestResponseDTO> getRequestsWithFilters(
            AccountCreatingRequestFilterDTO filter,
            int page,
            int size,
            String sortBy,
            String sortDirection) {

        log.info("Getting account requests with filters: {}", filter);

        int offset = page * size;

        String statusStr = filter.getStatus() != null ? filter.getStatus().name() : null;
        String genderStr = filter.getGender();

        List<Object[]> results = accountCreatingRequestsRepository.findAllWithFiltersNative(
                filter.getId(),
                filter.getName(),
                filter.getSurname(),
                filter.getPatronymic(),
                genderStr,
                filter.getDateFrom(),
                filter.getDateTo(),
                filter.getStudentEmail(),
                filter.getPhoneNumber(),
                filter.getStudentIdNumber(),
                filter.getCourseNumber(),
                statusStr,
                filter.getGroupId(),
                filter.getSpecialityId(),
                offset,
                size);

        long total = accountCreatingRequestsRepository.countAllWithFiltersNative(
                filter.getId(),
                filter.getName(),
                filter.getSurname(),
                filter.getPatronymic(),
                genderStr,
                filter.getDateFrom(),
                filter.getDateTo(),
                filter.getStudentEmail(),
                filter.getPhoneNumber(),
                filter.getStudentIdNumber(),
                filter.getCourseNumber(),
                statusStr,
                filter.getGroupId(),
                filter.getSpecialityId());

        List<AccountCreatingRequestResponseDTO> dtoList = results.stream()
                .map(this::mapRowToAccountCreatingRequestResponseDTO)
                .collect(Collectors.toList());

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return new PageImpl<>(dtoList, pageable, total);
    }

    private AccountCreatingRequestResponseDTO mapRowToAccountCreatingRequestResponseDTO(Object[] row) {
        // Получаем статус из БД (русская строка)
        String statusFromDb = (String) row[11];

        // Преобразуем русскую строку в enum
        AccountCreatingRequestStatus status = null;
        if (statusFromDb != null) {
            switch (statusFromDb) {
                case "НА_РАССМОТРЕНИИ":
                    status = AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ;
                    break;
                case "ОДОБРЕНА":
                    status = AccountCreatingRequestStatus.ОДОБРЕНА;
                    break;
                case "ОТКЛОНЕНА":
                    status = AccountCreatingRequestStatus.ОТКЛОНЕНА;
                    break;
                default:
                    log.warn("Unknown status: {}", statusFromDb);
            }
        }

        // Получаем фото как byte[]
        byte[] photoBytes = (byte[]) row[18];
        String photoBase64 = photoBytes != null && photoBytes.length > 0
                ? ImageUtil.encodeToBase64(photoBytes)
                : null;

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
                .status(status)
                .createdAt(row[12] != null ? ((java.sql.Timestamp) row[12]).toLocalDateTime() : null)
                .updatedAt(row[13] != null ? ((java.sql.Timestamp) row[13]).toLocalDateTime() : null)
                .groupId(row[14] != null ? ((Number) row[14]).longValue() : null)
                .groupName((String) row[15])
                .specialityId(row[16] != null ? ((Number) row[16]).longValue() : null)
                .specialityName((String) row[17])
                .photo(photoBase64)  // Устанавливаем фото в Base64
                .build();
    }
}
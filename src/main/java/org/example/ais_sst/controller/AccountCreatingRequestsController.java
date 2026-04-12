package org.example.ais_sst.controller;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestRejectDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestsSummaryDTO;
import org.example.ais_sst.dto.social_status.UserSocialStatusesDTO;
import org.example.ais_sst.entity.AccountCreatingRequest;
import org.example.ais_sst.entity.Role;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.entity.enums.AccountCreatingRequestStatus;
import org.example.ais_sst.exception.AccountCreatingRequestDoesNotExistException;
import org.example.ais_sst.repository.AccountCreatingRequestsRepository;
import org.example.ais_sst.repository.RoleRepository;
import org.example.ais_sst.repository.UserRepository;
import org.example.ais_sst.service.accountCreatingRequestsService.AccountCreatingRequestsService;
import org.example.ais_sst.service.socialStatusService.AccountCreatingRequestsSocialStatusService;
import org.example.ais_sst.service.socialStatusService.SocialStatusService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.http.HttpResponse;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/account_requests")
@RequiredArgsConstructor
public class AccountCreatingRequestsController {

    private final AccountCreatingRequestsService accountCreatingRequestsService;
    private final SocialStatusService socialStatusService;
    private final AccountCreatingRequestsSocialStatusService accountCreatingRequestsSocialStatusService;

    private final AccountCreatingRequestsRepository accountCreatingRequestsRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createAccountRequest(@RequestBody @Valid AccountCreatingRequestsSummaryDTO AccountRequestDTO) throws Exception {
        AccountCreatingRequest accountCreatingRequest = accountCreatingRequestsService.createAccountRequest(AccountRequestDTO);
        return new ResponseEntity<>(accountCreatingRequest, HttpStatus.CREATED);
    }

    @PutMapping("/reject/{id}")
    public ResponseEntity<?> rejectAccountRequest(@PathVariable Long id, @Valid @RequestBody AccountCreatingRequestRejectDTO accountCreatingRequestReject) {
        AccountCreatingRequest accountCreatingRequest = accountCreatingRequestsService.rejectAccountRequest(id, accountCreatingRequestReject);
        return new ResponseEntity<>("Заявка отклонена", HttpStatus.OK);
    }

    @PutMapping("/accept/{id}")
    public ResponseEntity<?>  acceptAccountRequest(@PathVariable Long id) {
        AccountCreatingRequest request = accountCreatingRequestsRepository.findAccountCreatingRequestById(id)
                .orElseThrow(() -> new AccountCreatingRequestDoesNotExistException(
                        String.format("Заявка с id: %s не существует", id)));

        // Проверяем, что заявка на рассмотрении
        if (request.getStatus() != AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ) {
            throw new IllegalStateException(
                    String.format("Заявка с id: %s уже обработана. Статус: %s", id, request.getStatus()));
        }

        Role userRole = roleRepository.findByTitle("Activist")
                .orElseThrow(() -> new RuntimeException("Ошибка: Роль Activist не найдена в БД!"));

        // Создаем пользователя из заявки
        User user = User.builder()
                .name(request.getName())
                .surname(request.getSurname())
                .patronymic(request.getPatronymic())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .studentEmail(request.getStudentEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(request.getPassword()) // Пароль уже закодирован при создании заявки
                .studentIdNumber(request.getStudentIdNumber())
                .courseNumber(request.getCourseNumber())
                .role(userRole) // По умолчанию обычный пользователь
                .group(request.getGroup())
                .speciality(request.getSpeciality())
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully from request ID: {}, User ID: {}", id, savedUser.getId());

        // Получаем социальные статусы из заявки и привязываем к пользователю
        // Для этого нужно получить DTO или список статусов из заявки
        // Вариант 1: Если у вас есть доступ к social_statuses через связь
        List<Long> socialStatusIds = accountCreatingRequestsSocialStatusService
                .getSocialStatusIdsByRequestId(id);

        if (socialStatusIds != null && !socialStatusIds.isEmpty()) {
            UserSocialStatusesDTO socialStatusesDTO = UserSocialStatusesDTO.builder()
                    .userId(savedUser.getId())
                    .social_statuses_id(socialStatusIds)
                    .build();

            socialStatusService.createUserSocialStatuses(socialStatusesDTO);
            log.info("Social statuses assigned to user ID: {}", savedUser.getId());
        }

        // Обновляем статус заявки
        request.setStatus(AccountCreatingRequestStatus.ОДОБРЕНА);
        accountCreatingRequestsRepository.save(request);
        log.info("Account request ID: {} approved", id);

        return new ResponseEntity<>("Заявка принята", HttpStatus.ACCEPTED);
    }

        @GetMapping
    public Page<?> getRequests(@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return accountCreatingRequestsService.getRequests(pageable);
    }
}

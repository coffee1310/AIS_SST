package org.example.ais_sst.service.accountCreatingRequestsService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestRejectDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestResponseDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestsSummaryDTO;
import org.example.ais_sst.entity.AccountCreatingRequest;
import org.example.ais_sst.entity.Group;
import org.example.ais_sst.entity.Speciality;
import org.example.ais_sst.entity.enums.AccountCreatingRequestStatus;
import org.example.ais_sst.entity.enums.Gender;
import org.example.ais_sst.exception.*;
import org.example.ais_sst.repository.AccountCreatingRequestsRepository;
import org.example.ais_sst.repository.GroupRepository;
import org.example.ais_sst.repository.SpecialityRepository;
import org.example.ais_sst.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AccountCreatingRequestsService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final SpecialityRepository specialityRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountCreatingRequestsRepository accountCreatingRequestsRepository;

    public AccountCreatingRequest createAccountRequest(AccountCreatingRequestsSummaryDTO AccountRequestDTO) throws Exception {
        log.info("Registration attempt for email: {}", AccountRequestDTO.getStudentEmail());

        if (userRepository.existsByStudentEmail(AccountRequestDTO.getStudentEmail())) {
            log.warn("Registration failed - email already exists: {}", AccountRequestDTO.getStudentEmail());
            throw new EmailAlreadyExistsException("Ошибка: Email уже используется!");
        }

        if (userRepository.existsByPhoneNumber(AccountRequestDTO.getPhoneNumber())) {
            log.warn("Registration failed - phone already exists: {}", AccountRequestDTO.getPhoneNumber());
            throw new PhoneAlreadyExistException("Ошибка: Телефон уже используется!");
        }

        try {
            Group userGroup = groupRepository.findGroupById(AccountRequestDTO.getGroup_id())
                    .orElseThrow(() -> new GroupDoesNotExistException(String.format("Ошибка: Группа с id: %s не существует", AccountRequestDTO.getGroup_id())));

            Speciality userSpeciality = specialityRepository.findSpecialityById(AccountRequestDTO.getSpeciality_id())
                    .orElseThrow(() -> new SpecialityDoesNotExistException(String.format("Ошибка: Специальность с id: %s не существует", AccountRequestDTO.getSpeciality_id())));

           AccountCreatingRequest accountCreatingRequest = AccountCreatingRequest.builder()
                    .name(AccountRequestDTO.getName())
                    .surname(AccountRequestDTO.getSurname())
                    .patronymic(AccountRequestDTO.getPatronymic())
                    .gender(Gender.valueOf(AccountRequestDTO.getGender()))
                    .dateOfBirth(AccountRequestDTO.getDateOfBirth())
                    .studentEmail(AccountRequestDTO.getStudentEmail())
                    .phoneNumber(AccountRequestDTO.getPhoneNumber())
                    .password(passwordEncoder.encode(AccountRequestDTO.getPassword()))
                    .reasonForRefusal(null)
                    .studentIdNumber(AccountRequestDTO.getStudentIdNumber())
                    .courseNumber(AccountRequestDTO.getCourseNumber())
                    .status(AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ)
                    .group(userGroup)
                    .speciality(userSpeciality)
                    .build();

            AccountCreatingRequest savedAccountCreatingRequest = accountCreatingRequestsRepository.save(accountCreatingRequest);
            log.info("User registered successfully with ID: {}", savedAccountCreatingRequest.getId());

            return savedAccountCreatingRequest;
        } catch (Exception e) {
            log.error("Registration failed: ", e);
            throw new Exception("Ошибка при создании заявки: " + e.getMessage(), e);
        }
    }

    public AccountCreatingRequest rejectAccountRequest(Long id, AccountCreatingRequestRejectDTO accountCreatingRequestReject) {
        AccountCreatingRequest accountCreatingRequest = accountCreatingRequestsRepository.findAccountCreatingRequestById(id)
                .orElseThrow(() -> new AccountCreatingRequestDoesNotExistException(String.format("Заявка с id: %s не существует", id)));

        accountCreatingRequest.setStatus(AccountCreatingRequestStatus.ОТКЛОНЕНА);
        accountCreatingRequest.setReasonForRefusal(accountCreatingRequestReject.getRejectionReason());
        return accountCreatingRequestsRepository.save(accountCreatingRequest);
    }

    public AccountCreatingRequest acceptAccountRequest(Long id) {
        AccountCreatingRequest accountCreatingRequest = accountCreatingRequestsRepository.findAccountCreatingRequestById(id)
                .orElseThrow(() -> new AccountCreatingRequestDoesNotExistException(String.format("Заявка с id: %s не существует", id)));

        accountCreatingRequest.setStatus(AccountCreatingRequestStatus.ОДОБРЕНА);
        return accountCreatingRequestsRepository.save(accountCreatingRequest);
    }

    public Page<AccountCreatingRequestResponseDTO> getRequests(Pageable pageable) {
        Page<AccountCreatingRequest> requests = accountCreatingRequestsRepository.findAll(pageable);
        return requests.map(AccountCreatingRequestResponseDTO::from);
    }

}

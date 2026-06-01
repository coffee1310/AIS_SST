package org.example.ais_sst.controller.applications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestFilterDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestRejectDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestResponseDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestsSummaryDTO;
import org.example.ais_sst.mapper.AccountCreatingRequestMapper;
import org.example.ais_sst.service.accountCreatingRequestsService.AccountCreatingRequestsService;
import org.example.ais_sst.service.accountCreatingRequestsService.AccountRequestPhotoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountApplicationStrategy implements
        ApplicationStrategy<AccountCreatingRequestResponseDTO,
                AccountCreatingRequestsSummaryDTO,
                AccountCreatingRequestRejectDTO,
                AccountCreatingRequestFilterDTO> {

    private final AccountCreatingRequestsService accountCreatingRequestsService;
    private final AccountCreatingRequestMapper accountCreatingRequestMapper;
    private final AccountRequestPhotoService accountRequestPhotoService;

    @Override
    public AccountCreatingRequestResponseDTO createApplication(AccountCreatingRequestsSummaryDTO createDto) {
        log.info("Creating account creation application");
        return accountCreatingRequestMapper.toResponseDto(
                accountCreatingRequestsService.createAccountRequest(createDto),
                accountRequestPhotoService
        );
    }

    @Override
    public AccountCreatingRequestResponseDTO rejectApplication(Long id, AccountCreatingRequestRejectDTO rejectDto) {
        log.info("Account creating request with id: {} was rejected", id);
        return accountCreatingRequestsService.rejectAccountRequest(id, rejectDto);
    }

    @Override
    public AccountCreatingRequestResponseDTO acceptApplication(Long id) {
        log.info("Account creating request with id: {} was accepted", id);
        return accountCreatingRequestsService.acceptAccountRequest(id);
    }

    @Override
    public AccountCreatingRequestResponseDTO getApplicationById(Long id) {
        log.info("Getting account creating request with id: {}", id);
        return accountCreatingRequestsService.getRequestById(id);
    }

    @Override
    public Page<AccountCreatingRequestResponseDTO> getAllApplications(
            AccountCreatingRequestFilterDTO filter,
            Pageable pageable) {
        return accountCreatingRequestsService.getRequestsWithFilters(
                filter,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().getOrderFor("createdAt") != null ? "createdAt" : "id",
                pageable.getSort().getOrderFor("createdAt") != null ?
                        pageable.getSort().getOrderFor("createdAt").getDirection().name() : "DESC"
        );
    }
}
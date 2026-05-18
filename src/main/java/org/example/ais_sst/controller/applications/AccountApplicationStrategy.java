package org.example.ais_sst.controller.applications;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestFilterDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestRejectDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestResponseDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestsSummaryDTO;
import org.example.ais_sst.entity.AccountCreatingRequest;
import org.example.ais_sst.mapper.AccountCreatingRequestMapper;
import org.example.ais_sst.service.accountCreatingRequestsService.AccountCreatingRequestsService;
import org.example.ais_sst.service.accountCreatingRequestsService.AccountRequestPhotoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
    public AccountCreatingRequestResponseDTO createApplication(@RequestBody @Valid AccountCreatingRequestsSummaryDTO dto) {
        log.info("Creating account creation application");
        return accountCreatingRequestMapper.toResponseDto(accountCreatingRequestsService.createAccountRequest(dto)
                , accountRequestPhotoService);
    }

    @Override
    public AccountCreatingRequestResponseDTO rejectApplication(Long id, AccountCreatingRequestRejectDTO RejectDto) {
        log.info("Account creating request with id: {} was rejected", id);
        return accountCreatingRequestsService.rejectAccountRequest(id, RejectDto);
    }

    @Override
    public AccountCreatingRequestResponseDTO acceptApplication(Long id) {
        log.info("Account creating request with id: {} was accepted", id);
        return accountCreatingRequestsService.acceptAccountRequest(id);
    }

    @Override
    public AccountCreatingRequestResponseDTO getById(Long id) {
        return null;
    }

    @Override
    public Page<AccountCreatingRequestResponseDTO> getAll(AccountCreatingRequestFilterDTO filter,
                                                          Pageable pageable) {
        return accountCreatingRequestsService.getRequestsWithFilters(
                filter,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().getOrderFor("createdAt") != null ? "createdAt" : "id",
                pageable.getSort().getOrderFor("createdAt") != null ? "DESC" : "ASC"
        );
    }
}

package org.example.ais_sst.controller.applications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestFilterDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestRejectDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestResponseDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestsSummaryDTO;
import org.example.ais_sst.service.accountCreatingRequestsService.AccountCreatingRequestsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/account_requestsNew")
@RequiredArgsConstructor
public class AccountCreatingRequestsControllerNew extends BaseApplicationController<
        AccountCreatingRequestResponseDTO,
        AccountCreatingRequestsSummaryDTO,
        AccountCreatingRequestRejectDTO,
        AccountCreatingRequestFilterDTO> {

    private final AccountApplicationStrategy accountApplicationStrategy;
    private final AccountCreatingRequestsService accountCreatingRequestsService;

    @Override
    protected AccountApplicationStrategy getStrategy() {
        return accountApplicationStrategy;
    }

    @Override
    protected String getApplicationName() {
        return "Заявка на создание аккаунта";
    }

    // Специфичный метод для получения pending заявок
    @GetMapping("/pending")
    public Page<AccountCreatingRequestResponseDTO> getPendingRequests(
            @PageableDefault(size = 20, direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Getting pending requests");
        return accountCreatingRequestsService.getPendingRequests(pageable);
    }
}
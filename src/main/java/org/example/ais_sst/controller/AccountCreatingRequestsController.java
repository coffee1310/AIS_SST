package org.example.ais_sst.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestRejectDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestResponseDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestsSummaryDTO;
import org.example.ais_sst.dto.user.UserSummaryDTO;
import org.example.ais_sst.entity.AccountCreatingRequest;
import org.example.ais_sst.service.accountCreatingRequestsService.AccountCreatingRequestsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/account_requests")
@RequiredArgsConstructor
public class AccountCreatingRequestsController {

    private final AccountCreatingRequestsService accountCreatingRequestsService;

    @PostMapping
    public ResponseEntity<?> createAccountRequest(@RequestBody @Valid AccountCreatingRequestsSummaryDTO dto) {
        AccountCreatingRequest accountCreatingRequest = accountCreatingRequestsService.createAccountRequest(dto);
        return new ResponseEntity<>(accountCreatingRequest, HttpStatus.CREATED);
    }

    @PutMapping("/reject/{id}")
    public ResponseEntity<?> rejectAccountRequest(
            @PathVariable Long id,
            @Valid @RequestBody AccountCreatingRequestRejectDTO rejectDto) {

        AccountCreatingRequestResponseDTO rejectedRequest = accountCreatingRequestsService.rejectAccountRequest(id, rejectDto);

        return ResponseEntity.ok()
                .body(Map.of(
                        "message", "Заявка отклонена",
                        "request", rejectedRequest
                ));
    }

    @PutMapping("/accept/{id}")
    public ResponseEntity<?> acceptAccountRequest(@PathVariable Long id) {
        UserSummaryDTO createdUser = accountCreatingRequestsService.acceptAccountRequest(id);

        return ResponseEntity.ok()
                .body(Map.of(
                        "message", "Заявка принята. Пользователь создан.",
                        "user", createdUser
                ));
    }

    @GetMapping
    public Page<AccountCreatingRequestResponseDTO> getRequests(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return accountCreatingRequestsService.getRequests(pageable);
    }
}
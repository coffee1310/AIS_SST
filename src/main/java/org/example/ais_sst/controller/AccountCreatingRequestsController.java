package org.example.ais_sst.controller;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestRejectDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestsSummaryDTO;
import org.example.ais_sst.entity.AccountCreatingRequest;
import org.example.ais_sst.service.accountCreatingRequestsService.AccountCreatingRequestsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/account_requests")
@RequiredArgsConstructor
public class AccountCreatingRequestsController {

    private final AccountCreatingRequestsService accountCreatingRequestsService;

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
    public ResponseEntity<?> acceptAccountRequest(@PathVariable Long id) {
        AccountCreatingRequest accountCreatingRequest = accountCreatingRequestsService.acceptAccountRequest(id);
        return new ResponseEntity<>("Заявка принята", HttpStatus.OK);
    }

    @GetMapping
    public Page<?> getRequests(@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return accountCreatingRequestsService.getRequests(pageable);
    }
}

package org.example.ais_sst.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestsSummaryDTO;
import org.example.ais_sst.entity.AccountCreatingRequest;
import org.example.ais_sst.repository.GroupRepository;
import org.example.ais_sst.repository.SpecialityRepository;
import org.example.ais_sst.repository.UserRepository;
import org.example.ais_sst.service.accountCreatingRequestsService.AccountCreatingRequestsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/account_requests")
@RequiredArgsConstructor
public class AccountCreatingRequestsController {

    private final AccountCreatingRequestsService accountCreatingRequestsService;

    @PostMapping
    public ResponseEntity<?> createAccountRequests(@RequestBody @Valid AccountCreatingRequestsSummaryDTO AccountRequestDTO) throws Exception {
        AccountCreatingRequest accountCreatingRequest = accountCreatingRequestsService.createAccountRequest(AccountRequestDTO);
        return new ResponseEntity<>(accountCreatingRequest, HttpStatus.CREATED);
    }


}

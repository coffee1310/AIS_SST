package org.example.ais_sst.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.controller.base.BaseController;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestFilterDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestRejectDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestResponseDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestsSummaryDTO;
import org.example.ais_sst.dto.common.PageRequestDTO;
import org.example.ais_sst.dto.user.UserSummaryDTO;
import org.example.ais_sst.entity.AccountCreatingRequest;
import org.example.ais_sst.entity.CustomUserDetails;
import org.example.ais_sst.entity.enums.AccountCreatingRequestStatus;
import org.example.ais_sst.service.accountCreatingRequestsService.AccountCreatingRequestsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/account_requests")
@RequiredArgsConstructor
public class AccountCreatingRequestsController extends BaseController {

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

        AccountCreatingRequestResponseDTO rejectedRequest =
                accountCreatingRequestsService.rejectAccountRequest(id, rejectDto);

        return createSuccessResponse("Заявка отклонена", rejectedRequest);
    }

    @PutMapping("/accept/{id}")
    public ResponseEntity<?> acceptAccountRequest(@PathVariable Long id) {
        accountCreatingRequestsService.acceptAccountRequest(id);
        return createSuccessResponse("Заявка принята. Пользователь создан.");
    }

    @GetMapping
    public Page<AccountCreatingRequestResponseDTO> getRequests(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return accountCreatingRequestsService.getRequests(pageable);
    }

    @GetMapping("/filter")
    public Page<AccountCreatingRequestResponseDTO> getRequestsWithFilters(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            @RequestParam(required = false) String patronymic,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String studentEmail,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) Integer studentIdNumber,
            @RequestParam(required = false) Short courseNumber,
            @RequestParam(required = false) AccountCreatingRequestStatus status,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long specialityId,
            @RequestParam(required = false) Boolean hasPhoto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        logInfo("/api/account_requests/filter", "Getting requests with filters");

        AccountCreatingRequestFilterDTO filter = AccountCreatingRequestFilterDTO.builder()
                .id(id).name(name).surname(surname).patronymic(patronymic)
                .gender(gender).dateFrom(dateFrom).dateTo(dateTo)
                .studentEmail(studentEmail).phoneNumber(phoneNumber)
                .studentIdNumber(studentIdNumber).courseNumber(courseNumber)
                .status(status).groupId(groupId).specialityId(specialityId)
                .hasPhoto(hasPhoto).build();

        // Используем метод для сервисов с параметрами (не Pageable)
        return getFilteredPageWithParams(
                accountCreatingRequestsService::getRequestsWithFilters,
                filter,
                page, size, sortBy, sortDirection
        );
    }

    @GetMapping("/pending")
    public Page<AccountCreatingRequestResponseDTO> getPendingRequests(
            @PageableDefault(size = 20, direction = Sort.Direction.DESC) Pageable pageable) {
        logInfo("/api/account_requests/pending", "Getting pending requests");
        return accountCreatingRequestsService.getPendingRequests(pageable);
    }
}
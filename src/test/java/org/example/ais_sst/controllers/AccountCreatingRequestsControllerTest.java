package org.example.ais_sst.controllers;

import org.example.ais_sst.controller.AccountCreatingRequestsController;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestFilterDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestRejectDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestResponseDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestsSummaryDTO;
import org.example.ais_sst.dto.user.UserSummaryDTO;
import org.example.ais_sst.entity.AccountCreatingRequest;
import org.example.ais_sst.entity.enums.AccountCreatingRequestStatus;
import org.example.ais_sst.service.accountCreatingRequestsService.AccountCreatingRequestsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountCreatingRequestsControllerTest {

    @Mock
    private AccountCreatingRequestsService accountCreatingRequestsService;

    @InjectMocks
    private AccountCreatingRequestsController accountCreatingRequestsController;

    private AccountCreatingRequestsSummaryDTO summaryDTO;
    private AccountCreatingRequest accountCreatingRequest;
    private AccountCreatingRequestResponseDTO responseDTO;
    private AccountCreatingRequestRejectDTO rejectDTO;
    private UserSummaryDTO userSummaryDTO;
    private Page<AccountCreatingRequestResponseDTO> responsePage;

    @BeforeEach
    void setUp() {
        summaryDTO = AccountCreatingRequestsSummaryDTO.builder()
                .name("Иван")
                .surname("Иванов")
                .patronymic("Иванович")
                .gender("Мужчина")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .studentEmail("ivan@test.com")
                .phoneNumber("+79991234567")
                .password("password123")
                .studentIdNumber(12345)
                .courseNumber((short) 3)
                .group_id(1L)
                .speciality_id(1L)
                .build();

        accountCreatingRequest = AccountCreatingRequest.builder()
                .id(1L)
                .name("Иван")
                .surname("Иванов")
                .status(AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ)
                .build();

        responseDTO = AccountCreatingRequestResponseDTO.builder()
                .id(1L)
                .name("Иван")
                .surname("Иванов")
                .status(AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ)
                .build();

        rejectDTO = AccountCreatingRequestRejectDTO.builder()
                .rejectionReason("Недостаточно данных")
                .build();

        userSummaryDTO = UserSummaryDTO.builder()
                .id(1L)
                .name("Иван")
                .surname("Иванов")
                .studentEmail("ivan@test.com")
                .build();

        responsePage = new PageImpl<>(java.util.List.of(responseDTO));
    }

    // ==================== TESTS FOR createAccountRequest ====================

    @Test
    void createAccountRequest_Success() {
        // given
        when(accountCreatingRequestsService.createAccountRequest(any(AccountCreatingRequestsSummaryDTO.class)))
                .thenReturn(accountCreatingRequest);

        // when
        ResponseEntity<?> response = accountCreatingRequestsController.createAccountRequest(summaryDTO);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(accountCreatingRequest);

        verify(accountCreatingRequestsService).createAccountRequest(summaryDTO);
    }

    @Test
    void createAccountRequest_WithInvalidData_ThrowsException() {
        // given
        AccountCreatingRequestsSummaryDTO invalidDTO = AccountCreatingRequestsSummaryDTO.builder().build();
        when(accountCreatingRequestsService.createAccountRequest(any(AccountCreatingRequestsSummaryDTO.class)))
                .thenThrow(new RuntimeException("Invalid data"));

        // when & then
        // Контроллер выбросит исключение, которое будет перехвачено глобальным обработчиком
        // В тесте мы просто проверяем, что исключение было выброшено
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            accountCreatingRequestsController.createAccountRequest(invalidDTO);
        });

        verify(accountCreatingRequestsService).createAccountRequest(invalidDTO);
    }

    // ==================== TESTS FOR rejectAccountRequest ====================

    @Test
    void rejectAccountRequest_Success() {
        // given
        when(accountCreatingRequestsService.rejectAccountRequest(eq(1L), any(AccountCreatingRequestRejectDTO.class)))
                .thenReturn(responseDTO);

        // when
        ResponseEntity<?> response = accountCreatingRequestsController.rejectAccountRequest(1L, rejectDTO);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKey("message");
        assertThat(body).containsKey("request");
        assertThat(body.get("message")).isEqualTo("Заявка отклонена");
        assertThat(body.get("request")).isEqualTo(responseDTO);

        verify(accountCreatingRequestsService).rejectAccountRequest(eq(1L), any(AccountCreatingRequestRejectDTO.class));
    }

    @Test
    void rejectAccountRequest_WithNullReason_Success() {
        // given
        AccountCreatingRequestRejectDTO emptyRejectDTO = AccountCreatingRequestRejectDTO.builder().build();
        when(accountCreatingRequestsService.rejectAccountRequest(eq(1L), any(AccountCreatingRequestRejectDTO.class)))
                .thenReturn(responseDTO);

        // when
        ResponseEntity<?> response = accountCreatingRequestsController.rejectAccountRequest(1L, emptyRejectDTO);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(accountCreatingRequestsService).rejectAccountRequest(eq(1L), any(AccountCreatingRequestRejectDTO.class));
    }

    // ==================== TESTS FOR acceptAccountRequest ====================

    @Test
    void acceptAccountRequest_Success() {
        // given
        when(accountCreatingRequestsService.acceptAccountRequest(1L)).thenReturn(userSummaryDTO);

        // when
        ResponseEntity<?> response = accountCreatingRequestsController.acceptAccountRequest(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKey("message");
        assertThat(body).containsKey("user");
        assertThat(body.get("message")).isEqualTo("Заявка принята. Пользователь создан.");
        assertThat(body.get("user")).isEqualTo(userSummaryDTO);

        verify(accountCreatingRequestsService).acceptAccountRequest(1L);
    }

    // ==================== TESTS FOR getRequests ====================

    @Test
    void getRequests_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 20, Sort.by("id").descending());
        when(accountCreatingRequestsService.getRequests(any(Pageable.class))).thenReturn(responsePage);

        // when
        Page<AccountCreatingRequestResponseDTO> result = accountCreatingRequestsController.getRequests(pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);

        verify(accountCreatingRequestsService).getRequests(any(Pageable.class));
    }

    @Test
    void getRequests_WithCustomPageable_Success() {
        // given
        Pageable pageable = PageRequest.of(1, 10, Sort.by("createdAt").descending());
        when(accountCreatingRequestsService.getRequests(any(Pageable.class))).thenReturn(responsePage);

        // when
        Page<AccountCreatingRequestResponseDTO> result = accountCreatingRequestsController.getRequests(pageable);

        // then
        assertThat(result).isNotNull();
        verify(accountCreatingRequestsService).getRequests(pageable);
    }

    // ==================== TESTS FOR getPendingRequests ====================

    @Test
    void getPendingRequests_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 20, Sort.by("id").descending());
        when(accountCreatingRequestsService.getPendingRequests(any(Pageable.class))).thenReturn(responsePage);

        // when
        Page<AccountCreatingRequestResponseDTO> result = accountCreatingRequestsController.getPendingRequests(pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(accountCreatingRequestsService).getPendingRequests(any(Pageable.class));
    }

    // ==================== TESTS FOR getRequestsWithFilters ====================

    @Test
    void getRequestsWithFilters_Success() {
        // given
        when(accountCreatingRequestsService.getRequestsWithFilters(
                any(AccountCreatingRequestFilterDTO.class), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(responsePage);

        // when
        Page<AccountCreatingRequestResponseDTO> result = accountCreatingRequestsController.getRequestsWithFilters(
                1L, "Иван", "Иванов", "Иванович", "Мужчина",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31),
                "ivan@test.com", "+79991234567", 12345, (short) 3,
                AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ, 1L, 1L, true,
                0, 20, "createdAt", "DESC");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(accountCreatingRequestsService).getRequestsWithFilters(
                any(AccountCreatingRequestFilterDTO.class), eq(0), eq(20), eq("createdAt"), eq("DESC"));
    }

    @Test
    void getRequestsWithFilters_WithDefaultValues_Success() {
        // given
        when(accountCreatingRequestsService.getRequestsWithFilters(
                any(AccountCreatingRequestFilterDTO.class), eq(0), eq(20), eq("createdAt"), eq("DESC")))
                .thenReturn(responsePage);

        // when - вызываем без параметров, используем значения по умолчанию
        Page<AccountCreatingRequestResponseDTO> result = accountCreatingRequestsController.getRequestsWithFilters(
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, 0, 20, "createdAt", "DESC");

        // then
        assertThat(result).isNotNull();

        verify(accountCreatingRequestsService).getRequestsWithFilters(
                any(AccountCreatingRequestFilterDTO.class), eq(0), eq(20), eq("createdAt"), eq("DESC"));
    }

    @Test
    void getRequestsWithFilters_WithOnlyIdFilter_Success() {
        // given
        when(accountCreatingRequestsService.getRequestsWithFilters(
                any(AccountCreatingRequestFilterDTO.class), eq(0), eq(20), eq("id"), eq("ASC")))
                .thenReturn(responsePage);

        // when
        Page<AccountCreatingRequestResponseDTO> result = accountCreatingRequestsController.getRequestsWithFilters(
                1L, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, 0, 20, "id", "ASC");

        // then
        assertThat(result).isNotNull();

        verify(accountCreatingRequestsService).getRequestsWithFilters(
                any(AccountCreatingRequestFilterDTO.class), eq(0), eq(20), eq("id"), eq("ASC"));
    }

    @Test
    void getRequestsWithFilters_WithDateRangeFilter_Success() {
        // given
        LocalDate dateFrom = LocalDate.of(2024, 1, 1);
        LocalDate dateTo = LocalDate.of(2024, 12, 31);

        when(accountCreatingRequestsService.getRequestsWithFilters(
                any(AccountCreatingRequestFilterDTO.class), eq(0), eq(20), eq("createdAt"), eq("DESC")))
                .thenReturn(responsePage);

        // when
        Page<AccountCreatingRequestResponseDTO> result = accountCreatingRequestsController.getRequestsWithFilters(
                null, null, null, null, null, dateFrom, dateTo, null, null, null, null,
                null, null, null, null, 0, 20, "createdAt", "DESC");

        // then
        assertThat(result).isNotNull();

        verify(accountCreatingRequestsService).getRequestsWithFilters(
                any(AccountCreatingRequestFilterDTO.class), eq(0), eq(20), eq("createdAt"), eq("DESC"));
    }

    @Test
    void getRequestsWithFilters_WithStatusFilter_Success() {
        // given
        when(accountCreatingRequestsService.getRequestsWithFilters(
                any(AccountCreatingRequestFilterDTO.class), eq(0), eq(20), eq("createdAt"), eq("DESC")))
                .thenReturn(responsePage);

        // when
        Page<AccountCreatingRequestResponseDTO> result = accountCreatingRequestsController.getRequestsWithFilters(
                null, null, null, null, null, null, null, null, null, null, null,
                AccountCreatingRequestStatus.ОДОБРЕНА, null, null, null, 0, 20, "createdAt", "DESC");

        // then
        assertThat(result).isNotNull();

        verify(accountCreatingRequestsService).getRequestsWithFilters(
                any(AccountCreatingRequestFilterDTO.class), eq(0), eq(20), eq("createdAt"), eq("DESC"));
    }

    @Test
    void getRequestsWithFilters_WithPaginationParams_Success() {
        // given
        when(accountCreatingRequestsService.getRequestsWithFilters(
                any(AccountCreatingRequestFilterDTO.class), eq(2), eq(50), eq("studentEmail"), eq("ASC")))
                .thenReturn(responsePage);

        // when
        Page<AccountCreatingRequestResponseDTO> result = accountCreatingRequestsController.getRequestsWithFilters(
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, 2, 50, "studentEmail", "ASC");

        // then
        assertThat(result).isNotNull();

        verify(accountCreatingRequestsService).getRequestsWithFilters(
                any(AccountCreatingRequestFilterDTO.class), eq(2), eq(50), eq("studentEmail"), eq("ASC"));
    }

    @Test
    void getRequestsWithFilters_WithHasPhotoFilter_Success() {
        // given
        when(accountCreatingRequestsService.getRequestsWithFilters(
                any(AccountCreatingRequestFilterDTO.class), eq(0), eq(20), eq("createdAt"), eq("DESC")))
                .thenReturn(responsePage);

        // when
        Page<AccountCreatingRequestResponseDTO> result = accountCreatingRequestsController.getRequestsWithFilters(
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, true, 0, 20, "createdAt", "DESC");

        // then
        assertThat(result).isNotNull();

        verify(accountCreatingRequestsService).getRequestsWithFilters(
                any(AccountCreatingRequestFilterDTO.class), eq(0), eq(20), eq("createdAt"), eq("DESC"));
    }
}
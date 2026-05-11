package org.example.ais_sst.services.SocialStatusesService;

import jakarta.persistence.EntityNotFoundException;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestsSummaryDTO;
import org.example.ais_sst.entity.AccountCreatingRequest;
import org.example.ais_sst.entity.AccountCreatingRequestsSocialStatuses;
import org.example.ais_sst.entity.SocialStatus;
import org.example.ais_sst.exception.SocialStatusDoesNotExistException;
import org.example.ais_sst.repository.AccountCreatingRequestSocialStatusRepository;
import org.example.ais_sst.repository.AccountCreatingRequestsRepository;
import org.example.ais_sst.repository.SocialStatusRepository;
import org.example.ais_sst.service.socialStatusService.AccountCreatingRequestsSocialStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountCreatingRequestsSocialStatusServiceTest {

    @Mock
    private AccountCreatingRequestsRepository accountCreatingRequestsRepository;

    @Mock
    private AccountCreatingRequestSocialStatusRepository accountCreatingRequestSocialStatusRepository;

    @Mock
    private SocialStatusRepository socialStatusRepository;

    @InjectMocks
    private AccountCreatingRequestsSocialStatusService accountCreatingRequestsSocialStatusService;

    private AccountCreatingRequest accountCreatingRequest;
    private SocialStatus socialStatus1;
    private SocialStatus socialStatus2;
    private AccountCreatingRequestsSummaryDTO summaryDto;
    private AccountCreatingRequestsSocialStatuses relation1;
    private AccountCreatingRequestsSocialStatuses relation2;

    @BeforeEach
    void setUp() {
        accountCreatingRequest = AccountCreatingRequest.builder()
                .id(1L)
                .name("Иван")
                .surname("Иванов")
                .studentEmail("ivan@test.com")
                .build();

        socialStatus1 = SocialStatus.builder()
                .id(1L)
                .title("Студент")
                .build();

        socialStatus2 = SocialStatus.builder()
                .id(2L)
                .title("Активист")
                .build();

        summaryDto = AccountCreatingRequestsSummaryDTO.builder()
                .id(1L)
                .name("Иван")
                .surname("Иванов")
                .social_statuses_id(Arrays.asList(1L, 2L))
                .build();

        relation1 = AccountCreatingRequestsSocialStatuses.builder()
                .id(1L)
                .accountCreatingRequest(accountCreatingRequest)
                .socialStatus(socialStatus1)
                .build();

        relation2 = AccountCreatingRequestsSocialStatuses.builder()
                .id(2L)
                .accountCreatingRequest(accountCreatingRequest)
                .socialStatus(socialStatus2)
                .build();
    }

    // ==================== TESTS FOR getSocialStatusIdsByRequestId ====================

    @Test
    void getSocialStatusIdsByRequestId_Success() {
        // given
        when(accountCreatingRequestsRepository.findAccountCreatingRequestById(1L))
                .thenReturn(Optional.of(accountCreatingRequest));
        when(accountCreatingRequestSocialStatusRepository.findByAccountCreatingRequest(accountCreatingRequest))
                .thenReturn(Arrays.asList(relation1, relation2));

        // when
        List<Long> result = accountCreatingRequestsSocialStatusService.getSocialStatusIdsByRequestId(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(1L, 2L);

        verify(accountCreatingRequestsRepository).findAccountCreatingRequestById(1L);
        verify(accountCreatingRequestSocialStatusRepository).findByAccountCreatingRequest(accountCreatingRequest);
    }

    @Test
    void getSocialStatusIdsByRequestId_NoSocialStatuses_ReturnsEmptyList() {
        // given
        when(accountCreatingRequestsRepository.findAccountCreatingRequestById(1L))
                .thenReturn(Optional.of(accountCreatingRequest));
        when(accountCreatingRequestSocialStatusRepository.findByAccountCreatingRequest(accountCreatingRequest))
                .thenReturn(Collections.emptyList());

        // when
        List<Long> result = accountCreatingRequestsSocialStatusService.getSocialStatusIdsByRequestId(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(accountCreatingRequestsRepository).findAccountCreatingRequestById(1L);
        verify(accountCreatingRequestSocialStatusRepository).findByAccountCreatingRequest(accountCreatingRequest);
    }

    @Test
    void getSocialStatusIdsByRequestId_RequestNotFound_ThrowsEntityNotFoundException() {
        // given
        when(accountCreatingRequestsRepository.findAccountCreatingRequestById(1L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> accountCreatingRequestsSocialStatusService.getSocialStatusIdsByRequestId(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Request with id: 1 not found");

        verify(accountCreatingRequestsRepository).findAccountCreatingRequestById(1L);
        verify(accountCreatingRequestSocialStatusRepository, never()).findByAccountCreatingRequest(any());
    }

    @Test
    void getSocialStatusIdsByRequestId_WithNullId_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> accountCreatingRequestsSocialStatusService.getSocialStatusIdsByRequestId(null))
                .isInstanceOf(Exception.class);
    }

    // ==================== TESTS FOR createAccountCreatingRequestSocialStatus ====================

    @Test
    void createAccountCreatingRequestSocialStatus_Success() {
        // given
        when(accountCreatingRequestsRepository.findAccountCreatingRequestById(1L))
                .thenReturn(Optional.of(accountCreatingRequest));
        when(socialStatusRepository.findAllById(anyList()))
                .thenReturn(Arrays.asList(socialStatus1, socialStatus2));
        when(accountCreatingRequestSocialStatusRepository.saveAll(anyList()))
                .thenReturn(Arrays.asList(relation1, relation2));

        // when
        List<AccountCreatingRequestsSocialStatuses> result =
                accountCreatingRequestsSocialStatusService.createAccountCreatingRequestSocialStatus(summaryDto);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSocialStatus().getId()).isEqualTo(1L);
        assertThat(result.get(1).getSocialStatus().getId()).isEqualTo(2L);

        verify(accountCreatingRequestsRepository).findAccountCreatingRequestById(1L);
        verify(socialStatusRepository).findAllById(Arrays.asList(1L, 2L));
        verify(accountCreatingRequestSocialStatusRepository).saveAll(anyList());
    }

    @Test
    void createAccountCreatingRequestSocialStatus_SingleSocialStatus_Success() {
        // given
        summaryDto.setSocial_statuses_id(List.of(1L));

        when(accountCreatingRequestsRepository.findAccountCreatingRequestById(1L))
                .thenReturn(Optional.of(accountCreatingRequest));
        when(socialStatusRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(socialStatus1));
        when(accountCreatingRequestSocialStatusRepository.saveAll(anyList()))
                .thenReturn(List.of(relation1));

        // when
        List<AccountCreatingRequestsSocialStatuses> result =
                accountCreatingRequestsSocialStatusService.createAccountCreatingRequestSocialStatus(summaryDto);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSocialStatus().getId()).isEqualTo(1L);

        verify(socialStatusRepository).findAllById(List.of(1L));
        verify(accountCreatingRequestSocialStatusRepository).saveAll(anyList());
    }

    @Test
    void createAccountCreatingRequestSocialStatus_EmptySocialStatusList_Success() {
        // given
        summaryDto.setSocial_statuses_id(Collections.emptyList());

        when(accountCreatingRequestsRepository.findAccountCreatingRequestById(1L))
                .thenReturn(Optional.of(accountCreatingRequest));
        when(socialStatusRepository.findAllById(Collections.emptyList()))
                .thenReturn(Collections.emptyList());
        when(accountCreatingRequestSocialStatusRepository.saveAll(Collections.emptyList()))
                .thenReturn(Collections.emptyList());

        // when
        List<AccountCreatingRequestsSocialStatuses> result =
                accountCreatingRequestsSocialStatusService.createAccountCreatingRequestSocialStatus(summaryDto);

        // then
        assertThat(result).isEmpty();

        verify(socialStatusRepository).findAllById(Collections.emptyList());
        verify(accountCreatingRequestSocialStatusRepository).saveAll(Collections.emptyList());
    }

    @Test
    void createAccountCreatingRequestSocialStatus_RequestNotFound_ThrowsEntityNotFoundException() {
        // given
        when(accountCreatingRequestsRepository.findAccountCreatingRequestById(1L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> accountCreatingRequestsSocialStatusService
                .createAccountCreatingRequestSocialStatus(summaryDto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Request with id: 1 not found");

        verify(accountCreatingRequestsRepository).findAccountCreatingRequestById(1L);
        verify(socialStatusRepository, never()).findAllById(any());
        verify(accountCreatingRequestSocialStatusRepository, never()).saveAll(any());
    }

    @Test
    void createAccountCreatingRequestSocialStatus_SomeSocialStatusesNotFound_ThrowsSocialStatusDoesNotExistException() {
        // given
        when(accountCreatingRequestsRepository.findAccountCreatingRequestById(1L))
                .thenReturn(Optional.of(accountCreatingRequest));
        // Только один статус найден из двух
        when(socialStatusRepository.findAllById(anyList()))
                .thenReturn(List.of(socialStatus1)); // socialStatus2 не найден

        // when & then
        assertThatThrownBy(() -> accountCreatingRequestsSocialStatusService
                .createAccountCreatingRequestSocialStatus(summaryDto))
                .isInstanceOf(SocialStatusDoesNotExistException.class)
                .hasMessageContaining("Social statuses with ids not found: [2]");

        verify(accountCreatingRequestsRepository).findAccountCreatingRequestById(1L);
        verify(socialStatusRepository).findAllById(Arrays.asList(1L, 2L));
        verify(accountCreatingRequestSocialStatusRepository, never()).saveAll(any());
    }

    @Test
    void createAccountCreatingRequestSocialStatus_AllSocialStatusesNotFound_ThrowsSocialStatusDoesNotExistException() {
        // given
        when(accountCreatingRequestsRepository.findAccountCreatingRequestById(1L))
                .thenReturn(Optional.of(accountCreatingRequest));
        when(socialStatusRepository.findAllById(anyList()))
                .thenReturn(Collections.emptyList());

        // when & then
        assertThatThrownBy(() -> accountCreatingRequestsSocialStatusService
                .createAccountCreatingRequestSocialStatus(summaryDto))
                .isInstanceOf(SocialStatusDoesNotExistException.class)
                .hasMessageContaining("Social statuses with ids not found: [1, 2]");

        verify(accountCreatingRequestsRepository).findAccountCreatingRequestById(1L);
        verify(socialStatusRepository).findAllById(Arrays.asList(1L, 2L));
        verify(accountCreatingRequestSocialStatusRepository, never()).saveAll(any());
    }

    @Test
    void createAccountCreatingRequestSocialStatus_WithNullSocialStatusesList_ThrowsSocialStatusDoesNotExistException() {
        // given
        summaryDto.setSocial_statuses_id(null);

        when(accountCreatingRequestsRepository.findAccountCreatingRequestById(1L))
                .thenReturn(Optional.of(accountCreatingRequest));

        // when & then
        assertThatThrownBy(() -> accountCreatingRequestsSocialStatusService
                .createAccountCreatingRequestSocialStatus(summaryDto))
                .isInstanceOf(SocialStatusDoesNotExistException.class)
                .hasMessageContaining("Список соц статусов не может быть null");

        verify(accountCreatingRequestsRepository).findAccountCreatingRequestById(1L);
        verify(socialStatusRepository, never()).findAllById(any());
        verify(accountCreatingRequestSocialStatusRepository, never()).saveAll(any());
    }

    @Test
    void createAccountCreatingRequestSocialStatus_WithNullSocialStatusesList_ThrowsException() {
        // given
        summaryDto.setSocial_statuses_id(null);

        when(accountCreatingRequestsRepository.findAccountCreatingRequestById(1L))
                .thenReturn(Optional.of(accountCreatingRequest));

        // when & then
        assertThatThrownBy(() -> accountCreatingRequestsSocialStatusService
                .createAccountCreatingRequestSocialStatus(summaryDto))
                .isInstanceOf(Exception.class);

        verify(socialStatusRepository, never()).findAllById(any());
    }

    @Test
    void createAccountCreatingRequestSocialStatus_RepositorySaveAllThrowsException_PropagatesException() {
        // given
        when(accountCreatingRequestsRepository.findAccountCreatingRequestById(1L))
                .thenReturn(Optional.of(accountCreatingRequest));
        when(socialStatusRepository.findAllById(anyList()))
                .thenReturn(Arrays.asList(socialStatus1, socialStatus2));
        when(accountCreatingRequestSocialStatusRepository.saveAll(anyList()))
                .thenThrow(new RuntimeException("Database error"));

        // when & then
        assertThatThrownBy(() -> accountCreatingRequestsSocialStatusService
                .createAccountCreatingRequestSocialStatus(summaryDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");

        verify(accountCreatingRequestsRepository).findAccountCreatingRequestById(1L);
        verify(socialStatusRepository).findAllById(anyList());
        verify(accountCreatingRequestSocialStatusRepository).saveAll(anyList());
    }

    @Test
    void createAccountCreatingRequestSocialStatus_VerifyRelationsAreBuiltCorrectly() {
        // given
        when(accountCreatingRequestsRepository.findAccountCreatingRequestById(1L))
                .thenReturn(Optional.of(accountCreatingRequest));
        when(socialStatusRepository.findAllById(anyList()))
                .thenReturn(Arrays.asList(socialStatus1, socialStatus2));
        when(accountCreatingRequestSocialStatusRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        List<AccountCreatingRequestsSocialStatuses> result =
                accountCreatingRequestsSocialStatusService.createAccountCreatingRequestSocialStatus(summaryDto);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAccountCreatingRequest()).isEqualTo(accountCreatingRequest);
        assertThat(result.get(0).getSocialStatus()).isEqualTo(socialStatus1);
        assertThat(result.get(1).getAccountCreatingRequest()).isEqualTo(accountCreatingRequest);
        assertThat(result.get(1).getSocialStatus()).isEqualTo(socialStatus2);
    }
}
package org.example.ais_sst.services.SocialStatusesService;

import jakarta.persistence.EntityNotFoundException;
import org.example.ais_sst.dto.social_status.UserSocialStatusesDTO;
import org.example.ais_sst.entity.SocialStatus;
import org.example.ais_sst.entity.SocialStatusStudent;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.exception.SocialStatusDoesNotExistException;
import org.example.ais_sst.exception.UserDoesNotExistException;
import org.example.ais_sst.repository.SocialStatusRepository;
import org.example.ais_sst.repository.SocialStatusStudentsRepository;
import org.example.ais_sst.repository.UserRepository;
import org.example.ais_sst.service.socialStatusService.SocialStatusService;
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
class SocialStatusServiceTest {

    @Mock
    private SocialStatusRepository socialStatusRepository;

    @Mock
    private SocialStatusStudentsRepository socialStatusStudentsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SocialStatusService socialStatusService;

    private User user;
    private SocialStatus socialStatus1;
    private SocialStatus socialStatus2;
    private UserSocialStatusesDTO userSocialStatusesDTO;
    private SocialStatusStudent socialStatusStudent1;
    private SocialStatusStudent socialStatusStudent2;

    @BeforeEach
    void setUp() {
        user = User.builder()
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

        userSocialStatusesDTO = UserSocialStatusesDTO.builder()
                .userId(1L)
                .social_statuses_id(Arrays.asList(1L, 2L))
                .build();

        socialStatusStudent1 = SocialStatusStudent.builder()
                .id(1L)
                .student(user)
                .socialStatus(socialStatus1)
                .build();

        socialStatusStudent2 = SocialStatusStudent.builder()
                .id(2L)
                .student(user)
                .socialStatus(socialStatus2)
                .build();
    }

    // ==================== TESTS FOR getSocialStatuses ====================

    @Test
    void getSocialStatuses_Success() {
        // given
        List<SocialStatus> expectedStatuses = Arrays.asList(socialStatus1, socialStatus2);
        when(socialStatusRepository.findAll()).thenReturn(expectedStatuses);

        // when
        List<SocialStatus> result = socialStatusService.getSocialStatuses();

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(socialStatus1, socialStatus2);

        verify(socialStatusRepository).findAll();
    }

    @Test
    void getSocialStatuses_WhenNoStatuses_ReturnsEmptyList() {
        // given
        when(socialStatusRepository.findAll()).thenReturn(Collections.emptyList());

        // when
        List<SocialStatus> result = socialStatusService.getSocialStatuses();

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(socialStatusRepository).findAll();
    }

    @Test
    void getSocialStatuses_WhenRepositoryReturnsNull_ReturnsNull() {
        // given
        when(socialStatusRepository.findAll()).thenReturn(null);

        // when
        List<SocialStatus> result = socialStatusService.getSocialStatuses();

        // then
        assertThat(result).isNull();

        verify(socialStatusRepository).findAll();
    }

    // ==================== TESTS FOR createUserSocialStatuses ====================

    @Test
    void createUserSocialStatuses_Success() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(socialStatusRepository.findAllById(anyList()))
                .thenReturn(Arrays.asList(socialStatus1, socialStatus2));
        when(socialStatusStudentsRepository.saveAll(anyList()))
                .thenReturn(Arrays.asList(socialStatusStudent1, socialStatusStudent2));

        // when
        List<SocialStatusStudent> result = socialStatusService.createUserSocialStatuses(userSocialStatusesDTO);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSocialStatus()).isEqualTo(socialStatus1);
        assertThat(result.get(1).getSocialStatus()).isEqualTo(socialStatus2);

        verify(userRepository).findById(1L);
        verify(socialStatusRepository).findAllById(Arrays.asList(1L, 2L));
        verify(socialStatusStudentsRepository).saveAll(anyList());
    }

    @Test
    void createUserSocialStatuses_SingleSocialStatus_Success() {
        // given
        userSocialStatusesDTO.setSocial_statuses_id(List.of(1L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(socialStatusRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(socialStatus1));
        when(socialStatusStudentsRepository.saveAll(anyList()))
                .thenReturn(List.of(socialStatusStudent1));

        // when
        List<SocialStatusStudent> result = socialStatusService.createUserSocialStatuses(userSocialStatusesDTO);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSocialStatus()).isEqualTo(socialStatus1);

        verify(socialStatusRepository).findAllById(List.of(1L));
    }

    @Test
    void createUserSocialStatuses_EmptySocialStatusList_ReturnsEmptyList() {
        // given
        userSocialStatusesDTO.setSocial_statuses_id(Collections.emptyList());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        List<SocialStatusStudent> result = socialStatusService.createUserSocialStatuses(userSocialStatusesDTO);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(userRepository).findById(1L);
        verify(socialStatusRepository, never()).findAllById(any());
        verify(socialStatusStudentsRepository, never()).saveAll(any());
    }

    @Test
    void createUserSocialStatuses_NullSocialStatusList_ReturnsEmptyList() {
        // given
        userSocialStatusesDTO.setSocial_statuses_id(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        List<SocialStatusStudent> result = socialStatusService.createUserSocialStatuses(userSocialStatusesDTO);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(userRepository).findById(1L);
        verify(socialStatusRepository, never()).findAllById(any());
        verify(socialStatusStudentsRepository, never()).saveAll(any());
    }

    @Test
    void createUserSocialStatuses_UserNotFound_ThrowsUserDoesNotExistException() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> socialStatusService.createUserSocialStatuses(userSocialStatusesDTO))
                .isInstanceOf(UserDoesNotExistException.class)
                .hasMessageContaining("Пользователь с id: 1 не существует");

        verify(userRepository).findById(1L);
        verify(socialStatusRepository, never()).findAllById(any());
        verify(socialStatusStudentsRepository, never()).saveAll(any());
    }

    @Test
    void createUserSocialStatuses_SomeSocialStatusesNotFound_ThrowsSocialStatusDoesNotExistException() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(socialStatusRepository.findAllById(anyList()))
                .thenReturn(List.of(socialStatus1)); // socialStatus2 не найден

        // when & then
        assertThatThrownBy(() -> socialStatusService.createUserSocialStatuses(userSocialStatusesDTO))
                .isInstanceOf(SocialStatusDoesNotExistException.class)
                .hasMessageContaining("Социальные статусы с ids не были найдены: [2]");

        verify(userRepository).findById(1L);
        verify(socialStatusRepository).findAllById(Arrays.asList(1L, 2L));
        verify(socialStatusStudentsRepository, never()).saveAll(any());
    }

    @Test
    void createUserSocialStatuses_AllSocialStatusesNotFound_ThrowsSocialStatusDoesNotExistException() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(socialStatusRepository.findAllById(anyList()))
                .thenReturn(Collections.emptyList());

        // when & then
        assertThatThrownBy(() -> socialStatusService.createUserSocialStatuses(userSocialStatusesDTO))
                .isInstanceOf(SocialStatusDoesNotExistException.class)
                .hasMessageContaining("Социальные статусы с ids не были найдены: [1, 2]");

        verify(userRepository).findById(1L);
        verify(socialStatusRepository).findAllById(Arrays.asList(1L, 2L));
        verify(socialStatusStudentsRepository, never()).saveAll(any());
    }

    @Test
    void createUserSocialStatuses_RepositorySaveAllThrowsException_PropagatesException() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(socialStatusRepository.findAllById(anyList()))
                .thenReturn(Arrays.asList(socialStatus1, socialStatus2));
        when(socialStatusStudentsRepository.saveAll(anyList()))
                .thenThrow(new RuntimeException("Database error"));

        // when & then
        assertThatThrownBy(() -> socialStatusService.createUserSocialStatuses(userSocialStatusesDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");

        verify(userRepository).findById(1L);
        verify(socialStatusRepository).findAllById(anyList());
        verify(socialStatusStudentsRepository).saveAll(anyList());
    }

    @Test
    void createUserSocialStatuses_VerifyRelationsAreBuiltCorrectly() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(socialStatusRepository.findAllById(anyList()))
                .thenReturn(Arrays.asList(socialStatus1, socialStatus2));
        when(socialStatusStudentsRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        List<SocialStatusStudent> result = socialStatusService.createUserSocialStatuses(userSocialStatusesDTO);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStudent()).isEqualTo(user);
        assertThat(result.get(0).getSocialStatus()).isEqualTo(socialStatus1);
        assertThat(result.get(1).getStudent()).isEqualTo(user);
        assertThat(result.get(1).getSocialStatus()).isEqualTo(socialStatus2);
    }

    @Test
    void createUserSocialStatuses_WithNullDto_ThrowsNullPointerException() {
        // when & then
        assertThatThrownBy(() -> socialStatusService.createUserSocialStatuses(null))
                .isInstanceOf(NullPointerException.class);

        verify(userRepository, never()).findById(any());
        verify(socialStatusRepository, never()).findAllById(any());
        verify(socialStatusStudentsRepository, never()).saveAll(any());
    }

    @Test
    void createUserSocialStatuses_WithNullUserId_ThrowsUserDoesNotExistException() {
        // given
        userSocialStatusesDTO.setUserId(null);

        // when & then
        assertThatThrownBy(() -> socialStatusService.createUserSocialStatuses(userSocialStatusesDTO))
                .isInstanceOf(UserDoesNotExistException.class)
                .hasMessageContaining("Пользователь с id: null не существует");

        verify(userRepository).findById(null);
        verify(socialStatusRepository, never()).findAllById(any());
        verify(socialStatusStudentsRepository, never()).saveAll(any());
    }

    @Test
    void createUserSocialStatuses_DuplicateSocialStatuses_RemovesDuplicates() {
        // given
        userSocialStatusesDTO.setSocial_statuses_id(Arrays.asList(1L, 1L, 2L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        // findAllById вызывается с уникальными значениями (без дубликатов)
        when(socialStatusRepository.findAllById(Arrays.asList(1L, 2L)))
                .thenReturn(Arrays.asList(socialStatus1, socialStatus2));
        when(socialStatusStudentsRepository.saveAll(anyList()))
                .thenReturn(Arrays.asList(socialStatusStudent1, socialStatusStudent2));

        // when
        List<SocialStatusStudent> result = socialStatusService.createUserSocialStatuses(userSocialStatusesDTO);

        // then
        assertThat(result).hasSize(2);

        // Проверяем, что findAllById был вызван с уникальными значениями
        verify(socialStatusRepository).findAllById(Arrays.asList(1L, 2L));
    }

}
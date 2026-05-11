package org.example.ais_sst.services.AccountCreatingRequests;

import org.example.ais_sst.dto.account_request.AccountCreatingRequestFilterDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestRejectDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestResponseDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestsSummaryDTO;
import org.example.ais_sst.dto.user.UserSummaryDTO;
import org.example.ais_sst.entity.*;
import org.example.ais_sst.entity.enums.AccountCreatingRequestStatus;
import org.example.ais_sst.entity.enums.Gender;
import org.example.ais_sst.exception.*;
import org.example.ais_sst.mapper.AccountCreatingRequestMapper;
import org.example.ais_sst.mapper.UserMapper;
import org.example.ais_sst.repository.*;
import org.example.ais_sst.service.accountCreatingRequestsService.AccountCreatingRequestsService;
import org.example.ais_sst.service.accountCreatingRequestsService.AccountRequestPhotoService;
import org.example.ais_sst.service.socialStatusService.AccountCreatingRequestsSocialStatusService;
import org.example.ais_sst.service.socialStatusService.SocialStatusService;
import org.example.ais_sst.utils.ImageUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountCreatingRequestsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private SpecialityRepository specialityRepository;

    @Mock
    private AccountCreatingRequestsSocialStatusService accountCreatingRequestsSocialStatusService;

    @Mock
    private SocialStatusService socialStatusService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccountCreatingRequestsRepository accountCreatingRequestsRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AccountCreatingRequestSocialStatusRepository accountCreatingRequestsSocialStatusesRepository;

    @Mock
    private SectorParticipantRepository sectorParticipantRepository;

    @Mock
    private AccountCreatingRequestMapper requestMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AccountRequestPhotoService accountRequestPhotoService; // Добавлено

    @InjectMocks
    private AccountCreatingRequestsService accountCreatingRequestsService;

    private AccountCreatingRequestsSummaryDTO summaryDto;
    private AccountCreatingRequest accountCreatingRequest;
    private Group group;
    private Speciality speciality;
    private Role role;
    private User user;
    private AccountCreatingRequestRejectDTO rejectDto;

    @BeforeEach
    void setUp() {
        group = Group.builder()
                .id(1L)
                .title("ПИ-101")
                .build();

        speciality = Speciality.builder()
                .id(1L)
                .title("Программная инженерия")
                .build();

        role = Role.builder()
                .id(1L)
                .title("Activist")
                .build();

        summaryDto = AccountCreatingRequestsSummaryDTO.builder()
                .name("Иван")
                .surname("Иванов")
                .patronymic("Иванович")
                .gender(String.valueOf(Gender.Мужчина))
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .studentEmail("ivan@test.com")
                .phoneNumber("+79991234567")
                .password("password123")
                .studentIdNumber(12345)
                .courseNumber((short) 3)
                .group_id(1L)
                .speciality_id(1L)
                .photo("base64photo")
                .build();

        accountCreatingRequest = AccountCreatingRequest.builder()
                .id(1L)
                .name("Иван")
                .surname("Иванов")
                .patronymic("Иванович")
                .gender(Gender.Мужчина)
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .studentEmail("ivan@test.com")
                .phoneNumber("+79991234567")
                .password("encodedPassword")
                .studentIdNumber(12345)
                .courseNumber((short) 3)
                .group(group)
                .speciality(speciality)
                .status(AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ)
                .createdAt(LocalDateTime.now())
                .build();

        user = User.builder()
                .id(1L)
                .name("Иван")
                .surname("Иванов")
                .patronymic("Иванович")
                .studentEmail("ivan@test.com")
                .phoneNumber("+79991234567")
                .role(role)
                .group(group)
                .speciality(speciality)
                .isActive(true)
                .isBanned(false)
                .build();

        rejectDto = AccountCreatingRequestRejectDTO.builder()
                .rejectionReason("Недостаточно данных")
                .build();
    }

    @Test
    void createAccountRequest_Success() {
        // given
        when(userRepository.existsByStudentEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(groupRepository.findGroupById(1L)).thenReturn(Optional.of(group));
        when(specialityRepository.findSpecialityById(1L)).thenReturn(Optional.of(speciality));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(requestMapper.toEntity(any(AccountCreatingRequestsSummaryDTO.class))).thenReturn(accountCreatingRequest);
        when(accountCreatingRequestsRepository.save(any(AccountCreatingRequest.class))).thenReturn(accountCreatingRequest);

        // Исправление: используем when().thenReturn() вместо doNothing()
        // Предполагаем, что метод возвращает какой-то объект (например, созданный объект)
        when(accountCreatingRequestsSocialStatusService.createAccountCreatingRequestSocialStatus(any()))
                .thenReturn(null); // или .thenReturn(someObject)

        // when
        AccountCreatingRequest result = accountCreatingRequestsService.createAccountRequest(summaryDto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ);

        verify(userRepository).existsByStudentEmail("ivan@test.com");
        verify(userRepository).existsByPhoneNumber("+79991234567");
        verify(groupRepository).findGroupById(1L);
        verify(specialityRepository).findSpecialityById(1L);
        verify(passwordEncoder).encode("password123");
        verify(accountCreatingRequestsRepository).save(any(AccountCreatingRequest.class));
        verify(accountCreatingRequestsSocialStatusService).createAccountCreatingRequestSocialStatus(any());
    }

    @Test
    void createAccountRequest_EmailAlreadyExists_ThrowsException() {
        // given
        when(userRepository.existsByStudentEmail(anyString())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> accountCreatingRequestsService.createAccountRequest(summaryDto))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Email уже используется");

        verify(userRepository).existsByStudentEmail("ivan@test.com");
        verify(userRepository, never()).existsByPhoneNumber(anyString());
        verify(accountCreatingRequestsRepository, never()).save(any());
    }

    @Test
    void createAccountRequest_PhoneAlreadyExists_ThrowsException() {
        // given
        when(userRepository.existsByStudentEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> accountCreatingRequestsService.createAccountRequest(summaryDto))
                .isInstanceOf(PhoneAlreadyExistException.class)
                .hasMessageContaining("Телефон уже используется");

        verify(userRepository).existsByStudentEmail("ivan@test.com");
        verify(userRepository).existsByPhoneNumber("+79991234567");
        verify(accountCreatingRequestsRepository, never()).save(any());
    }

    @Test
    void createAccountRequest_GroupNotFound_ThrowsException() {
        // given
        when(userRepository.existsByStudentEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(groupRepository.findGroupById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> accountCreatingRequestsService.createAccountRequest(summaryDto))
                .isInstanceOf(GroupDoesNotExistException.class)
                .hasMessageContaining("Группа с id: 1 не существует");
    }

    @Test
    void createAccountRequest_SpecialityNotFound_ThrowsException() {
        // given
        when(userRepository.existsByStudentEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(groupRepository.findGroupById(1L)).thenReturn(Optional.of(group));
        when(specialityRepository.findSpecialityById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> accountCreatingRequestsService.createAccountRequest(summaryDto))
                .isInstanceOf(SpecialityDoesNotExistException.class)
                .hasMessageContaining("Специальность с id: 1 не существует");
    }

    @Test
    void rejectAccountRequest_Success() {
        // given
        when(accountCreatingRequestsRepository.findAccountCreatingRequestById(1L))
                .thenReturn(Optional.of(accountCreatingRequest));
        when(accountCreatingRequestsRepository.save(any(AccountCreatingRequest.class)))
                .thenReturn(accountCreatingRequest);
        when(requestMapper.toResponseDto(any(AccountCreatingRequest.class), any(AccountRequestPhotoService.class)))
                .thenReturn(AccountCreatingRequestResponseDTO.builder().id(1L).build());

        // when
        AccountCreatingRequestResponseDTO result = accountCreatingRequestsService.rejectAccountRequest(1L, rejectDto);

        // then
        assertThat(result).isNotNull();
        assertThat(accountCreatingRequest.getStatus()).isEqualTo(AccountCreatingRequestStatus.ОТКЛОНЕНА);
        assertThat(accountCreatingRequest.getReasonForRefusal()).isEqualTo("Недостаточно данных");

        verify(accountCreatingRequestsRepository).findAccountCreatingRequestById(1L);
        verify(accountCreatingRequestsRepository).save(accountCreatingRequest);
    }

    @Test
    void rejectAccountRequest_RequestNotFound_ThrowsException() {
        // given
        when(accountCreatingRequestsRepository.findAccountCreatingRequestById(1L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> accountCreatingRequestsService.rejectAccountRequest(1L, rejectDto))
                .isInstanceOf(AccountCreatingRequestDoesNotExistException.class)
                .hasMessageContaining("Заявка с id: 1 не существует");
    }

    @Test
    void rejectAccountRequest_AlreadyProcessed_ThrowsException() {
        // given
        accountCreatingRequest.setStatus(AccountCreatingRequestStatus.ОДОБРЕНА);
        when(accountCreatingRequestsRepository.findAccountCreatingRequestById(1L))
                .thenReturn(Optional.of(accountCreatingRequest));

        // when & then
        assertThatThrownBy(() -> accountCreatingRequestsService.rejectAccountRequest(1L, rejectDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Заявка с id: 1 уже обработана");
    }

    @Test
    void acceptAccountRequest_Success() {
        // given
        when(accountCreatingRequestsRepository.findAccountCreatingRequestById(1L))
                .thenReturn(Optional.of(accountCreatingRequest));
        when(roleRepository.findByTitle("Activist")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(accountCreatingRequestsSocialStatusService.getSocialStatusIdsByRequestId(1L))
                .thenReturn(Arrays.asList(1L, 2L));

        // Исправление: используем when().thenReturn() вместо doNothing()
        when(socialStatusService.createUserSocialStatuses(any())).thenReturn(null);

        when(accountCreatingRequestsRepository.save(any(AccountCreatingRequest.class)))
                .thenReturn(accountCreatingRequest);
        when(userMapper.toDto(any(User.class))).thenReturn(UserSummaryDTO.builder().id(1L).build());

        // when
        UserSummaryDTO result = accountCreatingRequestsService.acceptAccountRequest(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(accountCreatingRequest.getStatus()).isEqualTo(AccountCreatingRequestStatus.ОДОБРЕНА);

        verify(accountCreatingRequestsRepository).findAccountCreatingRequestById(1L);
        verify(roleRepository).findByTitle("Activist");
        verify(userRepository).save(any(User.class));
        verify(socialStatusService).createUserSocialStatuses(any());
        verify(accountCreatingRequestsRepository).save(accountCreatingRequest);
    }

    @Test
    void getRequests_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<AccountCreatingRequest> requestPage = new PageImpl<>(List.of(accountCreatingRequest));

        when(accountCreatingRequestsRepository.findAll(pageable)).thenReturn(requestPage);
        when(requestMapper.toResponseDto(any(AccountCreatingRequest.class), any(AccountRequestPhotoService.class)))
                .thenReturn(AccountCreatingRequestResponseDTO.builder().id(1L).build());
        when(accountCreatingRequestsSocialStatusesRepository.findSocialStatusTitlesByRequestId(1L))
                .thenReturn(List.of("Студент", "Активист"));

        // when
        Page<AccountCreatingRequestResponseDTO> result = accountCreatingRequestsService.getRequests(pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSocialStatuses()).contains("Студент", "Активист");

        verify(accountCreatingRequestsRepository).findAll(pageable);
        verify(accountCreatingRequestsSocialStatusesRepository).findSocialStatusTitlesByRequestId(1L);
    }

    @Test
    void getPendingRequests_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<AccountCreatingRequest> requestPage = new PageImpl<>(List.of(accountCreatingRequest));

        when(accountCreatingRequestsRepository.findByStatus(AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ, pageable))
                .thenReturn(requestPage);
        when(requestMapper.toResponseDto(any(AccountCreatingRequest.class), any(AccountRequestPhotoService.class)))
                .thenReturn(AccountCreatingRequestResponseDTO.builder().id(1L).build());
        when(accountCreatingRequestsSocialStatusesRepository.findSocialStatusTitlesByRequestId(1L))
                .thenReturn(List.of("Студент"));

        // when
        Page<AccountCreatingRequestResponseDTO> result = accountCreatingRequestsService.getPendingRequests(pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(accountCreatingRequestsRepository).findByStatus(AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ, pageable);
    }

    @Test
    void getRequestsWithFilters_Success() {
        // given
        AccountCreatingRequestFilterDTO filter = AccountCreatingRequestFilterDTO.builder()
                .name("Иван")
                .status(AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ)
                .build();

        Object[] row = new Object[]{
                1L, "Иван", "Иванов", "Иванович", "Мужчина",
                java.sql.Date.valueOf("2000-01-01"), 3, 12345,
                "ivan@test.com", "+79991234567", null, "На рассмотрении",
                java.sql.Timestamp.valueOf(LocalDateTime.now()),
                java.sql.Timestamp.valueOf(LocalDateTime.now()),
                1L, "ПИ-101", 1L, "Программная инженерия", new byte[0]
        };

        List<Object[]> rowList = new java.util.ArrayList<>();
        rowList.add(row);

        when(accountCreatingRequestsRepository.findAllWithFiltersNative(any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(rowList);
        when(accountCreatingRequestsRepository.countAllWithFiltersNative(any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1L);
        when(accountCreatingRequestsSocialStatusesRepository.findSocialStatusTitlesByRequestId(1L))
                .thenReturn(List.of("Студент"));

        // when
        Page<AccountCreatingRequestResponseDTO> result = accountCreatingRequestsService.getRequestsWithFilters(
                filter, 0, 10, "id", "ASC");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(accountCreatingRequestsRepository).findAllWithFiltersNative(any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), eq(0), eq(10));
        verify(accountCreatingRequestsRepository).countAllWithFiltersNative(any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
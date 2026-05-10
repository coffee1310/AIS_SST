package org.example.ais_sst.services.SectorsService;

import org.example.ais_sst.dto.request.SectorIntroductionRequestDTO;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTOSummary;
import org.example.ais_sst.dto.sector.SectorParticipantDTO;
import org.example.ais_sst.entity.*;
import org.example.ais_sst.entity.enums.SectorIntroductionStatus;
import org.example.ais_sst.entity.enums.SectorParticipantStatuses;
import org.example.ais_sst.exception.*;
import org.example.ais_sst.mapper.SectorIntroductionRequestMapper;
import org.example.ais_sst.mapper.SectorMapper;
import org.example.ais_sst.mapper.SectorParticipantMapper;
import org.example.ais_sst.repository.SectorIntroductionRequestRepository;
import org.example.ais_sst.repository.SectorParticipantRepository;
import org.example.ais_sst.repository.SectorRepository;
import org.example.ais_sst.repository.UserRepository;
import org.example.ais_sst.service.sectorService.SectorIntroductionRequestService;
import org.example.ais_sst.service.sectorService.SectorParticipantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectorIntroductionRequestServiceTest {

    @Mock
    private SectorIntroductionRequestMapper sectorIntroductionRequestMapper;

    @Mock
    private SectorIntroductionRequestRepository sectorIntroductionRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private SectorParticipantRepository sectorParticipantRepository;

    @Mock
    private SectorParticipantMapper sectorParticipantMapper;

    @Mock
    private SectorParticipantService sectorParticipantService;

    @Mock
    private SectorMapper sectorMapper;

    @InjectMocks
    private SectorIntroductionRequestService sectorIntroductionRequestService;

    private User user;
    private Sector sector;
    private SectorIntroductionRequest request;
    private SectorParticipant sectorParticipant;
    private SectorParticipant coordinatorParticipant;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Иван")
                .surname("Иванов")
                .studentEmail("ivan@test.com")
                .build();

        sector = Sector.builder()
                .id(1L)
                .title("Спортивный сектор")
                .description("Описание")
                .isActive(true)
                .build();

        request = SectorIntroductionRequest.builder()
                .id(1L)
                .user(user)
                .sector(sector)
                .status(SectorIntroductionStatus.НА_РАССМОТРЕНИИ)
                .build();

        sectorParticipant = SectorParticipant.builder()
                .id(1L)
                .sector(sector)
                .student(user)
                .isCoordinator(false)
                .status(SectorParticipantStatuses.Активный)
                .entryDate(LocalDate.now())
                .build();

        coordinatorParticipant = SectorParticipant.builder()
                .id(2L)
                .sector(sector)
                .student(user)
                .isCoordinator(true)
                .status(SectorParticipantStatuses.Активный)
                .entryDate(LocalDate.now())
                .build();
    }

    // ==================== TESTS FOR createRequest ====================

    @Test
    void createRequest_Success() {
        // given
        when(userRepository.findUserById(1L)).thenReturn(Optional.of(user));
        when(sectorRepository.findSectorById(1L)).thenReturn(Optional.of(sector));
        when(sectorIntroductionRequestRepository.findByUserIdAndSectorIdAndStatusIn(eq(1L), eq(1L), any()))
                .thenReturn(new ArrayList<>());
        when(sectorParticipantRepository.findByStudentIdAndSectorId(1L, 1L))
                .thenReturn(Optional.empty());
        when(sectorIntroductionRequestRepository.save(any(SectorIntroductionRequest.class))).thenReturn(request);
        when(sectorIntroductionRequestMapper.toSectorIntroductionRequestDTO(any(SectorIntroductionRequest.class)))
                .thenReturn(SectorIntroductionRequestDTO.builder().id(1L).build());

        // when
        SectorIntroductionRequestDTO result = sectorIntroductionRequestService.createRequest(1L, 1L);

        // then
        assertThat(result).isNotNull();
        verify(sectorIntroductionRequestRepository).save(any(SectorIntroductionRequest.class));
    }

    @Test
    void createRequest_UserNotFound_ThrowsException() {
        // given
        when(userRepository.findUserById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sectorIntroductionRequestService.createRequest(1L, 1L))
                .isInstanceOf(UserDoesNotExistException.class)
                .hasMessageContaining("Пользователь с id: 1 не найден");
    }

    @Test
    void createRequest_SectorNotFound_ThrowsException() {
        // given
        when(userRepository.findUserById(1L)).thenReturn(Optional.of(user));
        when(sectorRepository.findSectorById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sectorIntroductionRequestService.createRequest(1L, 1L))
                .isInstanceOf(SectorDoesNotExistException.class)
                .hasMessageContaining("Сектор с таким id: 1 не найден");
    }

    @Test
    void createRequest_ExistingActiveRequest_ThrowsException() {
        // given
        when(userRepository.findUserById(1L)).thenReturn(Optional.of(user));
        when(sectorRepository.findSectorById(1L)).thenReturn(Optional.of(sector));
        when(sectorIntroductionRequestRepository.findByUserIdAndSectorIdAndStatusIn(eq(1L), eq(1L), any()))
                .thenReturn(List.of(request));

        // when & then
        assertThatThrownBy(() -> sectorIntroductionRequestService.createRequest(1L, 1L))
                .isInstanceOf(SectorIntroductionRequestAlreadyExistsException.class)
                .hasMessageContaining("У вас уже есть активная заявка");
    }

    @Test
    void createRequest_UserAlreadyActiveParticipant_ThrowsException() {
        // given
        when(userRepository.findUserById(1L)).thenReturn(Optional.of(user));
        when(sectorRepository.findSectorById(1L)).thenReturn(Optional.of(sector));
        when(sectorIntroductionRequestRepository.findByUserIdAndSectorIdAndStatusIn(eq(1L), eq(1L), any()))
                .thenReturn(new ArrayList<>());
        when(sectorParticipantRepository.findByStudentIdAndSectorId(1L, 1L))
                .thenReturn(Optional.of(sectorParticipant));

        // when & then
        assertThatThrownBy(() -> sectorIntroductionRequestService.createRequest(1L, 1L))
                .isInstanceOf(UserIsAlreadyInThisSectorException.class)
                .hasMessageContaining("уже является активным участником сектора");
    }

    @Test
    void createRequest_RestoreFormerParticipant_Success() {
        // given
        SectorParticipant formerParticipant = SectorParticipant.builder()
                .id(1L)
                .sector(sector)
                .student(user)
                .isCoordinator(false)
                .status(SectorParticipantStatuses.Вышедший)
                .build();

        when(userRepository.findUserById(1L)).thenReturn(Optional.of(user));
        when(sectorRepository.findSectorById(1L)).thenReturn(Optional.of(sector));
        when(sectorIntroductionRequestRepository.findByUserIdAndSectorIdAndStatusIn(eq(1L), eq(1L), any()))
                .thenReturn(new ArrayList<>());
        when(sectorParticipantRepository.findByStudentIdAndSectorId(1L, 1L))
                .thenReturn(Optional.of(formerParticipant));
        when(sectorIntroductionRequestRepository.save(any(SectorIntroductionRequest.class))).thenReturn(request);
        when(sectorIntroductionRequestMapper.toSectorIntroductionRequestDTO(any(SectorIntroductionRequest.class)))
                .thenReturn(SectorIntroductionRequestDTO.builder().id(1L).build());

        // when
        SectorIntroductionRequestDTO result = sectorIntroductionRequestService.createRequest(1L, 1L);

        // then
        assertThat(result).isNotNull();
        verify(sectorIntroductionRequestRepository).save(any(SectorIntroductionRequest.class));
    }

    // ==================== TESTS FOR acceptRequest ====================

    @Test
    void acceptRequest_Success_NewParticipant() {
        // given
        when(sectorIntroductionRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(sectorParticipantRepository.findByStudentIdAndSectorId(1L, 1L))
                .thenReturn(Optional.empty());
        when(sectorParticipantService.createParticipant(request)).thenReturn(sectorParticipant);
        when(sectorParticipantMapper.toSectorParticipantDTO(any(SectorParticipant.class)))
                .thenReturn(SectorParticipantDTO.builder().id(1L).build());
        when(sectorIntroductionRequestRepository.save(any(SectorIntroductionRequest.class))).thenReturn(request);
        when(sectorIntroductionRequestRepository.findByUserIdAndSectorIdAndStatusIn(eq(1L), eq(1L), any()))
                .thenReturn(new ArrayList<>());
        when(sectorIntroductionRequestMapper.toSummary(any(SectorIntroductionRequest.class)))
                .thenReturn(SectorIntroductionRequestDTOSummary.builder().id(1L).build());

        // when
        SectorIntroductionRequestDTOSummary result = sectorIntroductionRequestService.acceptRequest(1L);

        // then
        assertThat(result).isNotNull();
        verify(sectorParticipantService).createParticipant(request);
        verify(sectorIntroductionRequestRepository).save(request);
    }

    @Test
    void acceptRequest_Success_RestoreFormerParticipant() {
        // given
        SectorParticipant formerParticipant = SectorParticipant.builder()
                .id(1L)
                .sector(sector)
                .student(user)
                .isCoordinator(false)
                .status(SectorParticipantStatuses.Вышедший)
                .build();

        when(sectorIntroductionRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(sectorParticipantRepository.findByStudentIdAndSectorId(1L, 1L))
                .thenReturn(Optional.of(formerParticipant));
        when(sectorParticipantRepository.save(any(SectorParticipant.class))).thenReturn(formerParticipant);
        when(sectorParticipantMapper.toSectorParticipantDTO(any(SectorParticipant.class)))
                .thenReturn(SectorParticipantDTO.builder().id(1L).build());
        when(sectorIntroductionRequestRepository.save(any(SectorIntroductionRequest.class))).thenReturn(request);
        when(sectorIntroductionRequestRepository.findByUserIdAndSectorIdAndStatusIn(eq(1L), eq(1L), any()))
                .thenReturn(new ArrayList<>());
        when(sectorIntroductionRequestMapper.toSummary(any(SectorIntroductionRequest.class)))
                .thenReturn(SectorIntroductionRequestDTOSummary.builder().id(1L).build());

        // when
        SectorIntroductionRequestDTOSummary result = sectorIntroductionRequestService.acceptRequest(1L);

        // then
        assertThat(result).isNotNull();
        verify(sectorParticipantRepository).save(any(SectorParticipant.class));
        verify(sectorParticipantService, never()).createParticipant(any());
    }

    @Test
    void acceptRequest_RequestNotFound_ThrowsException() {
        // given
        when(sectorIntroductionRequestRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sectorIntroductionRequestService.acceptRequest(1L))
                .isInstanceOf(SectorIntroductionRequestDoesNotExistException.class)
                .hasMessageContaining("Заявка на вступление в сектор с id: 1 не найдена");
    }

    @Test
    void acceptRequest_RequestAlreadyRejected_ThrowsException() {
        // given
        request.setStatus(SectorIntroductionStatus.ОТКЛОНЕНА);
        when(sectorIntroductionRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        // when & then
        assertThatThrownBy(() -> sectorIntroductionRequestService.acceptRequest(1L))
                .isInstanceOf(SectorIntroductionRequestAlreadyProcessedException.class)
                .hasMessageContaining("Заявка уже обработана");
    }

    @Test
    void acceptRequest_UserAlreadyActiveParticipant_ThrowsException() {
        // given
        when(sectorIntroductionRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(sectorParticipantRepository.findByStudentIdAndSectorId(1L, 1L))
                .thenReturn(Optional.of(sectorParticipant));

        // when & then
        assertThatThrownBy(() -> sectorIntroductionRequestService.acceptRequest(1L))
                .isInstanceOf(UserIsAlreadyInThisSectorException.class)
                .hasMessageContaining("уже является активным участником сектора");
    }

    // ==================== TESTS FOR rejectRequest ====================

    @Test
    void rejectRequest_Success() {
        // given
        when(sectorIntroductionRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(sectorIntroductionRequestRepository.save(any(SectorIntroductionRequest.class))).thenReturn(request);
        when(sectorIntroductionRequestMapper.toSummary(any(SectorIntroductionRequest.class)))
                .thenReturn(SectorIntroductionRequestDTOSummary.builder().id(1L).build());

        // when
        SectorIntroductionRequestDTOSummary result = sectorIntroductionRequestService.rejectRequest(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(request.getStatus()).isEqualTo(SectorIntroductionStatus.ОТКЛОНЕНА);
        verify(sectorIntroductionRequestRepository).save(request);
    }

    @Test
    void rejectRequest_RequestNotFound_ThrowsException() {
        // given
        when(sectorIntroductionRequestRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sectorIntroductionRequestService.rejectRequest(1L))
                .isInstanceOf(SectorIntroductionRequestDoesNotExistException.class);
    }

    @Test
    void rejectRequest_RequestAlreadyApproved_ThrowsException() {
        // given
        request.setStatus(SectorIntroductionStatus.ОДОБРЕНА);
        when(sectorIntroductionRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        // when & then
        assertThatThrownBy(() -> sectorIntroductionRequestService.rejectRequest(1L))
                .isInstanceOf(SectorIntroductionRequestAlreadyProcessedException.class);
    }

    // ==================== TESTS FOR getRequestsListByCoordinator ====================

    @Test
    void getRequestsListByCoordinator_Success() {
        // given
        when(sectorParticipantRepository.findSectorsWhereUserIsCoordinator(1L))
                .thenReturn(List.of(coordinatorParticipant));
        when(sectorIntroductionRequestRepository.getSectorIntroductionRequestsBySector_Id(1L))
                .thenReturn(List.of(request));
        when(sectorIntroductionRequestMapper.toSectorIntroductionRequestDTO(any(SectorIntroductionRequest.class)))
                .thenReturn(SectorIntroductionRequestDTO.builder().id(1L).build());

        // when
        List<SectorIntroductionRequestDTO> result = sectorIntroductionRequestService.getRequestsListByCoordinator(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(sectorIntroductionRequestRepository).getSectorIntroductionRequestsBySector_Id(1L);
    }

    @Test
    void getRequestsListByCoordinator_UserNotCoordinator_ReturnsEmptyList() {
        // given
        when(sectorParticipantRepository.findSectorsWhereUserIsCoordinator(1L))
                .thenReturn(new ArrayList<>());

        // when
        List<SectorIntroductionRequestDTO> result = sectorIntroductionRequestService.getRequestsListByCoordinator(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(sectorIntroductionRequestRepository, never()).getSectorIntroductionRequestsBySector_Id(any());
    }

    @Test
    void getRequestsListByCoordinator_MultipleSectors_ReturnsAllRequests() {
        // given
        Sector sector2 = Sector.builder().id(2L).title("Второй сектор").build();
        SectorParticipant coordinatorParticipant2 = SectorParticipant.builder()
                .id(3L)
                .sector(sector2)
                .student(user)
                .isCoordinator(true)
                .build();

        SectorIntroductionRequest request2 = SectorIntroductionRequest.builder()
                .id(2L)
                .user(user)
                .sector(sector2)
                .build();

        when(sectorParticipantRepository.findSectorsWhereUserIsCoordinator(1L))
                .thenReturn(List.of(coordinatorParticipant, coordinatorParticipant2));
        when(sectorIntroductionRequestRepository.getSectorIntroductionRequestsBySector_Id(1L))
                .thenReturn(List.of(request));
        when(sectorIntroductionRequestRepository.getSectorIntroductionRequestsBySector_Id(2L))
                .thenReturn(List.of(request2));
        when(sectorIntroductionRequestMapper.toSectorIntroductionRequestDTO(any(SectorIntroductionRequest.class)))
                .thenReturn(SectorIntroductionRequestDTO.builder().id(1L).build())
                .thenReturn(SectorIntroductionRequestDTO.builder().id(2L).build());

        // when
        List<SectorIntroductionRequestDTO> result = sectorIntroductionRequestService.getRequestsListByCoordinator(1L);

        // then
        assertThat(result).hasSize(2);
        verify(sectorIntroductionRequestRepository).getSectorIntroductionRequestsBySector_Id(1L);
        verify(sectorIntroductionRequestRepository).getSectorIntroductionRequestsBySector_Id(2L);
    }

    // ==================== TESTS FOR getRequestsListByCoordinatorWithStatus ====================

    @Test
    void getRequestsListByCoordinatorWithStatus_Success() {
        // given
        when(sectorParticipantRepository.findSectorsWhereUserIsCoordinator(1L))
                .thenReturn(List.of(coordinatorParticipant));
        when(sectorIntroductionRequestRepository.findRequestsByCoordinatorIdAndStatus(eq(1L), anyString()))
                .thenReturn(List.of(request));
        when(sectorIntroductionRequestMapper.toSectorIntroductionRequestDTO(any(SectorIntroductionRequest.class)))
                .thenReturn(SectorIntroductionRequestDTO.builder().id(1L).build());

        // when
        List<SectorIntroductionRequestDTO> result = sectorIntroductionRequestService
                .getRequestsListByCoordinatorWithStatus(1L, SectorIntroductionStatus.НА_РАССМОТРЕНИИ);

        // then
        assertThat(result).hasSize(1);
        verify(sectorIntroductionRequestRepository).findRequestsByCoordinatorIdAndStatus(eq(1L), anyString());
    }

    @Test
    void getRequestsListByCoordinatorWithStatus_UserNotCoordinator_ReturnsEmptyList() {
        // given
        when(sectorParticipantRepository.findSectorsWhereUserIsCoordinator(1L))
                .thenReturn(new ArrayList<>());

        // when
        List<SectorIntroductionRequestDTO> result = sectorIntroductionRequestService
                .getRequestsListByCoordinatorWithStatus(1L, SectorIntroductionStatus.НА_РАССМОТРЕНИИ);

        // then
        assertThat(result).isEmpty();
        verify(sectorIntroductionRequestRepository, never()).findRequestsByCoordinatorIdAndStatus(any(), any());
    }

    @Test
    void getRequestsListByCoordinatorWithStatus_NullStatus_ReturnsAllRequests() {
        // given
        when(sectorParticipantRepository.findSectorsWhereUserIsCoordinator(1L))
                .thenReturn(List.of(coordinatorParticipant));
        when(sectorIntroductionRequestRepository.findRequestsByCoordinatorId(1L))
                .thenReturn(List.of(request));
        when(sectorIntroductionRequestMapper.toSectorIntroductionRequestDTO(any(SectorIntroductionRequest.class)))
                .thenReturn(SectorIntroductionRequestDTO.builder().id(1L).build());

        // when
        List<SectorIntroductionRequestDTO> result = sectorIntroductionRequestService
                .getRequestsListByCoordinatorWithStatus(1L, null);

        // then
        assertThat(result).hasSize(1);
        verify(sectorIntroductionRequestRepository).findRequestsByCoordinatorId(1L);
        verify(sectorIntroductionRequestRepository, never()).findRequestsByCoordinatorIdAndStatus(any(), any());
    }
}
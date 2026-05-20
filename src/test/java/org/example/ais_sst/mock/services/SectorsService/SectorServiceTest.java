package org.example.ais_sst.mock.services.SectorsService;

import org.example.ais_sst.dto.sector.SectorDTO;
import org.example.ais_sst.dto.sector.SectorParticipantResponseDTO;
import org.example.ais_sst.dto.sector.SectorWithUserStatusDTO;
import org.example.ais_sst.entity.*;
import org.example.ais_sst.entity.enums.SectorIntroductionStatus;
import org.example.ais_sst.entity.enums.SectorParticipantStatuses;
import org.example.ais_sst.exception.SectorDoesNotExistException;
import org.example.ais_sst.exception.UserDoesNotExistException;
import org.example.ais_sst.mapper.SectorMapper;
import org.example.ais_sst.mapper.SectorParticipantMapper;
import org.example.ais_sst.mapper.converter.SectorWithUserStatusConverter;
import org.example.ais_sst.repository.*;
import org.example.ais_sst.service.sectorService.SectorPhotoService;
import org.example.ais_sst.service.sectorService.SectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.management.relation.RoleNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectorServiceTest {

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private SectorMapper sectorMapper;

    @Mock
    private SectorParticipantMapper sectorParticipantMapper;

    @Mock
    private SectorWithUserStatusConverter sectorWithUserStatusConverter;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SectorParticipantRepository sectorParticipantRepository;

    @Mock
    private SectorIntroductionRequestRepository sectorIntroductionRequestRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private SectorPhotoService sectorPhotoService;

    @InjectMocks
    private SectorService sectorService;

    private Sector sector;
    private SectorDTO sectorDTO;
    private User user;
    private User coordinator;
    private User participant;
    private SectorParticipant sectorParticipant;
    private SectorParticipant coordinatorParticipant;
    private SectorParticipant nonCoordinatorParticipant;
    private Role coordinatorRole;
    private Role activistRole;
    private SectorIntroductionRequest introductionRequest;

    @BeforeEach
    void setUp() {
        sector = Sector.builder()
                .id(1L)
                .title("Спортивный сектор")
                .description("Описание спортивного сектора")
                .isActive(true)
                .build();

        sectorDTO = SectorDTO.builder()
                .id(1L)
                .title("Спортивный сектор")
                .description("Описание спортивного сектора")
                .build();

        user = User.builder()
                .id(1L)
                .name("Иван")
                .surname("Иванов")
                .patronymic("Иванович")
                .studentEmail("ivan@test.com")
                .build();

        coordinator = User.builder()
                .id(2L)
                .name("Петр")
                .surname("Петров")
                .patronymic("Петрович")
                .studentEmail("petr@test.com")
                .build();

        participant = User.builder()
                .id(3L)
                .name("Сидор")
                .surname("Сидоров")
                .patronymic("Сидорович")
                .studentEmail("sidor@test.com")
                .build();

        coordinatorRole = Role.builder()
                .id(1L)
                .title("Sector_coordinator")
                .build();

        activistRole = Role.builder()
                .id(2L)
                .title("Activist")
                .build();

        sectorParticipant = SectorParticipant.builder()
                .id(1L)
                .sector(sector)
                .student(participant)
                .isCoordinator(false)
                .status(SectorParticipantStatuses.Активный)
                .entryDate(LocalDate.now())
                .build();

        coordinatorParticipant = SectorParticipant.builder()
                .id(2L)
                .sector(sector)
                .student(coordinator)
                .isCoordinator(true)
                .status(SectorParticipantStatuses.Активный)
                .entryDate(LocalDate.now())
                .build();

        nonCoordinatorParticipant = SectorParticipant.builder()
                .id(3L)
                .sector(sector)
                .student(coordinator)
                .isCoordinator(false)
                .status(SectorParticipantStatuses.Активный)
                .entryDate(LocalDate.now())
                .build();

        introductionRequest = SectorIntroductionRequest.builder()
                .id(1L)
                .sector(sector)
                .user(participant)
                .status(SectorIntroductionStatus.ОДОБРЕНА)
                .build();
    }

    @Test
    void createSector_Success() throws RoleNotFoundException {
        // given
        SectorDTO inputDTO = SectorDTO.builder()
                .title("Спортивный сектор")
                .description("Описание спортивного сектора")
                .build();

        Sector savedSector = Sector.builder()
                .id(1L)
                .title(inputDTO.getTitle())
                .description(inputDTO.getDescription())
                .isActive(true)
                .build();

        SectorDTO expectedDTO = SectorDTO.builder()
                .id(1L)
                .title(inputDTO.getTitle())
                .description(inputDTO.getDescription())
                .build();

        when(sectorMapper.toEntity(inputDTO)).thenReturn(savedSector);
        when(sectorRepository.save(any(Sector.class))).thenReturn(savedSector);
        // Исправлено: используем eq(null) для второго параметра, так как в коде передаётся null
        when(sectorMapper.toSectorDTO(any(Sector.class), isNull())).thenReturn(expectedDTO);

        // when
        SectorDTO result = sectorService.createSector(inputDTO);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        verify(sectorMapper).toEntity(inputDTO);
        verify(sectorRepository).save(any(Sector.class));
        verify(sectorMapper).toSectorDTO(any(Sector.class), isNull());
    }

    @Test
    void getSectorById_NotFound_ThrowsException() {
        // given
        when(sectorRepository.findSectorById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sectorService.getSectorById(1L))
                .isInstanceOf(SectorDoesNotExistException.class);
    }

    @Test
    void getSectorParticipants_Success() {
        // given
        Long sectorId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Page<SectorParticipant> participantPage = new PageImpl<>(List.of(sectorParticipant));

        when(sectorRepository.findById(sectorId)).thenReturn(Optional.of(sector));
        when(sectorParticipantRepository.findBySectorId(eq(sectorId), any(Pageable.class)))
                .thenReturn(participantPage);
        when(sectorParticipantMapper.toResponseDto(any(SectorParticipant.class), any()))
                .thenReturn(SectorParticipantResponseDTO.builder()
                        .id(1L)
                        .studentId(3L)
                        .studentName("Сидор")
                        .studentSurname("Сидоров")
                        .isCoordinator(false)
                        .build());

        // when
        Page<SectorParticipantResponseDTO> result = sectorService.getSectorParticipants(sectorId, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(sectorRepository).findById(sectorId);
        verify(sectorParticipantRepository).findBySectorId(eq(sectorId), any(Pageable.class));
        verify(sectorParticipantMapper).toResponseDto(any(SectorParticipant.class), any());
    }

    @Test
    void getSectorsWithUserStatus_Success() {
        // given
        Long userId = 1L;

        Object[] row = new Object[]{
                1L, "Спортивный сектор", "Описание", true, false, false, null, null, 0L, null, null, null, null
        };

        List<Object[]> nativeQueryResult = new ArrayList<>();
        nativeQueryResult.add(row);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(sectorRepository.findSectorsWithUserStatus(userId)).thenReturn(nativeQueryResult);

        SectorWithUserStatusDTO expectedDTO = SectorWithUserStatusDTO.builder()
                .id(1L)
                .title("Спортивный сектор")
                .isParticipant(false)
                .hasActiveRequest(false)
                .build();

        when(sectorWithUserStatusConverter.fromNativeQuery(any(Object[].class), any()))
                .thenReturn(expectedDTO);

        // when
        List<SectorWithUserStatusDTO> result = sectorService.getSectorsWithUserStatus(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        verify(userRepository).existsById(userId);
        verify(sectorRepository).findSectorsWithUserStatus(userId);
        verify(sectorWithUserStatusConverter).fromNativeQuery(any(Object[].class), any());
    }

    @Test
    void addCoordinator_Success() throws RoleNotFoundException {
        // given
        // Используем nonCoordinatorParticipant (isCoordinator = false)
        when(userRepository.findById(2L)).thenReturn(Optional.of(coordinator));
        when(roleRepository.findByTitle("Sector_coordinator")).thenReturn(Optional.of(coordinatorRole));
        when(sectorParticipantRepository.findBySectorIdAndStudentId(1L, 2L))
                .thenReturn(Optional.of(nonCoordinatorParticipant));
        when(sectorParticipantRepository.save(any(SectorParticipant.class))).thenReturn(nonCoordinatorParticipant);

        // when
        sectorService.addCoordinator(1L, 2L);

        // then
        verify(userRepository).findById(2L);
        verify(roleRepository).findByTitle("Sector_coordinator");
        verify(sectorParticipantRepository).findBySectorIdAndStudentId(1L, 2L);
        verify(sectorParticipantRepository).save(nonCoordinatorParticipant);
        verify(userRepository).save(coordinator);
    }

    @Test
    void addCoordinator_UserNotFound_ThrowsException() {
        // given
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sectorService.addCoordinator(1L, 2L))
                .isInstanceOf(UserDoesNotExistException.class);

        verify(userRepository).findById(2L);
        verify(sectorParticipantRepository, never()).save(any());
    }

    @Test
    void removeCoordinatorFromSector_Success() throws RoleNotFoundException {
        // given
        when(sectorParticipantRepository.findBySectorIdAndStudentId(1L, 2L))
                .thenReturn(Optional.of(coordinatorParticipant));
        when(sectorParticipantRepository.save(any(SectorParticipant.class))).thenReturn(coordinatorParticipant);
        when(sectorParticipantRepository.findAllByStudentIdAndIsCoordinatorTrue(2L))
                .thenReturn(new ArrayList<>());
        when(userRepository.findById(2L)).thenReturn(Optional.of(coordinator));
        when(roleRepository.findByTitle("Activist")).thenReturn(Optional.of(activistRole));

        // when
        sectorService.removeCoordinatorFromSector(1L, 2L);

        // then
        verify(sectorParticipantRepository).save(coordinatorParticipant);
        verify(userRepository).findById(2L);
        verify(userRepository).save(coordinator);
        verify(roleRepository).findByTitle("Activist");
    }

    @Test
    void removeCoordinatorFromSector_UserNotCoordinator_ThrowsException() {
        // given
        SectorParticipant nonCoordinator = SectorParticipant.builder()
                .isCoordinator(false)
                .build();
        when(sectorParticipantRepository.findBySectorIdAndStudentId(1L, 3L))
                .thenReturn(Optional.of(nonCoordinator));

        // when & then
        assertThatThrownBy(() -> sectorService.removeCoordinatorFromSector(1L, 3L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void kickParticipantFromSector_Success() {
        // given
        Long sectorId = 1L;
        Long coordinatorId = 2L;
        Long participantId = 3L;

        when(sectorParticipantRepository.findBySectorIdAndStudentId(sectorId, coordinatorId))
                .thenReturn(Optional.of(coordinatorParticipant));
        when(sectorParticipantRepository.findBySectorIdAndStudentId(sectorId, participantId))
                .thenReturn(Optional.of(sectorParticipant));
        when(sectorParticipantRepository.save(any(SectorParticipant.class)))
                .thenReturn(sectorParticipant);

        // when
        sectorService.kickParticipantFromSector(sectorId, coordinatorId, participantId);

        // then
        verify(sectorParticipantRepository).findBySectorIdAndStudentId(sectorId, coordinatorId);
        verify(sectorParticipantRepository).findBySectorIdAndStudentId(sectorId, participantId);
        verify(sectorParticipantRepository).save(any(SectorParticipant.class));
    }

    @Test
    void kickParticipantFromSector_CoordinatorNotAuthorized_ThrowsException() {
        // given
        Long sectorId = 1L;
        Long coordinatorId = 2L;
        Long participantId = 3L;

        when(sectorParticipantRepository.findBySectorIdAndStudentId(sectorId, coordinatorId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sectorService.kickParticipantFromSector(sectorId, coordinatorId, participantId))
                .isInstanceOf(UserDoesNotExistException.class);

        verify(sectorParticipantRepository).findBySectorIdAndStudentId(sectorId, coordinatorId);
        verify(sectorParticipantRepository, never()).findBySectorIdAndStudentId(sectorId, participantId);
        verify(sectorParticipantRepository, never()).save(any());
    }

    @Test
    void leaveSector_Success() {
        // given
        Long sectorId = 1L;
        Long userId = 1L;

        SectorParticipant participant = SectorParticipant.builder()
                .id(1L)
                .sector(sector)
                .student(user)
                .isCoordinator(false)
                .status(SectorParticipantStatuses.Активный)
                .build();

        when(sectorParticipantRepository.findByStudentIdAndSectorId(userId, sectorId))
                .thenReturn(Optional.of(participant));
        when(sectorParticipantRepository.save(any(SectorParticipant.class))).thenReturn(participant);

        // when
        sectorService.leaveSector(sectorId, userId);

        // then
        verify(sectorParticipantRepository).findByStudentIdAndSectorId(userId, sectorId);
        verify(sectorParticipantRepository).save(participant);
    }

    @Test
    void leaveSector_UserIsCoordinator_ThrowsException() {
        // given
        Long sectorId = 1L;
        Long userId = 2L;

        when(sectorParticipantRepository.findByStudentIdAndSectorId(userId, sectorId))
                .thenReturn(Optional.of(coordinatorParticipant));

        // when & then
        assertThatThrownBy(() -> sectorService.leaveSector(sectorId, userId))
                .isInstanceOf(IllegalStateException.class);

        verify(sectorParticipantRepository, never()).save(any());
    }

    @Test
    void leaveSector_UserAlreadyLeft_ThrowsException() {
        // given
        SectorParticipant leftParticipant = SectorParticipant.builder()
                .isCoordinator(false)
                .status(SectorParticipantStatuses.Вышедший)
                .build();

        when(sectorParticipantRepository.findByStudentIdAndSectorId(1L, 1L))
                .thenReturn(Optional.of(leftParticipant));

        // when & then
        assertThatThrownBy(() -> sectorService.leaveSector(1L, 1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void leaveSector_UserNotParticipant_ThrowsException() {
        // given
        when(sectorParticipantRepository.findByStudentIdAndSectorId(1L, 1L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sectorService.leaveSector(1L, 1L))
                .isInstanceOf(UserDoesNotExistException.class);
    }

    @Test
    void getSectorParticipants_SectorNotFound_ThrowsException() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        when(sectorRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sectorService.getSectorParticipants(1L, pageable))
                .isInstanceOf(SectorDoesNotExistException.class);
    }

    @Test
    void getSectorsWithUserStatus_UserNotFound_ReturnsEmptyList() {
        // given
        when(userRepository.existsById(1L)).thenReturn(false);

        // when
        List<SectorWithUserStatusDTO> result = sectorService.getSectorsWithUserStatus(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(sectorRepository, never()).findSectorsWithUserStatus(any());
    }
}
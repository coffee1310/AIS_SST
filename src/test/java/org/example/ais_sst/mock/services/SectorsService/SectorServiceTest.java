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
import org.example.ais_sst.utils.ImageUtil;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        when(sectorMapper.toEntity(sectorDTO)).thenReturn(sector);
        when(sectorRepository.save(any(Sector.class))).thenReturn(sector);
        when(sectorMapper.toSectorDTO(any(Sector.class), any(SectorPhotoService.class))).thenReturn(sectorDTO);

        // when
        SectorDTO result = sectorService.createSector(sectorDTO);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        verify(sectorMapper).toEntity(sectorDTO);
        verify(sectorRepository).save(any(Sector.class));
    }

//    @Test
//    void getSectorById_Success() {
//        // given
//        when(sectorRepository.findSectorById(1L)).thenReturn(Optional.of(sector));
//        when(sectorMapper.toSectorDTO(sector, sectorPhotoService)).thenReturn(sectorDTO);
//        when(sectorParticipantRepository.findBySectorIdAndIsCoordinatorTrue(1L))
//                .thenReturn(Optional.of(coordinatorParticipant));
//
//        // when
//        SectorDTO result = sectorService.getSectorById(1L);
//
//        // then
//        assertThat(result).isNotNull();
//        assertThat(result.getId()).isEqualTo(1L);
//
//        verify(sectorRepository).findSectorById(1L);
//    }

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
        Pageable pageable = PageRequest.of(0, 10);
        Page<SectorParticipant> participantPage = new PageImpl<>(List.of(sectorParticipant));
        SectorParticipantResponseDTO responseDTO = SectorParticipantResponseDTO.builder().id(1L).build();

        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(sectorParticipantRepository.findBySectorId(eq(1L), any(Pageable.class)))
                .thenReturn(participantPage);
        when(sectorParticipantMapper.toResponseDto(any(SectorParticipant.class)))
                .thenReturn(responseDTO);

        // when
        Page<SectorParticipantResponseDTO> result = sectorService.getSectorParticipants(1L, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

//    @Test
//    void getSectorCoordinator_Success() {
//        // given
//        when(sectorParticipantRepository.findBySectorIdAndIsCoordinatorTrue(1L))
//                .thenReturn(Optional.of(coordinatorParticipant));
//        when(sectorParticipantMapper.toResponseDto(coordinatorParticipant))
//                .thenReturn(SectorParticipantResponseDTO.builder().id(2L).build());
//
//        // when
//        SectorParticipantResponseDTO result = sectorService.getSectorCoordinator(1L);
//
//        // then
//        assertThat(result).isNotNull();
//    }

    @Test
    void getSectorsWithUserStatus_Success() {
        // given
        Object[] row = new Object[]{1L, "Сектор", "Описание", true, false, false, null, null, 0L, null, null, null, null};
        List<Object[]> results = new ArrayList<>();
        results.add(row);

        SectorWithUserStatusDTO statusDTO = SectorWithUserStatusDTO.builder()
                .id(1L)
                .title("Сектор")
                .isParticipant(true)
                .build();

        when(userRepository.existsById(1L)).thenReturn(true);
        when(sectorRepository.findSectorsWithUserStatus(1L)).thenReturn(results);
        when(sectorWithUserStatusConverter.fromNativeQuery(row)).thenReturn(statusDTO);

        // when
        List<SectorWithUserStatusDTO> result = sectorService.getSectorsWithUserStatus(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
    }

    @Test
    void addCoordinator_Success() throws RoleNotFoundException {
        // given
        when(userRepository.findUserById(2L)).thenReturn(Optional.of(coordinator));
        when(roleRepository.findByTitle("Sector_coordinator")).thenReturn(Optional.of(coordinatorRole));
        when(sectorParticipantRepository.findBySectorIdAndStudentId(1L, 2L))
                .thenReturn(Optional.of(coordinatorParticipant));
        when(sectorParticipantRepository.save(any(SectorParticipant.class))).thenReturn(coordinatorParticipant);

        // when
        sectorService.addCoordinator(1L, 2L);

        // then
        verify(userRepository).save(coordinator);
        verify(sectorParticipantRepository).save(coordinatorParticipant);
    }

    @Test
    void removeCoordinatorFromSector_Success() throws RoleNotFoundException {
        // given
        when(sectorParticipantRepository.findBySectorIdAndStudentId(1L, 2L))
                .thenReturn(Optional.of(coordinatorParticipant));
        when(sectorParticipantRepository.save(any(SectorParticipant.class))).thenReturn(coordinatorParticipant);
        when(sectorParticipantRepository.findAllByStudentIdAndIsCoordinatorTrue(2L))
                .thenReturn(new ArrayList<>());
        // Добавляем мок для userRepository.findUserById
        when(userRepository.findUserById(2L)).thenReturn(Optional.of(coordinator));
        when(roleRepository.findByTitle("Activist")).thenReturn(Optional.of(activistRole));

        // when
        sectorService.removeCoordinatorFromSector(1L, 2L);

        // then
        assertThat(coordinatorParticipant.getIsCoordinator()).isFalse();
        verify(sectorParticipantRepository).save(coordinatorParticipant);
        verify(userRepository).findUserById(2L);
        verify(userRepository).save(coordinator);
        verify(roleRepository).findByTitle("Activist");
    }

    @Test
    void kickParticipantFromSector_Success() throws RoleNotFoundException {
        // given
        when(userRepository.findUserById(2L)).thenReturn(Optional.of(coordinator));
        when(sectorParticipantRepository.findBySectorIdAndStudentId(1L, 2L))
                .thenReturn(Optional.of(coordinatorParticipant));
        when(userRepository.findUserById(3L)).thenReturn(Optional.of(participant));
        when(sectorParticipantRepository.findBySectorIdAndStudentId(1L, 3L))
                .thenReturn(Optional.of(sectorParticipant));
        when(sectorParticipantRepository.findByStudentId(3L)).thenReturn(new ArrayList<>());
        when(sectorIntroductionRequestRepository.getSectorIntroductionRequestsBySector_IdAndStatus(1L, SectorIntroductionStatus.ОДОБРЕНА))
                .thenReturn(new ArrayList<>()); // Пустой список, чтобы не вызывать save
        when(roleRepository.findByTitle("Activist")).thenReturn(Optional.of(activistRole));

        // when
        sectorService.kickParticipantFromSector(1L, 2L, 3L);

        // then
        verify(sectorParticipantRepository).delete(sectorParticipant);
    }

    @Test
    void leaveSector_Success() {
        // given
        when(sectorParticipantRepository.findByStudentIdAndSectorId(1L, 1L))
                .thenReturn(Optional.of(sectorParticipant));
        when(sectorParticipantRepository.save(any(SectorParticipant.class))).thenReturn(sectorParticipant);
        when(sectorIntroductionRequestRepository.getSectorIntroductionRequestsBySector_IdAndStatus(1L, SectorIntroductionStatus.ОДОБРЕНА))
                .thenReturn(new ArrayList<>()); // Пустой список

        // when
        sectorService.leaveSector(1L, 1L);

        // then
        assertThat(sectorParticipant.getStatus()).isEqualTo(SectorParticipantStatuses.Вышедший);
        verify(sectorParticipantRepository).save(sectorParticipant);
        // save НЕ вызывается, так как список approvedRequests пуст
        verify(sectorIntroductionRequestRepository, never()).save(any());
    }

    @Test
    void leaveSector_UserIsCoordinator_ThrowsException() {
        // given
        SectorParticipant coordinatorAsParticipant = SectorParticipant.builder()
                .isCoordinator(true)
                .status(SectorParticipantStatuses.Активный)
                .build();

        when(sectorParticipantRepository.findByStudentIdAndSectorId(2L, 1L))
                .thenReturn(Optional.of(coordinatorAsParticipant));

        // when & then
        assertThatThrownBy(() -> sectorService.leaveSector(1L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Координатор сектора");
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
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("уже покинул сектор");
    }


    @Test
    void getSectorParticipants_SectorNotFound_ThrowsException() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        when(sectorRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sectorService.getSectorParticipants(1L, pageable))
                .isInstanceOf(SectorDoesNotExistException.class)
                .hasMessageContaining("Сектор с id 1 не существует");
    }


//    @Test
//    void getSectorCoordinator_NotFound_ReturnsNull() {
//        // given
//        when(sectorParticipantRepository.findBySectorIdAndIsCoordinatorTrue(1L))
//                .thenReturn(Optional.empty());
//
//        // when
//        SectorParticipantResponseDTO result = sectorService.getSectorCoordinator(1L);
//
//        // then
//        assertThat(result).isNull();
//        verify(sectorParticipantRepository).findBySectorIdAndIsCoordinatorTrue(1L);
//    }

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


    @Test
    void addCoordinator_UserNotFound_ThrowsException() {
        // given
        when(userRepository.findUserById(2L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sectorService.addCoordinator(1L, 2L))
                .isInstanceOf(UserDoesNotExistException.class)
                .hasMessageContaining("Пользователь с id: 2 не найден");
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
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("не является координатором");
    }


    @Test
    void kickParticipantFromSector_CoordinatorNotAuthorized_ThrowsException() {
        // given
        SectorParticipant nonCoordinator = SectorParticipant.builder()
                .isCoordinator(false)
                .build();

        when(userRepository.findUserById(3L)).thenReturn(Optional.of(participant));
        when(sectorParticipantRepository.findBySectorIdAndStudentId(1L, 3L))
                .thenReturn(Optional.of(nonCoordinator));

        // when & then
        assertThatThrownBy(() -> sectorService.kickParticipantFromSector(1L, 3L, 4L))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("не является координатором");
    }

    @Test
    void leaveSector_UserNotParticipant_ThrowsException() {
        // given
        when(sectorParticipantRepository.findByStudentIdAndSectorId(1L, 1L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sectorService.leaveSector(1L, 1L))
                .isInstanceOf(UserDoesNotExistException.class)
                .hasMessageContaining("не является участником сектора");
    }
}
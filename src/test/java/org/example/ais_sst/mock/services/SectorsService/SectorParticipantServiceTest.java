package org.example.ais_sst.mock.services.SectorsService;

import org.example.ais_sst.entity.Sector;
import org.example.ais_sst.entity.SectorIntroductionRequest;
import org.example.ais_sst.entity.SectorParticipant;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.entity.enums.SectorIntroductionStatus;
import org.example.ais_sst.entity.enums.SectorParticipantStatuses;
import org.example.ais_sst.repository.SectorParticipantRepository;
import org.example.ais_sst.service.sectorService.SectorParticipantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectorParticipantServiceTest {

    @Mock
    private SectorParticipantRepository sectorParticipantRepository;

    @InjectMocks
    private SectorParticipantService sectorParticipantService;

    private Sector sector;
    private User user;
    private SectorIntroductionRequest request;
    private SectorParticipant expectedParticipant;

    @BeforeEach
    void setUp() {
        sector = Sector.builder()
                .id(1L)
                .title("Спортивный сектор")
                .description("Описание спортивного сектора")
                .isActive(true)
                .build();

        user = User.builder()
                .id(1L)
                .name("Иван")
                .surname("Иванов")
                .patronymic("Иванович")
                .studentEmail("ivan@test.com")
                .phoneNumber("+79991234567")
                .build();

        request = SectorIntroductionRequest.builder()
                .id(1L)
                .sector(sector)
                .user(user)
                .status(SectorIntroductionStatus.ОДОБРЕНА)
                .build();

        expectedParticipant = SectorParticipant.builder()
                .id(1L)
                .sector(sector)
                .student(user)
                .isCoordinator(false)
                .entryDate(LocalDate.now())
                .build();
    }

    @Test
    void createParticipant_Success() {
        // given
        when(sectorParticipantRepository.save(any(SectorParticipant.class))).thenReturn(expectedParticipant);

        // when
        SectorParticipant result = sectorParticipantService.createParticipant(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getSector()).isEqualTo(sector);
        assertThat(result.getStudent()).isEqualTo(user);
        assertThat(result.getIsCoordinator()).isFalse();

        verify(sectorParticipantRepository).save(any(SectorParticipant.class));
        verify(sectorParticipantRepository, times(1)).save(any(SectorParticipant.class));
    }

    @Test
    void createParticipant_WithNullRequest_ThrowsNullPointerException() {
        // when & then
        assertThatThrownBy(() -> sectorParticipantService.createParticipant(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("request");

        verify(sectorParticipantRepository, never()).save(any());
    }

    @Test
    void createParticipant_WithNullSectorInRequest_StillCreatesParticipant() {
        // given
        SectorIntroductionRequest invalidRequest = SectorIntroductionRequest.builder()
                .id(2L)
                .sector(null)
                .user(user)
                .build();

        SectorParticipant participantWithNullSector = SectorParticipant.builder()
                .id(2L)
                .sector(null)
                .student(user)
                .build();

        when(sectorParticipantRepository.save(any(SectorParticipant.class))).thenReturn(participantWithNullSector);

        // when
        SectorParticipant result = sectorParticipantService.createParticipant(invalidRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSector()).isNull();
        assertThat(result.getStudent()).isEqualTo(user);

        verify(sectorParticipantRepository).save(any(SectorParticipant.class));
    }

    @Test
    void createParticipant_WithNullUserInRequest_StillCreatesParticipant() {
        // given
        SectorIntroductionRequest invalidRequest = SectorIntroductionRequest.builder()
                .id(2L)
                .sector(sector)
                .user(null)
                .build();

        SectorParticipant participantWithNullUser = SectorParticipant.builder()
                .id(2L)
                .sector(sector)
                .student(null)
                .build();

        when(sectorParticipantRepository.save(any(SectorParticipant.class))).thenReturn(participantWithNullUser);

        // when
        SectorParticipant result = sectorParticipantService.createParticipant(invalidRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSector()).isEqualTo(sector);
        assertThat(result.getStudent()).isNull();

        verify(sectorParticipantRepository).save(any(SectorParticipant.class));
    }


    @Test
    void createParticipant_RepositoryThrowsException_PropagatesException() {
        // given
        when(sectorParticipantRepository.save(any(SectorParticipant.class)))
                .thenThrow(new RuntimeException("Database error"));

        // when & then
        try {
            sectorParticipantService.createParticipant(request);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class);
            assertThat(e.getMessage()).isEqualTo("Database error");
        }

        verify(sectorParticipantRepository).save(any(SectorParticipant.class));
    }

    @Test
    void createParticipant_VerifyBuilderFields() {
        // given
        when(sectorParticipantRepository.save(any(SectorParticipant.class))).thenAnswer(invocation -> {
            SectorParticipant saved = invocation.getArgument(0);
            return SectorParticipant.builder()
                    .id(5L)
                    .sector(saved.getSector())
                    .student(saved.getStudent())
                    .isCoordinator(saved.getIsCoordinator())
                    .entryDate(saved.getEntryDate())
                    .build();
        });

        // when
        SectorParticipant result = sectorParticipantService.createParticipant(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getSector().getId()).isEqualTo(1L);
        assertThat(result.getStudent().getId()).isEqualTo(1L);
        assertThat(result.getIsCoordinator()).isFalse();
        // статус не устанавливается, поэтому он null
        assertThat(result.getStatus()).isNull();
    }

    @Test
    void createParticipant_CreatesNewParticipantEachTime() {
        // given
        User anotherUser = User.builder()
                .id(2L)
                .name("Петр")
                .surname("Петров")
                .build();

        SectorIntroductionRequest anotherRequest = SectorIntroductionRequest.builder()
                .id(2L)
                .sector(sector)
                .user(anotherUser)
                .build();

        SectorParticipant anotherParticipant = SectorParticipant.builder()
                .id(2L)
                .sector(sector)
                .student(anotherUser)
                .build();

        when(sectorParticipantRepository.save(any(SectorParticipant.class)))
                .thenReturn(expectedParticipant)
                .thenReturn(anotherParticipant);

        // when
        SectorParticipant result1 = sectorParticipantService.createParticipant(request);
        SectorParticipant result2 = sectorParticipantService.createParticipant(anotherRequest);

        // then
        assertThat(result1.getStudent().getId()).isEqualTo(1L);
        assertThat(result2.getStudent().getId()).isEqualTo(2L);
        assertThat(result1).isNotEqualTo(result2);

        verify(sectorParticipantRepository, times(2)).save(any(SectorParticipant.class));
    }
}
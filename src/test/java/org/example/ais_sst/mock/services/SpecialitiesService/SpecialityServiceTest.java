package org.example.ais_sst.mock.services.SpecialitiesService;

import org.example.ais_sst.entity.Speciality;
import org.example.ais_sst.repository.SpecialityRepository;
import org.example.ais_sst.service.specialityService.SpecialityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecialityServiceTest {

    @Mock
    private SpecialityRepository specialityRepository;

    @InjectMocks
    private SpecialityService specialityService;

    private Speciality speciality1;
    private Speciality speciality2;

    @BeforeEach
    void setUp() {
        speciality1 = Speciality.builder()
                .id(1L)
                .title("Информационные системы и программирование")
                .shortTitle("ИСП")
                .build();

        speciality2 = Speciality.builder()
                .id(2L)
                .title("Программная инженерия")
                .shortTitle("ПИ")
                .build();
    }

    // ==================== TESTS FOR getSpecialities ====================

    @Test
    void getSpecialities_Success() {
        // given
        List<Speciality> expectedSpecialities = Arrays.asList(speciality1, speciality2);
        when(specialityRepository.findAll()).thenReturn(expectedSpecialities);

        // when
        List<Speciality> result = specialityService.getSpecialities();

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(speciality1, speciality2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getTitle()).isEqualTo("Информационные системы и программирование");
        assertThat(result.get(0).getShortTitle()).isEqualTo("ИСП");
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getTitle()).isEqualTo("Программная инженерия");
        assertThat(result.get(1).getShortTitle()).isEqualTo("ПИ");

        verify(specialityRepository).findAll();
        verify(specialityRepository, times(1)).findAll();
    }

    @Test
    void getSpecialities_WhenNoSpecialities_ReturnsEmptyList() {
        // given
        when(specialityRepository.findAll()).thenReturn(Collections.emptyList());

        // when
        List<Speciality> result = specialityService.getSpecialities();

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        assertThat(result).hasSize(0);

        verify(specialityRepository).findAll();
    }

    @Test
    void getSpecialities_WhenRepositoryReturnsNull_ReturnsNull() {
        // given
        when(specialityRepository.findAll()).thenReturn(null);

        // when
        List<Speciality> result = specialityService.getSpecialities();

        // then
        assertThat(result).isNull();

        verify(specialityRepository).findAll();
    }

    @Test
    void getSpecialities_WithSingleSpeciality_ReturnsListWithOneElement() {
        // given
        List<Speciality> expectedSpecialities = List.of(speciality1);
        when(specialityRepository.findAll()).thenReturn(expectedSpecialities);

        // when
        List<Speciality> result = specialityService.getSpecialities();

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getTitle()).isEqualTo("Информационные системы и программирование");

        verify(specialityRepository).findAll();
    }

    @Test
    void getSpecialities_WithSpecialitiesHavingNullShortTitle_ReturnsCorrectly() {
        // given
        Speciality specialityWithoutShortTitle = Speciality.builder()
                .id(3L)
                .title("Прикладная математика")
                .shortTitle(null)
                .build();

        List<Speciality> expectedSpecialities = Arrays.asList(speciality1, specialityWithoutShortTitle);
        when(specialityRepository.findAll()).thenReturn(expectedSpecialities);

        // when
        List<Speciality> result = specialityService.getSpecialities();

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getShortTitle()).isEqualTo("ИСП");
        assertThat(result.get(1).getShortTitle()).isNull();

        verify(specialityRepository).findAll();
    }

    @Test
    void getSpecialities_MultipleCalls_ReturnsConsistentResults() {
        // given
        List<Speciality> expectedSpecialities = Arrays.asList(speciality1, speciality2);
        when(specialityRepository.findAll()).thenReturn(expectedSpecialities);

        // when
        List<Speciality> firstCall = specialityService.getSpecialities();
        List<Speciality> secondCall = specialityService.getSpecialities();

        // then
        assertThat(firstCall).isSameAs(secondCall);
        assertThat(firstCall).hasSize(2);
        assertThat(secondCall).hasSize(2);

        verify(specialityRepository, times(2)).findAll();
    }

    @Test
    void getSpecialities_RepositoryThrowsException_PropagatesException() {
        // given
        when(specialityRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        // when & then
        try {
            specialityService.getSpecialities();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class);
            assertThat(e.getMessage()).isEqualTo("Database error");
        }

        verify(specialityRepository).findAll();
    }

    @Test
    void getSpecialities_VerifyRepositoryInteraction() {
        // given
        when(specialityRepository.findAll()).thenReturn(Arrays.asList(speciality1, speciality2));

        // when
        specialityService.getSpecialities();

        // then
        verify(specialityRepository, times(1)).findAll();
        verifyNoMoreInteractions(specialityRepository);
    }
}
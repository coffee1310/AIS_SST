package org.example.ais_sst.controllers;

import org.example.ais_sst.controller.SpecialityController;
import org.example.ais_sst.entity.Speciality;
import org.example.ais_sst.service.specialityService.SpecialityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecialityControllerTest {

    @Mock
    private SpecialityService specialityService;

    @InjectMocks
    private SpecialityController specialityController;

    private Speciality speciality1;
    private Speciality speciality2;
    private Speciality speciality3;
    private List<Speciality> specialityList;

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

        speciality3 = Speciality.builder()
                .id(3L)
                .title("Прикладная математика и информатика")
                .shortTitle("ПМИ")
                .build();

        specialityList = Arrays.asList(speciality1, speciality2, speciality3);
    }

    // ==================== TESTS FOR getSpecialities ====================

    @Test
    void getSpecialities_Success() {
        // given
        when(specialityService.getSpecialities()).thenReturn(specialityList);

        // when
        ResponseEntity<?> response = specialityController.getSpecialities();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(specialityList);

        @SuppressWarnings("unchecked")
        List<Speciality> body = (List<Speciality>) response.getBody();
        assertThat(body).hasSize(3);
        assertThat(body.get(0).getId()).isEqualTo(1L);
        assertThat(body.get(0).getTitle()).isEqualTo("Информационные системы и программирование");
        assertThat(body.get(0).getShortTitle()).isEqualTo("ИСП");
        assertThat(body.get(1).getId()).isEqualTo(2L);
        assertThat(body.get(1).getTitle()).isEqualTo("Программная инженерия");
        assertThat(body.get(1).getShortTitle()).isEqualTo("ПИ");
        assertThat(body.get(2).getId()).isEqualTo(3L);
        assertThat(body.get(2).getTitle()).isEqualTo("Прикладная математика и информатика");
        assertThat(body.get(2).getShortTitle()).isEqualTo("ПМИ");

        verify(specialityService).getSpecialities();
        verify(specialityService, times(1)).getSpecialities();
    }

    @Test
    void getSpecialities_WhenNoSpecialities_ReturnsEmptyList() {
        // given
        when(specialityService.getSpecialities()).thenReturn(Collections.emptyList());

        // when
        ResponseEntity<?> response = specialityController.getSpecialities();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        @SuppressWarnings("unchecked")
        List<Speciality> body = (List<Speciality>) response.getBody();
        assertThat(body).isEmpty();

        verify(specialityService).getSpecialities();
    }

    @Test
    void getSpecialities_WhenServiceReturnsNull_ReturnsNull() {
        // given
        when(specialityService.getSpecialities()).thenReturn(null);

        // when
        ResponseEntity<?> response = specialityController.getSpecialities();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNull();

        verify(specialityService).getSpecialities();
    }

    @Test
    void getSpecialities_WithSingleSpeciality_ReturnsListWithOneElement() {
        // given
        List<Speciality> singleSpecialityList = List.of(speciality1);
        when(specialityService.getSpecialities()).thenReturn(singleSpecialityList);

        // when
        ResponseEntity<?> response = specialityController.getSpecialities();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        List<Speciality> body = (List<Speciality>) response.getBody();
        assertThat(body).hasSize(1);
        assertThat(body.get(0).getId()).isEqualTo(1L);
        assertThat(body.get(0).getTitle()).isEqualTo("Информационные системы и программирование");
        assertThat(body.get(0).getShortTitle()).isEqualTo("ИСП");

        verify(specialityService).getSpecialities();
    }

    @Test
    void getSpecialities_WithSpecialitiesHavingNullShortTitle_HandlesCorrectly() {
        // given
        Speciality specialityWithoutShortTitle = Speciality.builder()
                .id(4L)
                .title("Бизнес-информатика")
                .shortTitle(null)
                .build();

        List<Speciality> specialities = Arrays.asList(speciality1, specialityWithoutShortTitle);
        when(specialityService.getSpecialities()).thenReturn(specialities);

        // when
        ResponseEntity<?> response = specialityController.getSpecialities();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        List<Speciality> body = (List<Speciality>) response.getBody();
        assertThat(body).hasSize(2);
        assertThat(body.get(0).getShortTitle()).isEqualTo("ИСП");
        assertThat(body.get(1).getShortTitle()).isNull();

        verify(specialityService).getSpecialities();
    }

    @Test
    void getSpecialities_ServiceThrowsException_PropagatesException() {
        // given
        when(specialityService.getSpecialities()).thenThrow(new RuntimeException("Database error"));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            specialityController.getSpecialities();
        });

        verify(specialityService).getSpecialities();
    }

    @Test
    void getSpecialities_ReturnsCorrectHttpStatus() {
        // given
        when(specialityService.getSpecialities()).thenReturn(specialityList);

        // when
        ResponseEntity<?> response = specialityController.getSpecialities();

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getSpecialities_ResponseBodyTypeIsCorrect() {
        // given
        when(specialityService.getSpecialities()).thenReturn(specialityList);

        // when
        ResponseEntity<?> response = specialityController.getSpecialities();

        // then
        assertThat(response.getBody()).isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<Speciality> body = (List<Speciality>) response.getBody();
        assertThat(body.get(0)).isInstanceOf(Speciality.class);
    }

    @Test
    void getSpecialities_MultipleCalls_ReturnConsistentResults() {
        // given
        when(specialityService.getSpecialities()).thenReturn(specialityList);

        // when
        ResponseEntity<?> firstCall = specialityController.getSpecialities();
        ResponseEntity<?> secondCall = specialityController.getSpecialities();

        // then
        assertThat(firstCall.getBody()).isEqualTo(secondCall.getBody());

        verify(specialityService, times(2)).getSpecialities();
    }

    // ==================== INTEGRATION STYLE TESTS ====================

    @Test
    void getSpecialities_EndpointReturnsListOfSpecialities() {
        // given
        when(specialityService.getSpecialities()).thenReturn(specialityList);

        // when
        ResponseEntity<?> response = specialityController.getSpecialities();

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();

        @SuppressWarnings("unchecked")
        List<Speciality> result = (List<Speciality>) response.getBody();
        assertThat(result).allMatch(speciality -> speciality instanceof Speciality);
    }

    @Test
    void getSpecialities_WithSpecialitiesHavingVeryLongTitles_HandlesCorrectly() {
        // given
        String longTitle = "Очень длинное название специальности которая может быть больше обычного лимита в несколько раз для проверки обработки длинных строк";
        Speciality longTitleSpeciality = Speciality.builder()
                .id(10L)
                .title(longTitle)
                .shortTitle("ДЛН")
                .build();

        List<Speciality> specialities = List.of(longTitleSpeciality);
        when(specialityService.getSpecialities()).thenReturn(specialities);

        // when
        ResponseEntity<?> response = specialityController.getSpecialities();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        List<Speciality> body = (List<Speciality>) response.getBody();
        assertThat(body.get(0).getTitle()).isEqualTo(longTitle);

        verify(specialityService).getSpecialities();
    }

    @Test
    void getSpecialities_WithNullValuesInList_ServiceShouldHandle() {
        // given
        List<Speciality> specialitiesWithNull = Arrays.asList(speciality1, null, speciality2);
        when(specialityService.getSpecialities()).thenReturn(specialitiesWithNull);

        // when
        ResponseEntity<?> response = specialityController.getSpecialities();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        List<Speciality> body = (List<Speciality>) response.getBody();
        assertThat(body).hasSize(3);
        assertThat(body.get(0)).isNotNull();
        assertThat(body.get(1)).isNull();
        assertThat(body.get(2)).isNotNull();

        verify(specialityService).getSpecialities();
    }

    @Test
    void getSpecialities_WithEmptyShortTitle_HandlesCorrectly() {
        // given
        Speciality emptyShortTitleSpeciality = Speciality.builder()
                .id(5L)
                .title("Информатика и вычислительная техника")
                .shortTitle("")
                .build();

        List<Speciality> specialities = List.of(emptyShortTitleSpeciality);
        when(specialityService.getSpecialities()).thenReturn(specialities);

        // when
        ResponseEntity<?> response = specialityController.getSpecialities();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        List<Speciality> body = (List<Speciality>) response.getBody();
        assertThat(body.get(0).getShortTitle()).isEmpty();

        verify(specialityService).getSpecialities();
    }

    @Test
    void getSpecialities_LargeListPerformance_ShouldHandle() {
        // given
        List<Speciality> largeList = Collections.nCopies(100, speciality1);
        when(specialityService.getSpecialities()).thenReturn(largeList);

        // when
        ResponseEntity<?> response = specialityController.getSpecialities();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        List<Speciality> body = (List<Speciality>) response.getBody();
        assertThat(body).hasSize(100);

        verify(specialityService).getSpecialities();
    }
}
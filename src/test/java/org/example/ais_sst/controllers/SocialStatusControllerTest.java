package org.example.ais_sst.controllers;

import org.example.ais_sst.controller.SocialStatusController;
import org.example.ais_sst.entity.SocialStatus;
import org.example.ais_sst.service.socialStatusService.SocialStatusService;
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
class SocialStatusControllerTest {

    @Mock
    private SocialStatusService socialStatusService;

    @InjectMocks
    private SocialStatusController socialStatusController;

    private SocialStatus socialStatus1;
    private SocialStatus socialStatus2;
    private List<SocialStatus> socialStatusList;

    @BeforeEach
    void setUp() {
        socialStatus1 = SocialStatus.builder()
                .id(1L)
                .title("Студент")
                .build();

        socialStatus2 = SocialStatus.builder()
                .id(2L)
                .title("Активист")
                .build();

        socialStatusList = Arrays.asList(socialStatus1, socialStatus2);
    }

    // ==================== TESTS FOR getSocial_statuses ====================

    @Test
    void getSocialStatuses_Success() {
        // given
        when(socialStatusService.getSocialStatuses()).thenReturn(socialStatusList);

        // when
        ResponseEntity<?> response = socialStatusController.getSocial_statuses();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(socialStatusList);

        @SuppressWarnings("unchecked")
        List<SocialStatus> body = (List<SocialStatus>) response.getBody();
        assertThat(body).hasSize(2);
        assertThat(body.get(0).getId()).isEqualTo(1L);
        assertThat(body.get(0).getTitle()).isEqualTo("Студент");
        assertThat(body.get(1).getId()).isEqualTo(2L);
        assertThat(body.get(1).getTitle()).isEqualTo("Активист");

        verify(socialStatusService).getSocialStatuses();
        verify(socialStatusService, times(1)).getSocialStatuses();
    }

    @Test
    void getSocialStatuses_WhenNoStatuses_ReturnsEmptyList() {
        // given
        when(socialStatusService.getSocialStatuses()).thenReturn(Collections.emptyList());

        // when
        ResponseEntity<?> response = socialStatusController.getSocial_statuses();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        @SuppressWarnings("unchecked")
        List<SocialStatus> body = (List<SocialStatus>) response.getBody();
        assertThat(body).isEmpty();

        verify(socialStatusService).getSocialStatuses();
    }

    @Test
    void getSocialStatuses_WhenServiceReturnsNull_ReturnsNull() {
        // given
        when(socialStatusService.getSocialStatuses()).thenReturn(null);

        // when
        ResponseEntity<?> response = socialStatusController.getSocial_statuses();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNull();

        verify(socialStatusService).getSocialStatuses();
    }

    @Test
    void getSocialStatuses_WithSingleStatus_ReturnsListWithOneElement() {
        // given
        List<SocialStatus> singleStatusList = List.of(socialStatus1);
        when(socialStatusService.getSocialStatuses()).thenReturn(singleStatusList);

        // when
        ResponseEntity<?> response = socialStatusController.getSocial_statuses();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        List<SocialStatus> body = (List<SocialStatus>) response.getBody();
        assertThat(body).hasSize(1);
        assertThat(body.get(0).getId()).isEqualTo(1L);
        assertThat(body.get(0).getTitle()).isEqualTo("Студент");

        verify(socialStatusService).getSocialStatuses();
    }

    @Test
    void getSocialStatuses_WithMultipleStatuses_ReturnsAllStatuses() {
        // given
        SocialStatus socialStatus3 = SocialStatus.builder()
                .id(3L)
                .title("Волонтер")
                .build();

        List<SocialStatus> multipleStatuses = Arrays.asList(socialStatus1, socialStatus2, socialStatus3);
        when(socialStatusService.getSocialStatuses()).thenReturn(multipleStatuses);

        // when
        ResponseEntity<?> response = socialStatusController.getSocial_statuses();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        List<SocialStatus> body = (List<SocialStatus>) response.getBody();
        assertThat(body).hasSize(3);
        assertThat(body.get(0).getTitle()).isEqualTo("Студент");
        assertThat(body.get(1).getTitle()).isEqualTo("Активист");
        assertThat(body.get(2).getTitle()).isEqualTo("Волонтер");

        verify(socialStatusService).getSocialStatuses();
    }

    @Test
    void getSocialStatuses_ServiceThrowsException_PropagatesException() {
        // given
        when(socialStatusService.getSocialStatuses()).thenThrow(new RuntimeException("Database error"));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            socialStatusController.getSocial_statuses();
        });

        verify(socialStatusService).getSocialStatuses();
    }

    @Test
    void getSocialStatuses_ReturnsCorrectHttpStatus() {
        // given
        when(socialStatusService.getSocialStatuses()).thenReturn(socialStatusList);

        // when
        ResponseEntity<?> response = socialStatusController.getSocial_statuses();

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getSocialStatuses_ResponseBodyTypeIsCorrect() {
        // given
        when(socialStatusService.getSocialStatuses()).thenReturn(socialStatusList);

        // when
        ResponseEntity<?> response = socialStatusController.getSocial_statuses();

        // then
        assertThat(response.getBody()).isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<SocialStatus> body = (List<SocialStatus>) response.getBody();
        assertThat(body.get(0)).isInstanceOf(SocialStatus.class);
    }

    @Test
    void getSocialStatuses_MultipleCalls_ReturnConsistentResults() {
        // given
        when(socialStatusService.getSocialStatuses()).thenReturn(socialStatusList);

        // when
        ResponseEntity<?> firstCall = socialStatusController.getSocial_statuses();
        ResponseEntity<?> secondCall = socialStatusController.getSocial_statuses();

        // then
        assertThat(firstCall.getBody()).isEqualTo(secondCall.getBody());

        verify(socialStatusService, times(2)).getSocialStatuses();
    }

    // ==================== INTEGRATION STYLE TESTS ====================

    @Test
    void getSocialStatuses_EndpointReturnsListOfSocialStatuses() {
        // given
        when(socialStatusService.getSocialStatuses()).thenReturn(socialStatusList);

        // when
        ResponseEntity<?> response = socialStatusController.getSocial_statuses();

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();

        @SuppressWarnings("unchecked")
        List<SocialStatus> result = (List<SocialStatus>) response.getBody();
        assertThat(result).allMatch(status -> status instanceof SocialStatus);
    }

    @Test
    void getSocialStatuses_WithStatusesHavingLongTitles_HandlesCorrectly() {
        // given
        SocialStatus longTitleStatus = SocialStatus.builder()
                .id(10L)
                .title("Очень длинное название социального статуса которое может быть больше обычного")
                .build();

        List<SocialStatus> statuses = List.of(longTitleStatus);
        when(socialStatusService.getSocialStatuses()).thenReturn(statuses);

        // when
        ResponseEntity<?> response = socialStatusController.getSocial_statuses();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        List<SocialStatus> body = (List<SocialStatus>) response.getBody();
        assertThat(body.get(0).getTitle()).isEqualTo(longTitleStatus.getTitle());

        verify(socialStatusService).getSocialStatuses();
    }

    @Test
    void getSocialStatuses_WithNullValuesInList_ServiceShouldHandle() {
        // given
        List<SocialStatus> statusesWithNull = Arrays.asList(socialStatus1, null, socialStatus2);
        when(socialStatusService.getSocialStatuses()).thenReturn(statusesWithNull);

        // when
        ResponseEntity<?> response = socialStatusController.getSocial_statuses();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        List<SocialStatus> body = (List<SocialStatus>) response.getBody();
        assertThat(body).hasSize(3);
        assertThat(body.get(0)).isNotNull();
        assertThat(body.get(1)).isNull();
        assertThat(body.get(2)).isNotNull();

        verify(socialStatusService).getSocialStatuses();
    }
}
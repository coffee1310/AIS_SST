package org.example.ais_sst.mock.controllers;

import org.example.ais_sst.controller.SectorsController;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTO;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTOSummary;
import org.example.ais_sst.dto.sector.SectorDTO;
import org.example.ais_sst.dto.sector.SectorParticipantResponseDTO;
import org.example.ais_sst.dto.sector.SectorWithUserStatusDTO;
import org.example.ais_sst.entity.CustomUserDetails;
import org.example.ais_sst.entity.enums.SectorIntroductionStatus;
import org.example.ais_sst.service.sectorService.SectorIntroductionRequestService;
import org.example.ais_sst.service.sectorService.SectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.management.relation.RoleNotFoundException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectorsControllerTest {

    @Mock
    private SectorService sectorService;

    @Mock
    private SectorIntroductionRequestService sectorIntroductionRequestService;

    @Mock
    private CustomUserDetails userDetails;

    @InjectMocks
    private SectorsController sectorsController;

    private SectorDTO sectorDTO;
    private SectorWithUserStatusDTO sectorWithUserStatusDTO;
    private SectorParticipantResponseDTO participantResponseDTO;
    private SectorIntroductionRequestDTO requestDTO;
    private SectorIntroductionRequestDTOSummary requestSummaryDTO;
    private Page<SectorParticipantResponseDTO> participantPage;

    @BeforeEach
    void setUp() {
        // Удаляем строку: when(userDetails.getId()).thenReturn(1L);
        // Добавляем stubbing только в тестах где нужно

        sectorDTO = SectorDTO.builder()
                .id(1L)
                .title("Спортивный сектор")
                .description("Описание спортивного сектора")
                .build();

        sectorWithUserStatusDTO = SectorWithUserStatusDTO.builder()
                .id(1L)
                .title("Спортивный сектор")
                .isParticipant(false)
                .hasActiveRequest(false)
                .build();

        participantResponseDTO = SectorParticipantResponseDTO.builder()
                .id(1L)
                .studentId(2L)
                .studentName("Иван")
                .studentSurname("Иванов")
                .isCoordinator(false)
                .build();

        requestDTO = SectorIntroductionRequestDTO.builder()
                .id(1L)
                .user_id(2L)
                .sector_id(1L)
                .status(SectorIntroductionStatus.НА_РАССМОТРЕНИИ)
                .build();

        requestSummaryDTO = SectorIntroductionRequestDTOSummary.builder()
                .id(1L)
                .status(SectorIntroductionStatus.НА_РАССМОТРЕНИИ)
                .build();

        participantPage = new PageImpl<>(List.of(participantResponseDTO));
    }

    // ==================== TESTS FOR createSector ====================

    @Test
    void createSector_Success() throws RoleNotFoundException {
        // given
        when(sectorService.createSector(any(SectorDTO.class))).thenReturn(sectorDTO);

        // when
        ResponseEntity<?> response = sectorsController.createSector(sectorDTO);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(sectorDTO);

        verify(sectorService).createSector(sectorDTO);
    }

    @Test
    void createSector_WithInvalidData_ThrowsException() throws RoleNotFoundException {
        // given
        when(sectorService.createSector(any(SectorDTO.class)))
                .thenThrow(new RuntimeException("Invalid sector data"));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            sectorsController.createSector(sectorDTO);
        });

        verify(sectorService).createSector(sectorDTO);
    }

    // ==================== TESTS FOR getSectors ====================

    @Test
    void getSectors_Success() {
        // given
        when(userDetails.getId()).thenReturn(1L);
        List<SectorWithUserStatusDTO> sectors = List.of(sectorWithUserStatusDTO);
        when(sectorService.getSectorsWithUserStatus(1L)).thenReturn(sectors);

        // when
        ResponseEntity<?> response = sectorsController.getSectors(userDetails);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(sectors);

        verify(sectorService).getSectorsWithUserStatus(1L);
    }

    @Test
    void getSectors_EmptyList_ReturnsEmptyList() {
        // given
        when(userDetails.getId()).thenReturn(1L);
        when(sectorService.getSectorsWithUserStatus(1L)).thenReturn(List.of());

        // when
        ResponseEntity<?> response = sectorsController.getSectors(userDetails);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        verify(sectorService).getSectorsWithUserStatus(1L);
    }

    // ==================== TESTS FOR getSectorById ====================

    @Test
    void getSectorById_Success() {
        // given
        when(sectorService.getSectorById(1L)).thenReturn(sectorDTO);

        // when
        ResponseEntity<SectorDTO> response = sectorsController.getSectorById(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(sectorDTO);

        verify(sectorService).getSectorById(1L);
    }

    @Test
    void getSectorById_NotFound_ThrowsException() {
        // given
        when(sectorService.getSectorById(999L)).thenThrow(new RuntimeException("Sector not found"));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            sectorsController.getSectorById(999L);
        });

        verify(sectorService).getSectorById(999L);
    }

    // ==================== TESTS FOR getSectorParticipants ====================

    @Test
    void getSectorParticipants_Success() {
        // given
        when(sectorService.getSectorParticipants(eq(1L), any(Pageable.class))).thenReturn(participantPage);

        // when
        ResponseEntity<Page<SectorParticipantResponseDTO>> response = sectorsController.getSectorParticipants(1L, 0, 20, "entryDate", "DESC");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);

        verify(sectorService).getSectorParticipants(eq(1L), any(Pageable.class));
    }

    @Test
    void getSectorParticipants_WithCustomPagination() {
        // given
        when(sectorService.getSectorParticipants(eq(1L), any(Pageable.class))).thenReturn(participantPage);

        // when
        sectorsController.getSectorParticipants(1L, 2, 50, "studentName", "ASC");

        // then
        verify(sectorService).getSectorParticipants(eq(1L),
                eq(PageRequest.of(2, 50, Sort.by(Sort.Direction.ASC, "studentName"))));
    }

    // ==================== TESTS FOR getSectorCoordinator ====================

    @Test
    void getSectorCoordinator_Success() {
        // given
        when(sectorService.getSectorCoordinator(1L)).thenReturn(participantResponseDTO);

        // when
        ResponseEntity<SectorParticipantResponseDTO> response = sectorsController.getSectorCoordinator(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(participantResponseDTO);

        verify(sectorService).getSectorCoordinator(1L);
    }

    @Test
    void getSectorCoordinator_NotFound_ReturnsNotFound() {
        // given
        when(sectorService.getSectorCoordinator(1L)).thenReturn(null);

        // when
        ResponseEntity<SectorParticipantResponseDTO> response = sectorsController.getSectorCoordinator(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();

        verify(sectorService).getSectorCoordinator(1L);
    }

    // ==================== TESTS FOR createSectorIntroductionRequest ====================

    @Test
    void createSectorIntroductionRequest_Success() {
        // given
        when(userDetails.getId()).thenReturn(1L);
        when(sectorIntroductionRequestService.createRequest(1L, 1L)).thenReturn(requestDTO);

        // when
        ResponseEntity<?> response = sectorsController.createSectorIntroductionRequest(userDetails, 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(requestDTO);

        verify(sectorIntroductionRequestService).createRequest(1L, 1L);
    }

    // ==================== TESTS FOR acceptSectorIntroductionRequest ====================

    @Test
    void acceptSectorIntroductionRequest_Success() {
        // given
        when(sectorIntroductionRequestService.acceptRequest(1L)).thenReturn(requestSummaryDTO);

        // when
        ResponseEntity<?> response = sectorsController.acceptSectorIntroductionRequest(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(Map.class);

        verify(sectorIntroductionRequestService).acceptRequest(1L);
    }

    // ==================== TESTS FOR rejectSectorIntroductionRequest ====================

    @Test
    void rejectSectorIntroductionRequest_Success() {
        // given
        when(sectorIntroductionRequestService.rejectRequest(1L)).thenReturn(requestSummaryDTO);

        // when
        ResponseEntity<?> response = sectorsController.rejectSectorIntroductionRequest(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(sectorIntroductionRequestService).rejectRequest(1L);
    }

    // ==================== TESTS FOR getSectorIntroductionRequests ====================

    @Test
    void getSectorIntroductionRequests_Success() {
        // given
        when(userDetails.getId()).thenReturn(1L);
        List<SectorIntroductionRequestDTO> requests = List.of(requestDTO);
        when(sectorIntroductionRequestService.getRequestsListByCoordinator(1L)).thenReturn(requests);

        // when
        ResponseEntity<?> response = sectorsController.getSectorIntroductionRequests(userDetails);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(requests);

        verify(sectorIntroductionRequestService).getRequestsListByCoordinator(1L);
    }

    @Test
    void getSectorIntroductionRequests_EmptyList_ReturnsEmptyList() {
        // given
        when(userDetails.getId()).thenReturn(1L);
        when(sectorIntroductionRequestService.getRequestsListByCoordinator(1L)).thenReturn(List.of());

        // when
        ResponseEntity<?> response = sectorsController.getSectorIntroductionRequests(userDetails);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        verify(sectorIntroductionRequestService).getRequestsListByCoordinator(1L);
    }

    // ==================== TESTS FOR getSectorIntroductionRequestsWithStatus ====================

    @Test
    void getSectorIntroductionRequestsWithStatus_Success() {
        // given
        when(userDetails.getId()).thenReturn(1L);
        List<SectorIntroductionRequestDTO> requests = List.of(requestDTO);
        when(sectorIntroductionRequestService.getRequestsListByCoordinatorWithStatus(1L, SectorIntroductionStatus.НА_РАССМОТРЕНИИ))
                .thenReturn(requests);

        // when
        ResponseEntity<?> response = sectorsController.getSectorIntroductionRequestsWithStatus(userDetails, SectorIntroductionStatus.НА_РАССМОТРЕНИИ);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(requests);

        verify(sectorIntroductionRequestService).getRequestsListByCoordinatorWithStatus(1L, SectorIntroductionStatus.НА_РАССМОТРЕНИИ);
    }

    @Test
    void getSectorIntroductionRequestsWithStatus_NullStatus() {
        // given
        when(userDetails.getId()).thenReturn(1L);
        List<SectorIntroductionRequestDTO> requests = List.of(requestDTO);
        when(sectorIntroductionRequestService.getRequestsListByCoordinatorWithStatus(1L, null))
                .thenReturn(requests);

        // when
        ResponseEntity<?> response = sectorsController.getSectorIntroductionRequestsWithStatus(userDetails, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(sectorIntroductionRequestService).getRequestsListByCoordinatorWithStatus(1L, null);
    }

    // ==================== TESTS FOR appointACoordinator ====================

    @Test
    void appointACoordinator_Success() throws RoleNotFoundException {
        // given
        doNothing().when(sectorService).addCoordinator(1L, 2L);

        // when
        ResponseEntity<?> response = sectorsController.appointACoordinator(1L, 2L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isEqualTo("Координтор был добавлен");

        verify(sectorService).addCoordinator(1L, 2L);
    }

    @Test
    void appointACoordinator_UserNotFound_ThrowsException() throws RoleNotFoundException {
        // given
        doThrow(new RuntimeException("User not found")).when(sectorService).addCoordinator(1L, 999L);

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            sectorsController.appointACoordinator(1L, 999L);
        });

        verify(sectorService).addCoordinator(1L, 999L);
    }

    // ==================== TESTS FOR removeCoordinatorFromSector ====================

    @Test
    void removeCoordinatorFromSector_Success() throws RoleNotFoundException {
        // given
        doNothing().when(sectorService).removeCoordinatorFromSector(1L, 2L);

        // when
        ResponseEntity<Void> response = sectorsController.removeCoordinatorFromSector(1L, 2L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(sectorService).removeCoordinatorFromSector(1L, 2L);
    }

    // ==================== TESTS FOR removeCurrentCoordinatorFromSector ====================

    @Test
    void removeCurrentCoordinatorFromSector_Success() throws RoleNotFoundException {
        // given
        when(userDetails.getId()).thenReturn(1L);
        doNothing().when(sectorService).removeCoordinatorFromSector(1L, 1L);

        // when
        ResponseEntity<Void> response = sectorsController.removeCurrentCoordinatorFromSector(1L, userDetails);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(sectorService).removeCoordinatorFromSector(1L, 1L);
    }

    // ==================== TESTS FOR kickParticipantFromSector ====================

    @Test
    void kickParticipantFromSector_Success() throws RoleNotFoundException {
        // given
        when(userDetails.getId()).thenReturn(1L);
        doNothing().when(sectorService).kickParticipantFromSector(1L, 1L, 3L);

        // when
        ResponseEntity<Void> response = sectorsController.kickParticipantFromSector(1L, 3L, userDetails);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(sectorService).kickParticipantFromSector(1L, 1L, 3L);
    }

    // ==================== TESTS FOR leaveSector ====================

    @Test
    void leaveSector_Success() {
        // given
        when(userDetails.getId()).thenReturn(1L);
        doNothing().when(sectorService).leaveSector(1L, 1L);

        // when
        ResponseEntity<Void> response = sectorsController.leaveSector(1L, userDetails);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(sectorService).leaveSector(1L, 1L);
    }

    // ==================== EDGE CASES ====================

    @Test
    void getSectors_WithNullUserDetails_ThrowsException() {
        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> {
            sectorsController.getSectors(null);
        });
    }

    @Test
    void createSectorIntroductionRequest_WithNullUserDetails_ThrowsException() {
        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> {
            sectorsController.createSectorIntroductionRequest(null, 1L);
        });
    }
}
package org.example.ais_sst.controllers;

import org.example.ais_sst.controller.EventRoleController;
import org.example.ais_sst.dto.event_roles.EventRoleCreateDTO;
import org.example.ais_sst.dto.event_roles.EventRoleFilterDTO;
import org.example.ais_sst.dto.event_roles.EventRoleResponseDTO;
import org.example.ais_sst.dto.event_roles.EventRoleUpdateDTO;
import org.example.ais_sst.service.eventService.EventRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventRoleControllerTest {

    @Mock
    private EventRoleService eventRoleService;

    @InjectMocks
    private EventRoleController eventRoleController;

    private EventRoleCreateDTO createDTO;
    private EventRoleUpdateDTO updateDTO;
    private EventRoleResponseDTO responseDTO;
    private Page<EventRoleResponseDTO> responsePage;

    @BeforeEach
    void setUp() {
        createDTO = EventRoleCreateDTO.builder()
                .eventId(1L)
                .globalEventRoleId(1L)
                .capacity(10)
                .reserveCapacity(2)
                .build();

        updateDTO = EventRoleUpdateDTO.builder()
                .capacity(15)
                .reserveCapacity(3)
                .deleted(false)
                .build();

        responseDTO = EventRoleResponseDTO.builder()
                .id(1L)
                .eventId(1L)
                .globalEventRoleId(1L)
                .capacity(10)
                .reserveCapacity(2)
                .deleted(false)
                .build();

        responsePage = new PageImpl<>(List.of(responseDTO));
    }

    // ==================== TESTS FOR createEventRole ====================

    @Test
    void createEventRole_Success() {
        // given
        when(eventRoleService.createEventRole(any(EventRoleCreateDTO.class))).thenReturn(responseDTO);

        // when
        ResponseEntity<EventRoleResponseDTO> response = eventRoleController.createEventRole(createDTO);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(responseDTO);
        assertThat(response.getBody().getId()).isEqualTo(1L);

        verify(eventRoleService).createEventRole(createDTO);
    }

    @Test
    void createEventRole_WithInvalidData_ThrowsException() {
        // given
        EventRoleCreateDTO invalidDTO = EventRoleCreateDTO.builder().build();
        when(eventRoleService.createEventRole(any(EventRoleCreateDTO.class)))
                .thenThrow(new RuntimeException("Invalid data"));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            eventRoleController.createEventRole(invalidDTO);
        });

        verify(eventRoleService).createEventRole(invalidDTO);
    }

    // ==================== TESTS FOR getEventRoleById ====================

    @Test
    void getEventRoleById_Success() {
        // given
        when(eventRoleService.getEventRoleById(1L)).thenReturn(responseDTO);

        // when
        ResponseEntity<EventRoleResponseDTO> response = eventRoleController.getEventRoleById(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(responseDTO);
        assertThat(response.getBody().getId()).isEqualTo(1L);

        verify(eventRoleService).getEventRoleById(1L);
    }

    @Test
    void getEventRoleById_NotFound_ThrowsException() {
        // given
        when(eventRoleService.getEventRoleById(999L)).thenThrow(new RuntimeException("Event role not found"));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            eventRoleController.getEventRoleById(999L);
        });

        verify(eventRoleService).getEventRoleById(999L);
    }

    // ==================== TESTS FOR updateEventRole ====================

    @Test
    void updateEventRole_Success() {
        // given
        when(eventRoleService.updateEventRole(eq(1L), any(EventRoleUpdateDTO.class))).thenReturn(responseDTO);

        // when
        ResponseEntity<EventRoleResponseDTO> response = eventRoleController.updateEventRole(1L, updateDTO);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(responseDTO);

        verify(eventRoleService).updateEventRole(eq(1L), any(EventRoleUpdateDTO.class));
    }

    @Test
    void updateEventRole_WithInvalidData_ThrowsException() {
        // given
        EventRoleUpdateDTO invalidDTO = EventRoleUpdateDTO.builder().build();
        when(eventRoleService.updateEventRole(eq(1L), any(EventRoleUpdateDTO.class)))
                .thenThrow(new RuntimeException("Invalid update data"));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            eventRoleController.updateEventRole(1L, invalidDTO);
        });

        verify(eventRoleService).updateEventRole(eq(1L), any(EventRoleUpdateDTO.class));
    }

    // ==================== TESTS FOR deleteEventRole ====================

    @Test
    void deleteEventRole_Success() {
        // given
        doNothing().when(eventRoleService).deleteEventRole(1L);

        // when
        ResponseEntity<Void> response = eventRoleController.deleteEventRole(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();

        verify(eventRoleService).deleteEventRole(1L);
    }

    @Test
    void deleteEventRole_NotFound_ThrowsException() {
        // given
        doThrow(new RuntimeException("Event role not found")).when(eventRoleService).deleteEventRole(999L);

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            eventRoleController.deleteEventRole(999L);
        });

        verify(eventRoleService).deleteEventRole(999L);
    }

    // ==================== TESTS FOR hardDeleteEventRole ====================

    @Test
    void hardDeleteEventRole_Success() {
        // given
        doNothing().when(eventRoleService).hardDeleteEventRole(1L);

        // when
        ResponseEntity<Void> response = eventRoleController.hardDeleteEventRole(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();

        verify(eventRoleService).hardDeleteEventRole(1L);
    }

    @Test
    void hardDeleteEventRole_NotFound_ThrowsException() {
        // given
        doThrow(new RuntimeException("Event role not found")).when(eventRoleService).hardDeleteEventRole(999L);

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            eventRoleController.hardDeleteEventRole(999L);
        });

        verify(eventRoleService).hardDeleteEventRole(999L);
    }

    // ==================== TESTS FOR getAllEventRoles ====================

    @Test
    void getAllEventRoles_Success() {
        // given
        when(eventRoleService.getAllEventRoles(any(EventRoleFilterDTO.class), any(Pageable.class)))
                .thenReturn(responsePage);

        // when
        ResponseEntity<Page<EventRoleResponseDTO>> response = eventRoleController.getAllEventRoles(
                1L, 1L, 1L, false, 0, 20, "id", "ASC");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getId()).isEqualTo(1L);

        verify(eventRoleService).getAllEventRoles(any(EventRoleFilterDTO.class), any(Pageable.class));
    }

    @Test
    void getAllEventRoles_WithEmptyFilters() {
        // given
        when(eventRoleService.getAllEventRoles(any(EventRoleFilterDTO.class), any(Pageable.class)))
                .thenReturn(responsePage);

        // when
        ResponseEntity<Page<EventRoleResponseDTO>> response = eventRoleController.getAllEventRoles(
                null, null, null, null, 0, 20, "id", "ASC");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        verify(eventRoleService).getAllEventRoles(any(EventRoleFilterDTO.class), any(Pageable.class));
    }

    @Test
    void getAllEventRoles_WithCustomPagination() {
        // given
        when(eventRoleService.getAllEventRoles(any(EventRoleFilterDTO.class), any(Pageable.class)))
                .thenReturn(responsePage);

        // when
        ResponseEntity<Page<EventRoleResponseDTO>> response = eventRoleController.getAllEventRoles(
                null, null, null, null, 2, 50, "capacity", "DESC");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(eventRoleService).getAllEventRoles(any(EventRoleFilterDTO.class),
                eq(PageRequest.of(2, 50, Sort.by(Sort.Direction.DESC, "capacity"))));
    }

    @Test
    void getAllEventRoles_WithDeletedFlagTrue() {
        // given
        when(eventRoleService.getAllEventRoles(any(EventRoleFilterDTO.class), any(Pageable.class)))
                .thenReturn(responsePage);

        // when
        ResponseEntity<Page<EventRoleResponseDTO>> response = eventRoleController.getAllEventRoles(
                null, null, null, true, 0, 20, "id", "ASC");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(eventRoleService).getAllEventRoles(argThat(filter ->
                filter.getDeleted() != null && filter.getDeleted()), any(Pageable.class));
    }

    @Test
    void getAllEventRoles_WithSpecificFilters() {
        // given
        when(eventRoleService.getAllEventRoles(any(EventRoleFilterDTO.class), any(Pageable.class)))
                .thenReturn(responsePage);

        // when
        ResponseEntity<Page<EventRoleResponseDTO>> response = eventRoleController.getAllEventRoles(
                10L, 5L, 3L, false, 0, 20, "id", "ASC");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(eventRoleService).getAllEventRoles(argThat(filter ->
                filter.getId() != null && filter.getId() == 10L &&
                        filter.getEventId() != null && filter.getEventId() == 5L &&
                        filter.getGlobalEventRoleId() != null && filter.getGlobalEventRoleId() == 3L &&
                        filter.getDeleted() != null && !filter.getDeleted()
        ), any(Pageable.class));
    }

    @Test
    void getAllEventRoles_EmptyResult() {
        // given
        Page<EventRoleResponseDTO> emptyPage = new PageImpl<>(List.of());
        when(eventRoleService.getAllEventRoles(any(EventRoleFilterDTO.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // when
        ResponseEntity<Page<EventRoleResponseDTO>> response = eventRoleController.getAllEventRoles(
                null, null, null, null, 0, 20, "id", "ASC");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();

        verify(eventRoleService).getAllEventRoles(any(EventRoleFilterDTO.class), any(Pageable.class));
    }

    // ==================== TESTS FOR SORTING ====================

    @Test
    void getAllEventRoles_WithDescendingSorting() {
        // given
        when(eventRoleService.getAllEventRoles(any(EventRoleFilterDTO.class), any(Pageable.class)))
                .thenReturn(responsePage);

        // when
        ResponseEntity<Page<EventRoleResponseDTO>> response = eventRoleController.getAllEventRoles(
                null, null, null, null, 0, 20, "capacity", "DESC");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(eventRoleService).getAllEventRoles(any(EventRoleFilterDTO.class),
                eq(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "capacity"))));
    }

    @Test
    void getAllEventRoles_WithAscendingSorting() {
        // given
        when(eventRoleService.getAllEventRoles(any(EventRoleFilterDTO.class), any(Pageable.class)))
                .thenReturn(responsePage);

        // when
        ResponseEntity<Page<EventRoleResponseDTO>> response = eventRoleController.getAllEventRoles(
                null, null, null, null, 0, 20, "eventId", "ASC");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(eventRoleService).getAllEventRoles(any(EventRoleFilterDTO.class),
                eq(PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "eventId"))));
    }
}
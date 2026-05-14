package org.example.ais_sst.mock.controllers;

import org.example.ais_sst.controller.GlobalEventRolesController;
import org.example.ais_sst.dto.events.GlobalEventRoleCreateDTO;
import org.example.ais_sst.dto.events.GlobalEventRoleDTO;
import org.example.ais_sst.dto.events.GlobalEventRoleUpdateDTO;
import org.example.ais_sst.service.eventService.GlobalEventRolesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalEventRolesControllerTest {

    @Mock
    private GlobalEventRolesService rolesAsTheEventService;

    @InjectMocks
    private GlobalEventRolesController globalEventRolesController;

    private GlobalEventRoleCreateDTO createDTO;
    private GlobalEventRoleUpdateDTO updateDTO;
    private GlobalEventRoleDTO roleDTO;
    private List<GlobalEventRoleDTO> roleList;

    @BeforeEach
    void setUp() {
        createDTO = GlobalEventRoleCreateDTO.builder()
                .title("Волонтер")
                .description("Помощь в организации мероприятий")
                .sector_id(1L)
                .build();

        updateDTO = GlobalEventRoleUpdateDTO.builder()
                .title("Главный волонтер")
                .description("Главный помощник в организации")
                .sector_id(2L)
                .build();

        roleDTO = GlobalEventRoleDTO.builder()
                .id(1L)
                .title("Волонтер")
                .description("Помощь в организации мероприятий")
                .sectorId(1L)
                .sectorTitle("Спортивный сектор")
                .build();

        roleList = List.of(roleDTO);
    }

    // ==================== TESTS FOR createRole ====================

    @Test
    void createRole_Success() {
        // given
        when(rolesAsTheEventService.createRole(any(GlobalEventRoleCreateDTO.class))).thenReturn(roleDTO);

        // when
        ResponseEntity<GlobalEventRoleDTO> response = globalEventRolesController.createRole(createDTO);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(roleDTO);
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getTitle()).isEqualTo("Волонтер");

        verify(rolesAsTheEventService).createRole(createDTO);
    }

    @Test
    void createRole_WithInvalidData_ThrowsException() {
        // given
        GlobalEventRoleCreateDTO invalidDTO = GlobalEventRoleCreateDTO.builder().build();
        when(rolesAsTheEventService.createRole(any(GlobalEventRoleCreateDTO.class)))
                .thenThrow(new RuntimeException("Invalid role data"));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            globalEventRolesController.createRole(invalidDTO);
        });

        verify(rolesAsTheEventService).createRole(invalidDTO);
    }

    // ==================== TESTS FOR getRoleById ====================

    @Test
    void getRoleById_Success() {
        // given
        when(rolesAsTheEventService.getRoleById(1L)).thenReturn(roleDTO);

        // when
        ResponseEntity<GlobalEventRoleDTO> response = globalEventRolesController.getRoleById(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(roleDTO);
        assertThat(response.getBody().getId()).isEqualTo(1L);

        verify(rolesAsTheEventService).getRoleById(1L);
    }

    @Test
    void getRoleById_NotFound_ThrowsException() {
        // given
        when(rolesAsTheEventService.getRoleById(999L)).thenThrow(new RuntimeException("Role not found"));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            globalEventRolesController.getRoleById(999L);
        });

        verify(rolesAsTheEventService).getRoleById(999L);
    }

    // ==================== TESTS FOR getRoleByTitle ====================

    @Test
    void getRoleByTitle_Success() throws UnsupportedEncodingException {
        // given
        String title = "Волонтер";
        when(rolesAsTheEventService.getRoleByTitle(title)).thenReturn(roleDTO);

        // when
        ResponseEntity<GlobalEventRoleDTO> response = globalEventRolesController.getRoleByTitle(title, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(roleDTO);
        assertThat(response.getBody().getTitle()).isEqualTo("Волонтер");

        verify(rolesAsTheEventService).getRoleByTitle(title);
    }

    @Test
    void getRoleByTitle_WithEncodedTitle_Success() throws UnsupportedEncodingException {
        // given
        String encodedTitle = "%D0%92%D0%BE%D0%BB%D0%BE%D0%BD%D1%82%D0%B5%D1%80"; // "Волонтер" в URL encoding
        String decodedTitle = "Волонтер";

        when(rolesAsTheEventService.getRoleByTitle(decodedTitle)).thenReturn(roleDTO);

        // when
        ResponseEntity<GlobalEventRoleDTO> response = globalEventRolesController.getRoleByTitle(encodedTitle, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(roleDTO);

        verify(rolesAsTheEventService).getRoleByTitle(decodedTitle);
    }

    @Test
    void getRoleByTitle_WithSpaceInTitle_Success() throws UnsupportedEncodingException {
        // given
        String titleWithSpace = "Главный волонтер";
        GlobalEventRoleDTO spaceRoleDTO = GlobalEventRoleDTO.builder()
                .id(2L)
                .title(titleWithSpace)
                .build();

        when(rolesAsTheEventService.getRoleByTitle(titleWithSpace)).thenReturn(spaceRoleDTO);

        // when
        ResponseEntity<GlobalEventRoleDTO> response = globalEventRolesController.getRoleByTitle(titleWithSpace, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTitle()).isEqualTo(titleWithSpace);

        verify(rolesAsTheEventService).getRoleByTitle(titleWithSpace);
    }

    @Test
    void getRoleByTitle_WithRussianTitle_Success() throws UnsupportedEncodingException {
        // given
        String russianTitle = "Организатор";
        GlobalEventRoleDTO russianRoleDTO = GlobalEventRoleDTO.builder()
                .id(3L)
                .title(russianTitle)
                .build();

        when(rolesAsTheEventService.getRoleByTitle(russianTitle)).thenReturn(russianRoleDTO);

        // when
        ResponseEntity<GlobalEventRoleDTO> response = globalEventRolesController.getRoleByTitle(russianTitle, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTitle()).isEqualTo(russianTitle);

        verify(rolesAsTheEventService).getRoleByTitle(russianTitle);
    }

    @Test
    void getRoleByTitle_NotFound_ThrowsException() throws UnsupportedEncodingException {
        // given
        String title = "Несуществующая роль";
        when(rolesAsTheEventService.getRoleByTitle(title)).thenThrow(new RuntimeException("Role not found"));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            globalEventRolesController.getRoleByTitle(title, null);
        });

        verify(rolesAsTheEventService).getRoleByTitle(title);
    }

    // ==================== TESTS FOR getAllRoles ====================

    @Test
    void getAllRoles_Success() {
        // given
        when(rolesAsTheEventService.getAllRoles()).thenReturn(roleList);

        // when
        ResponseEntity<List<GlobalEventRoleDTO>> response = globalEventRolesController.getAllRoles();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getId()).isEqualTo(1L);
        assertThat(response.getBody().get(0).getTitle()).isEqualTo("Волонтер");

        verify(rolesAsTheEventService).getAllRoles();
    }

    @Test
    void getAllRoles_EmptyList_ReturnsEmptyList() {
        // given
        when(rolesAsTheEventService.getAllRoles()).thenReturn(List.of());

        // when
        ResponseEntity<List<GlobalEventRoleDTO>> response = globalEventRolesController.getAllRoles();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();

        verify(rolesAsTheEventService).getAllRoles();
    }

    // ==================== TESTS FOR updateRole ====================

    @Test
    void updateRole_Success() {
        // given
        GlobalEventRoleDTO updatedRoleDTO = GlobalEventRoleDTO.builder()
                .id(1L)
                .title("Главный волонтер")
                .description("Главный помощник в организации")
                .sectorId(2L)
                .sectorTitle("Новый сектор")
                .build();

        when(rolesAsTheEventService.updateRole(eq(1L), any(GlobalEventRoleUpdateDTO.class)))
                .thenReturn(updatedRoleDTO);

        // when
        ResponseEntity<GlobalEventRoleDTO> response = globalEventRolesController.updateRole(1L, updateDTO);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(updatedRoleDTO);
        assertThat(response.getBody().getTitle()).isEqualTo("Главный волонтер");

        verify(rolesAsTheEventService).updateRole(eq(1L), any(GlobalEventRoleUpdateDTO.class));
    }

    @Test
    void updateRole_NotFound_ThrowsException() {
        // given
        when(rolesAsTheEventService.updateRole(eq(999L), any(GlobalEventRoleUpdateDTO.class)))
                .thenThrow(new RuntimeException("Role not found"));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            globalEventRolesController.updateRole(999L, updateDTO);
        });

        verify(rolesAsTheEventService).updateRole(eq(999L), any(GlobalEventRoleUpdateDTO.class));
    }

    @Test
    void updateRole_WithInvalidData_ThrowsException() {
        // given
        GlobalEventRoleUpdateDTO invalidDTO = GlobalEventRoleUpdateDTO.builder().build();
        when(rolesAsTheEventService.updateRole(eq(1L), any(GlobalEventRoleUpdateDTO.class)))
                .thenThrow(new RuntimeException("Invalid update data"));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            globalEventRolesController.updateRole(1L, invalidDTO);
        });

        verify(rolesAsTheEventService).updateRole(eq(1L), any(GlobalEventRoleUpdateDTO.class));
    }

    // ==================== TESTS FOR deleteRole ====================

    @Test
    void deleteRole_Success() {
        // given
        doNothing().when(rolesAsTheEventService).deleteRole(1L);

        // when
        ResponseEntity<Void> response = globalEventRolesController.deleteRole(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();

        verify(rolesAsTheEventService).deleteRole(1L);
    }

    @Test
    void deleteRole_NotFound_ThrowsException() {
        // given
        doThrow(new RuntimeException("Role not found")).when(rolesAsTheEventService).deleteRole(999L);

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            globalEventRolesController.deleteRole(999L);
        });

        verify(rolesAsTheEventService).deleteRole(999L);
    }

    // ==================== EDGE CASES ====================

    @Test
    void getRoleByTitle_WithEmptyTitle_ThrowsException() {
        // given
        String emptyTitle = "";
        when(rolesAsTheEventService.getRoleByTitle(emptyTitle)).thenThrow(new RuntimeException("Title cannot be empty"));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            globalEventRolesController.getRoleByTitle(emptyTitle, null);
        });

        verify(rolesAsTheEventService).getRoleByTitle(emptyTitle);
    }

    @Test
    void getRoleByTitle_WithNullTitle_ThrowsException() {
        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            globalEventRolesController.getRoleByTitle(null, null);
        });
    }

    @Test
    void createRole_WithNullSector_ThrowsException() {
        // given
        GlobalEventRoleCreateDTO noSectorDTO = GlobalEventRoleCreateDTO.builder()
                .title("Роль без сектора")
                .build();

        when(rolesAsTheEventService.createRole(any(GlobalEventRoleCreateDTO.class)))
                .thenThrow(new RuntimeException("Sector not found"));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            globalEventRolesController.createRole(noSectorDTO);
        });

        verify(rolesAsTheEventService).createRole(noSectorDTO);
    }

    @Test
    void getRoleById_WithInvalidId_ThrowsException() {
        // given
        Long invalidId = -1L;
        when(rolesAsTheEventService.getRoleById(invalidId)).thenThrow(new RuntimeException("Invalid id"));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            globalEventRolesController.getRoleById(invalidId);
        });

        verify(rolesAsTheEventService).getRoleById(invalidId);
    }
}
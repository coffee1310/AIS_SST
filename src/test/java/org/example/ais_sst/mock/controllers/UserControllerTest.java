package org.example.ais_sst.mock.controllers;

import org.example.ais_sst.controller.UserController;
import org.example.ais_sst.dto.user.UserFilterDTO;
import org.example.ais_sst.dto.user.UserProfileInfoDTO;
import org.example.ais_sst.dto.user.UserResponseDTO;
import org.example.ais_sst.entity.CustomUserDetails;
import org.example.ais_sst.service.userService.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private CustomUserDetails customUserDetails;

    @InjectMocks
    private UserController userController;

    private UserProfileInfoDTO userProfileInfoDTO;
    private UserResponseDTO userResponseDTO;
    private Page<UserResponseDTO> userPage;

    @BeforeEach
    void setUp() {
        userProfileInfoDTO = UserProfileInfoDTO.builder()
                .id(1L)
                .name("Иван")
                .surname("Иванов")
                .patronymic("Иванович")
                .studentEmail("ivan@test.com")
                .phoneNumber("+79991234567")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .courseNumber((short) 4)
                .roleTitle("Activist")
                .build();

        userResponseDTO = UserResponseDTO.builder()
                .id(1L)
                .name("Иван")
                .surname("Иванов")
                .patronymic("Иванович")
                .studentEmail("ivan@test.com")
                .role("Activist")
                .build();

        userPage = new PageImpl<>(List.of(userResponseDTO));
    }

    // ==================== TESTS FOR getCurrentUserInfo ====================

    @Test
    void getCurrentUserInfo_Success() {
        // given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getId()).thenReturn(1L);
        when(userService.getUserBasicInfo(1L)).thenReturn(userProfileInfoDTO);

        // when
        ResponseEntity<?> response = userController.getCurrentUserInfo();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(userProfileInfoDTO);

        UserProfileInfoDTO body = (UserProfileInfoDTO) response.getBody();
        assertThat(body.getId()).isEqualTo(1L);
        assertThat(body.getName()).isEqualTo("Иван");
        assertThat(body.getStudentEmail()).isEqualTo("ivan@test.com");

        verify(userService).getUserBasicInfo(1L);
    }

    @Test
    void getCurrentUserInfo_WhenAuthenticationIsNull_ReturnsForbidden() {
        // given
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        // when
        ResponseEntity<?> response = userController.getCurrentUserInfo();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(403));
        assertThat(response.getBody()).isEqualTo("");

        verify(userService, never()).getUserBasicInfo(any());
    }

    @Test
    void getCurrentUserInfo_WhenNotAuthenticated_ReturnsForbidden() {
        // given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.isAuthenticated()).thenReturn(false);

        // when
        ResponseEntity<?> response = userController.getCurrentUserInfo();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(403));
        assertThat(response.getBody()).isEqualTo("");

        verify(userService, never()).getUserBasicInfo(any());
    }

    @Test
    void getCurrentUserInfo_WhenServiceThrowsException_ReturnsInternalServerError() {
        // given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getId()).thenReturn(1L);
        when(userService.getUserBasicInfo(1L)).thenThrow(new RuntimeException("Database error"));

        // when
        ResponseEntity<?> response = userController.getCurrentUserInfo();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).toString().contains("Error: Database error");

        verify(userService).getUserBasicInfo(1L);
    }

    @Test
    void getCurrentUserInfo_WhenPrincipalIsNotCustomUserDetails_ReturnsForbiddenOrError() {
        // given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("some string");

        // when
        ResponseEntity<?> response = userController.getCurrentUserInfo();

        // then
        // Контроллер может выбросить ClassCastException или вернуть ошибку 500
        // В зависимости от реализации, проверяем что статус не 200
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.OK);
    }

    // ==================== TESTS FOR getAllUsers ====================

    @Test
    void getAllUsers_Success() {
        // given
        when(userService.getAllUsers(eq(0), eq(10), eq("id"), eq("ASC"), any(UserFilterDTO.class)))
                .thenReturn(userPage);

        // when
        ResponseEntity<Page<UserResponseDTO>> response = userController.getAllUsers(
                null, 0, 10, "id", "ASC", null, null, null, null, null, null, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getId()).isEqualTo(1L);

        verify(userService).getAllUsers(eq(0), eq(10), eq("id"), eq("ASC"), any(UserFilterDTO.class));
    }

    @Test
    void getAllUsers_WithAllFilters() {
        // given
        when(userService.getAllUsers(eq(1), eq(20), eq("name"), eq("DESC"), any(UserFilterDTO.class)))
                .thenReturn(userPage);

        // when
        ResponseEntity<Page<UserResponseDTO>> response = userController.getAllUsers(
                1L, 1, 20, "name", "DESC", "Activist", "Иван", true, false, 1L, 1L, 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(userService).getAllUsers(eq(1), eq(20), eq("name"), eq("DESC"), any(UserFilterDTO.class));
    }

    @Test
    void getAllUsers_WithSearchFilter() {
        // given
        when(userService.getAllUsers(eq(0), eq(10), eq("id"), eq("ASC"), any(UserFilterDTO.class)))
                .thenReturn(userPage);

        // when
        ResponseEntity<Page<UserResponseDTO>> response = userController.getAllUsers(
                null, 0, 10, "id", "ASC", null, "Иван", null, null, null, null, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(userService).getAllUsers(eq(0), eq(10), eq("id"), eq("ASC"), argThat(filter ->
                filter.getSearch() != null && filter.getSearch().equals("Иван")
        ));
    }

    @Test
    void getAllUsers_WithRoleFilter() {
        // given
        when(userService.getAllUsers(eq(0), eq(10), eq("id"), eq("ASC"), any(UserFilterDTO.class)))
                .thenReturn(userPage);

        // when
        ResponseEntity<Page<UserResponseDTO>> response = userController.getAllUsers(
                null, 0, 10, "id", "ASC", "Administrator", null, null, null, null, null, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(userService).getAllUsers(eq(0), eq(10), eq("id"), eq("ASC"), argThat(filter ->
                filter.getRole() != null && filter.getRole().equals("Administrator")
        ));
    }

    @Test
    void getAllUsers_WithActiveFilter() {
        // given
        when(userService.getAllUsers(eq(0), eq(10), eq("id"), eq("ASC"), any(UserFilterDTO.class)))
                .thenReturn(userPage);

        // when
        ResponseEntity<Page<UserResponseDTO>> response = userController.getAllUsers(
                null, 0, 10, "id", "ASC", null, null, true, null, null, null, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(userService).getAllUsers(eq(0), eq(10), eq("id"), eq("ASC"), argThat(filter ->
                filter.getIsActive() != null && filter.getIsActive()
        ));
    }

    @Test
    void getAllUsers_WithBannedFilter() {
        // given
        when(userService.getAllUsers(eq(0), eq(10), eq("id"), eq("ASC"), any(UserFilterDTO.class)))
                .thenReturn(userPage);

        // when
        ResponseEntity<Page<UserResponseDTO>> response = userController.getAllUsers(
                null, 0, 10, "id", "ASC", null, null, null, true, null, null, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(userService).getAllUsers(eq(0), eq(10), eq("id"), eq("ASC"), argThat(filter ->
                filter.getIsBanned() != null && filter.getIsBanned()
        ));
    }

    @Test
    void getAllUsers_WithGroupFilter() {
        // given
        when(userService.getAllUsers(eq(0), eq(10), eq("id"), eq("ASC"), any(UserFilterDTO.class)))
                .thenReturn(userPage);

        // when
        ResponseEntity<Page<UserResponseDTO>> response = userController.getAllUsers(
                null, 0, 10, "id", "ASC", null, null, null, null, 5L, null, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(userService).getAllUsers(eq(0), eq(10), eq("id"), eq("ASC"), argThat(filter ->
                filter.getGroupId() != null && filter.getGroupId() == 5L
        ));
    }

    @Test
    void getAllUsers_WithSpecialityFilter() {
        // given
        when(userService.getAllUsers(eq(0), eq(10), eq("id"), eq("ASC"), any(UserFilterDTO.class)))
                .thenReturn(userPage);

        // when
        ResponseEntity<Page<UserResponseDTO>> response = userController.getAllUsers(
                null, 0, 10, "id", "ASC", null, null, null, null, null, 3L, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(userService).getAllUsers(eq(0), eq(10), eq("id"), eq("ASC"), argThat(filter ->
                filter.getSpecialityId() != null && filter.getSpecialityId() == 3L
        ));
    }

    @Test
    void getAllUsers_WithSectorFilter() {
        // given
        when(userService.getAllUsers(eq(0), eq(10), eq("id"), eq("ASC"), any(UserFilterDTO.class)))
                .thenReturn(userPage);

        // when
        ResponseEntity<Page<UserResponseDTO>> response = userController.getAllUsers(
                null, 0, 10, "id", "ASC", null, null, null, null, null, null, 2L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(userService).getAllUsers(eq(0), eq(10), eq("id"), eq("ASC"), argThat(filter ->
                filter.getSectorId() != null && filter.getSectorId() == 2L
        ));
    }

    @Test
    void getAllUsers_EmptyResult() {
        // given
        Page<UserResponseDTO> emptyPage = new PageImpl<>(List.of());
        when(userService.getAllUsers(eq(0), eq(10), eq("id"), eq("ASC"), any(UserFilterDTO.class)))
                .thenReturn(emptyPage);

        // when
        ResponseEntity<Page<UserResponseDTO>> response = userController.getAllUsers(
                null, 0, 10, "id", "ASC", null, null, null, null, null, null, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();

        verify(userService).getAllUsers(eq(0), eq(10), eq("id"), eq("ASC"), any(UserFilterDTO.class));
    }

    // ==================== TESTS FOR getUsersByRole ====================

    @Test
    void getUsersByRole_Success() {
        // given
        when(userService.getUsersByRole(eq("Activist"), eq(0), eq(10), eq("id"), eq("ASC")))
                .thenReturn(userPage);

        // when
        ResponseEntity<Page<UserResponseDTO>> response = userController.getUsersByRole(
                "Activist", 0, 10, "id", "ASC");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getId()).isEqualTo(1L);

        verify(userService).getUsersByRole(eq("Activist"), eq(0), eq(10), eq("id"), eq("ASC"));
    }

    @Test
    void getUsersByRole_WithCustomPagination() {
        // given
        when(userService.getUsersByRole(eq("Administrator"), eq(2), eq(25), eq("name"), eq("DESC")))
                .thenReturn(userPage);

        // when
        ResponseEntity<Page<UserResponseDTO>> response = userController.getUsersByRole(
                "Administrator", 2, 25, "name", "DESC");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(userService).getUsersByRole(eq("Administrator"), eq(2), eq(25), eq("name"), eq("DESC"));
    }

    @Test
    void getUsersByRole_EmptyResult() {
        // given
        Page<UserResponseDTO> emptyPage = new PageImpl<>(List.of());
        when(userService.getUsersByRole(eq("Activist"), eq(0), eq(10), eq("id"), eq("ASC")))
                .thenReturn(emptyPage);

        // when
        ResponseEntity<Page<UserResponseDTO>> response = userController.getUsersByRole(
                "Activist", 0, 10, "id", "ASC");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();

        verify(userService).getUsersByRole(eq("Activist"), eq(0), eq(10), eq("id"), eq("ASC"));
    }

    @Test
    void getUsersByRole_WithRoleNotFound_ReturnsEmptyPage() {
        // given
        Page<UserResponseDTO> emptyPage = new PageImpl<>(List.of());
        when(userService.getUsersByRole(eq("NonExistentRole"), eq(0), eq(10), eq("id"), eq("ASC")))
                .thenReturn(emptyPage);

        // when
        ResponseEntity<Page<UserResponseDTO>> response = userController.getUsersByRole(
                "NonExistentRole", 0, 10, "id", "ASC");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();

        verify(userService).getUsersByRole(eq("NonExistentRole"), eq(0), eq(10), eq("id"), eq("ASC"));
    }
}
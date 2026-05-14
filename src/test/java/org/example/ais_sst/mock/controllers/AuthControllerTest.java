package org.example.ais_sst.mock.controllers;

import org.example.ais_sst.controller.AuthController;
import org.example.ais_sst.dto.request.LoginRequest;
import org.example.ais_sst.dto.request.RefreshTokenRequest;
import org.example.ais_sst.dto.response.JwtResponse;
import org.example.ais_sst.dto.user.UserSummaryDTO;
import org.example.ais_sst.entity.*;
import org.example.ais_sst.entity.enums.Gender;
import org.example.ais_sst.exception.GroupDoesNotExistException;
import org.example.ais_sst.exception.SpecialityDoesNotExistException;
import org.example.ais_sst.exception.TokenRefreshException;
import org.example.ais_sst.repository.GroupRepository;
import org.example.ais_sst.repository.RoleRepository;
import org.example.ais_sst.repository.SpecialityRepository;
import org.example.ais_sst.repository.UserRepository;
import org.example.ais_sst.security.jwt.JwtUtils;
import org.example.ais_sst.service.tokens.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private SpecialityRepository specialityRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private Authentication authentication;

    @Mock
    private CustomUserDetails customUserDetails;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AuthController authController;

    private LoginRequest loginRequest;
    private RefreshTokenRequest refreshTokenRequest;
    private UserSummaryDTO userSummaryDTO;
    private User user;
    private Role role;
    private Group group;
    private Speciality speciality;
    private RefreshToken refreshToken;
    private RefreshToken newRefreshToken;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setEmail("ivan@test.com");
        loginRequest.setPassword("password123");

        refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken("refresh-token-123");

        role = Role.builder()
                .id(1L)
                .title("Activist")
                .build();

        group = Group.builder()
                .id(1L)
                .title("ПИ-101")
                .build();

        speciality = Speciality.builder()
                .id(1L)
                .title("Информационные системы и программирование")
                .shortTitle("ИСП")
                .build();

        user = User.builder()
                .id(1L)
                .name("Иван")
                .surname("Иванов")
                .patronymic("Иванович")
                .studentEmail("ivan@test.com")
                .phoneNumber("+79991234567")
                .password("encodedPassword")
                .studentIdNumber(12345)
                .courseNumber((short) 3)
                .role(role)
                .group(group)
                .speciality(speciality)
                .isActive(true)
                .isBanned(false)
                .build();

        userSummaryDTO = UserSummaryDTO.builder()
                .name("Иван")
                .surname("Иванов")
                .patronymic("Иванович")
                .gender("Мужчина")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .studentEmail("ivan@test.com")
                .phoneNumber("+79991234567")
                .password("password123")
                .studentIdNumber(12345)
                .courseNumber((short) 3)
                .group_id(1L)
                .speciality_id(1L)
                .build();

        refreshToken = RefreshToken.builder()
                .id(1L)
                .token("refresh-token-123")
                .user(user)
                .build();

        newRefreshToken = RefreshToken.builder()
                .id(2L)
                .token("new-refresh-token-456")
                .user(user)
                .build();
    }

    // ==================== TESTS FOR LOGIN ====================

    @Test
    void authenticateUser_Success() {
        // given
        lenient().when(customUserDetails.getId()).thenReturn(1L);
        lenient().when(customUserDetails.getUsername()).thenReturn("ivan@test.com");
        lenient().when(customUserDetails.getName()).thenReturn("Иван");
        lenient().when(customUserDetails.getSurname()).thenReturn("Иванов");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt-token-123");
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(refreshToken);

        // when
        ResponseEntity<?> response = authController.authenticateUser(loginRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(JwtResponse.class);

        JwtResponse jwtResponse = (JwtResponse) response.getBody();
        assertThat(jwtResponse.getToken()).isEqualTo("jwt-token-123");
        assertThat(jwtResponse.getRefreshToken()).isEqualTo("refresh-token-123");
        assertThat(jwtResponse.getId()).isEqualTo(1L);
        assertThat(jwtResponse.getEmail()).isEqualTo("ivan@test.com");
        assertThat(jwtResponse.getName()).isEqualTo("Иван");
        assertThat(jwtResponse.getSurname()).isEqualTo("Иванов");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtils).generateJwtToken(authentication);
        verify(refreshTokenService).createRefreshToken(1L);
    }
        // ==================== TESTS FOR REFRESH TOKEN ====================

    @Test
    void refreshToken_Success() {
        // given
        when(refreshTokenService.findByToken("refresh-token-123"))
                .thenReturn(Optional.of(refreshToken));
        when(refreshTokenService.verifyExpiration(refreshToken)).thenReturn(refreshToken);
        when(jwtUtils.generateJwtToken(any(UsernamePasswordAuthenticationToken.class))).thenReturn("new-jwt-token");
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(newRefreshToken);

        // when
        ResponseEntity<?> response = authController.refreshToken(refreshTokenRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(JwtResponse.class);

        JwtResponse jwtResponse = (JwtResponse) response.getBody();
        assertThat(jwtResponse.getToken()).isEqualTo("new-jwt-token");
        assertThat(jwtResponse.getRefreshToken()).isEqualTo("new-refresh-token-456");
        assertThat(jwtResponse.getId()).isEqualTo(1L);

        verify(refreshTokenService).findByToken("refresh-token-123");
        verify(refreshTokenService).verifyExpiration(refreshToken);
        verify(jwtUtils).generateJwtToken(any(UsernamePasswordAuthenticationToken.class));
        verify(refreshTokenService).createRefreshToken(1L);
    }

    @Test
    void refreshToken_TokenNotFound_ThrowsException() {
        // given
        when(refreshTokenService.findByToken("refresh-token-123"))
                .thenReturn(Optional.empty());

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(TokenRefreshException.class, () -> {
            authController.refreshToken(refreshTokenRequest);
        });

        verify(refreshTokenService).findByToken("refresh-token-123");
        verify(refreshTokenService, never()).verifyExpiration(any());
        verify(jwtUtils, never()).generateJwtToken(any());
    }

    // ==================== TESTS FOR LOGOUT ====================

    @Test
    void logoutUser_Success() {
        // given
        String authHeader = "Bearer refresh-token-123";

        doNothing().when(refreshTokenService).revokeRefreshToken("refresh-token-123");

        // when
        ResponseEntity<?> response = authController.logoutUser(authHeader);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Logout successful");

        verify(refreshTokenService).revokeRefreshToken("refresh-token-123");
    }

    @Test
    void logoutUser_WithInvalidHeader_DoesNotRevoke() {
        // given
        String authHeader = "Invalid header";

        // when
        ResponseEntity<?> response = authController.logoutUser(authHeader);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Logout successful");

        verify(refreshTokenService, never()).revokeRefreshToken(anyString());
    }

    @Test
    void logoutUser_WithNullHeader_DoesNotRevoke() {
        // when
        ResponseEntity<?> response = authController.logoutUser(null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Logout successful");

        verify(refreshTokenService, never()).revokeRefreshToken(anyString());
    }

    // ==================== TESTS FOR LOGOUT ALL DEVICES ====================

    @Test
    void logoutAllDevices_Success() {
        // given
        when(customUserDetails.getId()).thenReturn(1L);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        SecurityContextHolder.setContext(securityContext);

        doNothing().when(refreshTokenService).revokeAllUserTokens(1L);

        // when
        ResponseEntity<?> response = authController.logoutAllDevices();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Logged out from all devices");

        verify(refreshTokenService).revokeAllUserTokens(1L);
    }

    @Test
    void logoutAllDevices_WithNoAuthentication_StillSucceeds() {
        // given
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        // when
        ResponseEntity<?> response = authController.logoutAllDevices();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Logged out from all devices");

        verify(refreshTokenService, never()).revokeAllUserTokens(anyLong());
    }

    // ==================== TESTS FOR REGISTER ====================

    @Test
    void registerUser_Success() {
        // given
        when(userRepository.existsByStudentEmail(userSummaryDTO.getStudentEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(userSummaryDTO.getPhoneNumber())).thenReturn(false);
        when(roleRepository.findByTitle("Activist")).thenReturn(Optional.of(role));
        when(groupRepository.findGroupById(1L)).thenReturn(Optional.of(group));
        when(specialityRepository.findSpecialityById(1L)).thenReturn(Optional.of(speciality));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // when
        ResponseEntity<?> response = authController.registerUser(userSummaryDTO);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Пользователь успешно зарегистрирован!");

        verify(userRepository).existsByStudentEmail("ivan@test.com");
        verify(userRepository).existsByPhoneNumber("+79991234567");
        verify(roleRepository).findByTitle("Activist");
        verify(groupRepository).findGroupById(1L);
        verify(specialityRepository).findSpecialityById(1L);
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_EmailAlreadyExists_ReturnsBadRequest() {
        // given
        when(userRepository.existsByStudentEmail(userSummaryDTO.getStudentEmail())).thenReturn(true);

        // when
        ResponseEntity<?> response = authController.registerUser(userSummaryDTO);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Ошибка: Email уже используется!");

        verify(userRepository).existsByStudentEmail("ivan@test.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_PhoneAlreadyExists_ReturnsBadRequest() {
        // given
        when(userRepository.existsByStudentEmail(userSummaryDTO.getStudentEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(userSummaryDTO.getPhoneNumber())).thenReturn(true);

        // when
        ResponseEntity<?> response = authController.registerUser(userSummaryDTO);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Ошибка: Телефон уже используется!");

        verify(userRepository).existsByStudentEmail("ivan@test.com");
        verify(userRepository).existsByPhoneNumber("+79991234567");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_RoleNotFound_ReturnsBadRequest() {
        // given
        when(userRepository.existsByStudentEmail(userSummaryDTO.getStudentEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(userSummaryDTO.getPhoneNumber())).thenReturn(false);
        when(roleRepository.findByTitle("Activist")).thenReturn(Optional.empty());

        // when
        ResponseEntity<?> response = authController.registerUser(userSummaryDTO);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().toString()).contains("Ошибка при регистрации");

        verify(roleRepository).findByTitle("Activist");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_GroupNotFound_ReturnsBadRequest() {
        // given
        when(userRepository.existsByStudentEmail(userSummaryDTO.getStudentEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(userSummaryDTO.getPhoneNumber())).thenReturn(false);
        when(roleRepository.findByTitle("Activist")).thenReturn(Optional.of(role));
        when(groupRepository.findGroupById(1L)).thenReturn(Optional.empty());

        // when
        ResponseEntity<?> response = authController.registerUser(userSummaryDTO);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().toString()).contains("Ошибка при регистрации");

        verify(groupRepository).findGroupById(1L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_SpecialityNotFound_ReturnsBadRequest() {
        // given
        when(userRepository.existsByStudentEmail(userSummaryDTO.getStudentEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(userSummaryDTO.getPhoneNumber())).thenReturn(false);
        when(roleRepository.findByTitle("Activist")).thenReturn(Optional.of(role));
        when(groupRepository.findGroupById(1L)).thenReturn(Optional.of(group));
        when(specialityRepository.findSpecialityById(1L)).thenReturn(Optional.empty());

        // when
        ResponseEntity<?> response = authController.registerUser(userSummaryDTO);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().toString()).contains("Ошибка при регистрации");

        verify(specialityRepository).findSpecialityById(1L);
        verify(userRepository, never()).save(any());
    }
}
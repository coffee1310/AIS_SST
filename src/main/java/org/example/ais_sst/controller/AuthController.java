package org.example.ais_sst.controller;

import org.example.ais_sst.controller.base.BaseController;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.service.tokens.RefreshTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SpecialityRepository specialityRepository;
    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        logInfo("/api/auth/login", "Login attempt for email: {}", loginRequest.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        logInfo("/api/auth/login", "User logged in successfully: {}", loginRequest.getEmail());

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

        JwtResponse jwtResponse = new JwtResponse(
                jwt,
                refreshToken.getToken(),
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getName(),
                userDetails.getSurname(),
                roles);

        return createSuccessResponse("Login successful", jwtResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        logInfo("/api/auth/refresh", "Refresh token request");

        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshToken -> refreshTokenService.verifyExpiration(refreshToken))
                .map(RefreshToken::getUser)
                .map(user -> {
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            user.getStudentEmail(), user.getPassword());
                    String accessToken = jwtUtils.generateJwtToken(authentication);

                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

                    List<String> roles = user.getRole() != null
                            ? List.of(user.getRole().getTitle())
                            : List.of("USER");

                    JwtResponse jwtResponse = new JwtResponse(
                            accessToken,
                            newRefreshToken.getToken(),
                            user.getId(),
                            user.getStudentEmail(),
                            user.getName(),
                            user.getSurname(),
                            roles);

                    return createSuccessResponse("Token refreshed successfully", jwtResponse);
                })
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken, "Refresh token not found"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@RequestHeader("Authorization") String authorizationHeader) {
        logInfo("/api/auth/logout", "Logout request");

        String token = null;
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
        }

        if (token != null) {
            refreshTokenService.revokeRefreshToken(token);
        }

        return createSuccessResponse("Logout successful", null);
    }

    @PostMapping("/logout/all")
    public ResponseEntity<?> logoutAllDevices() {
        logInfo("/api/auth/logout/all", "Logout from all devices request");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            refreshTokenService.revokeAllUserTokens(userDetails.getId());
        }

        return createSuccessResponse("Logged out from all devices", null);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserSummaryDTO userSummaryDTO) {
        logInfo("/api/auth/register", "Registration attempt for email: {}", userSummaryDTO.getStudentEmail());

        // Check if email already exists
        if (userRepository.existsByStudentEmail(userSummaryDTO.getStudentEmail())) {
            logWarn("/api/auth/register", "Registration failed - email already exists: {}",
                    userSummaryDTO.getStudentEmail());
            return ResponseEntity.badRequest().body("Ошибка: Email уже используется!");
        }

        // Check if phone already exists
        if (userRepository.existsByPhoneNumber(userSummaryDTO.getPhoneNumber())) {
            logWarn("/api/auth/register", "Registration failed - phone already exists: {}",
                    userSummaryDTO.getPhoneNumber());
            return ResponseEntity.badRequest().body("Ошибка: Телефон уже используется!");
        }

        try {
            // Get user role
            Role userRole = roleRepository.findByTitle("Activist")
                    .orElseThrow(() -> new RuntimeException("Ошибка: Роль Activist не найдена в БД!"));

            // Get user group
            Group userGroup = groupRepository.findGroupById(userSummaryDTO.getGroup_id())
                    .orElseThrow(() -> new GroupDoesNotExistException(
                            String.format("Ошибка: Группа с id: %s не существует", userSummaryDTO.getGroup_id())));

            // Get user speciality
            Speciality userSpeciality = specialityRepository.findSpecialityById(userSummaryDTO.getSpeciality_id())
                    .orElseThrow(() -> new SpecialityDoesNotExistException(
                            String.format("Ошибка: Специальность с id: %s не существует",
                                    userSummaryDTO.getSpeciality_id())));

            // Create new user
            User user = new User();
            user.setName(userSummaryDTO.getName());
            user.setSurname(userSummaryDTO.getSurname());
            user.setPatronymic(userSummaryDTO.getPatronymic());
            user.setGender(Gender.valueOf(userSummaryDTO.getGender()));
            user.setDateOfBirth(userSummaryDTO.getDateOfBirth());
            user.setStudentEmail(userSummaryDTO.getStudentEmail());
            user.setPhoneNumber(userSummaryDTO.getPhoneNumber());
            user.setPassword(passwordEncoder.encode(userSummaryDTO.getPassword()));
            user.setStudentIdNumber(userSummaryDTO.getStudentIdNumber());
            user.setCourseNumber(userSummaryDTO.getCourseNumber());
            user.setGroup(userGroup);
            user.setSpeciality(userSpeciality);
            user.setRole(userRole);
            user.setIsActive(true);
            user.setIsBanned(false);

            User savedUser = userRepository.save(user);
            logInfo("/api/auth/register", "User registered successfully with ID: {}", savedUser.getId());

            return createSuccessResponse("Пользователь успешно зарегистрирован!", null);

        } catch (Exception e) {
            logError("/api/auth/register", "Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Ошибка при регистрации: " + e.getMessage());
        }
    }
}
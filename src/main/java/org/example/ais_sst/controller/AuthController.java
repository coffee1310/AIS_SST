package org.example.ais_sst.controller;

import org.example.ais_sst.dto.request.LoginRequest;
import org.example.ais_sst.dto.request.RegisterRequest;
import org.example.ais_sst.dto.response.JwtResponse;
import org.example.ais_sst.entity.Role;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.entity.enums.Gender;
import org.example.ais_sst.repository.RoleRepository;
import org.example.ais_sst.repository.UserRepository;
import org.example.ais_sst.CustomUserDetails;
import org.example.ais_sst.jwt.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for email: {}", loginRequest.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        log.info("User logged in successfully: {}", loginRequest.getEmail());

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getName(),
                userDetails.getSurname(),
                roles));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Registration attempt for email: {}", registerRequest.getStudentEmail());

        // Проверка на существование email
        if (userRepository.existsByStudentEmail(registerRequest.getStudentEmail())) {
            log.warn("Registration failed - email already exists: {}", registerRequest.getStudentEmail());
            return ResponseEntity.badRequest().body("Ошибка: Email уже используется!");
        }

        // Проверка на существование телефона
        if (userRepository.existsByPhoneNumber(registerRequest.getPhoneNumber())) {
            log.warn("Registration failed - phone already exists: {}", registerRequest.getPhoneNumber());
            return ResponseEntity.badRequest().body("Ошибка: Телефон уже используется!");
        }

        try {
            // Получение роли USER (убедитесь, что такая роль существует в БД)
            Role userRole = roleRepository.findByTitle("USER")
                    .orElseThrow(() -> new RuntimeException("Ошибка: Роль USER не найдена в БД!"));

            // Создание нового пользователя
            User user = new User();
            user.setName(registerRequest.getName());
            user.setSurname(registerRequest.getSurname());
            user.setPatronymic(registerRequest.getPatronymic());
            user.setGender(Gender.valueOf(registerRequest.getGender()));
            user.setDateOfBirth(LocalDate.parse(registerRequest.getDateOfBirth(), DateTimeFormatter.ISO_DATE));
            user.setStudentEmail(registerRequest.getStudentEmail());
            user.setPhoneNumber(registerRequest.getPhoneNumber());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setRole(userRole);
            user.setIsActive(true);
            user.setIsBanned(false);

            User savedUser = userRepository.save(user);
            log.info("User registered successfully with ID: {}", savedUser.getId());

            return ResponseEntity.ok("Пользователь успешно зарегистрирован!");

        } catch (Exception e) {
            log.error("Registration failed: ", e);
            return ResponseEntity.badRequest().body("Ошибка при регистрации: " + e.getMessage());
        }
    }
}
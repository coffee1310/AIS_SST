package org.example.ais_sst.controller;

import org.example.ais_sst.dto.request.LoginRequest;
import org.example.ais_sst.dto.response.JwtResponse;
import org.example.ais_sst.dto.user.UserSummaryDTO;
import org.example.ais_sst.entity.*;
import org.example.ais_sst.entity.enums.Gender;
import org.example.ais_sst.exception.GroupDoesNotExistException;
import org.example.ais_sst.exception.SpecialityDoesNotExistException;
import org.example.ais_sst.repository.GroupRepository;
import org.example.ais_sst.repository.RoleRepository;
import org.example.ais_sst.repository.SpecialityRepository;
import org.example.ais_sst.repository.UserRepository;
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
    private final SpecialityRepository specialityRepository;
    private final GroupRepository groupRepository;
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

//    @PostMapping("/register")
//    public ResponseEntity<?> registerUser(@Valid @RequestBody UserSummaryDTO userSummaryDTO) {
//        log.info("Registration attempt for email: {}", userSummaryDTO.getStudentEmail());
//
//        if (userRepository.existsByStudentEmail(userSummaryDTO.getStudentEmail())) {
//            log.warn("Registration failed - email already exists: {}", userSummaryDTO.getStudentEmail());
//            return ResponseEntity.badRequest().body("Ошибка: Email уже используется!");
//        }
//
//        if (userRepository.existsByPhoneNumber(userSummaryDTO.getPhoneNumber())) {
//            log.warn("Registration failed - phone already exists: {}", userSummaryDTO.getPhoneNumber());
//            return ResponseEntity.badRequest().body("Ошибка: Телефон уже используется!");
//        }
//
//        try {
//            Role userRole = roleRepository.findByTitle("Activist")
//                    .orElseThrow(() -> new RuntimeException("Ошибка: Роль Activist не найдена в БД!"));
//
//            Group userGroup = groupRepository.findGroupById(userSummaryDTO.getGroup_id())
//                    .orElseThrow(() -> new GroupDoesNotExistException(String.format("Ошибка: Группа с id: %s не существует", userSummaryDTO.getGroup_id())));
//
//            Speciality userSpeciality = specialityRepository.findSpecialityById(userSummaryDTO.getSpeciality_id())
//                    .orElseThrow(() -> new SpecialityDoesNotExistException(String.format("Ошибка: Специальность с id: %s не существует", userSummaryDTO.getSpeciality_id())));
//
//            // Создание нового пользователя
//            User user = new User();
//            user.setName(userSummaryDTO.getName());
//            user.setSurname(userSummaryDTO.getSurname());
//            user.setPatronymic(userSummaryDTO.getPatronymic());
//            user.setGender(userSummaryDTO.getGender());
//            user.setDateOfBirth(userSummaryDTO.getDateOfBirth());
//            user.setStudentEmail(userSummaryDTO.getStudentEmail());
//            user.setPhoneNumber(userSummaryDTO.getPhoneNumber());
//            user.setPassword(passwordEncoder.encode(userSummaryDTO.getPassword()));
//            user.setStudentIdNumber(userSummaryDTO.getStudentIdNumber());
//            user.setCourseNumber(userSummaryDTO.getCourseNumber());
//            user.setGroup(userGroup);
//            user.setSpeciality(userSpeciality);
//            user.setRole(userRole);
//            user.setIsActive(true);
//            user.setIsBanned(false);
//
//            User savedUser = userRepository.save(user);
//            log.info("User registered successfully with ID: {}", savedUser.getId());
//
//            return ResponseEntity.ok("Пользователь успешно зарегистрирован!");
//
//        } catch (Exception e) {
//            log.error("Registration failed: ", e);
//            return ResponseEntity.badRequest().body("Ошибка при регистрации: " + e.getMessage());
//        }
//    }
}
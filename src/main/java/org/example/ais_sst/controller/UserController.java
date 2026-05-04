package org.example.ais_sst.controller;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.user.UserFilterDTO;
import org.example.ais_sst.dto.user.UserResponseDTO;
import org.example.ais_sst.entity.CustomUserDetails;
import org.example.ais_sst.dto.user.UserProfileInfoDTO;
import org.example.ais_sst.service.userService.UserService;
import io.swagger.v3.oas.annotations.Parameter;  // ИСПРАВЛЕННЫЙ ИМПОРТ!
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Сделать подсчет кол-ва баллов!
    @Transactional
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserInfo() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return new ResponseEntity<>("", HttpStatusCode.valueOf(403));
            }

            CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
            Long id = user.getId();
            UserProfileInfoDTO userProfileInfoDTO = userService.getUserBasicInfo(id);

            return ResponseEntity.ok(userProfileInfoDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @Parameter(description = "ID пользователя")
            @RequestParam(required = false) Long id,

            @Parameter(description = "Номер страницы (начиная с 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Размер страницы")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Поле для сортировки")
            @RequestParam(defaultValue = "id") String sortBy,

            @Parameter(description = "Направление сортировки (ASC/DESC)")
            @RequestParam(defaultValue = "ASC") String sortDirection,

            @Parameter(description = "Фильтр по роли")
            @RequestParam(required = false) String role,

            @Parameter(description = "Поиск по имени, фамилии или email")
            @RequestParam(required = false) String search,

            @Parameter(description = "Фильтр по активности")
            @RequestParam(required = false) Boolean isActive,

            @Parameter(description = "Фильтр по бану")
            @RequestParam(required = false) Boolean isBanned,

            @Parameter(description = "Фильтр по группе")
            @RequestParam(required = false) Long groupId,

            @Parameter(description = "Фильтр по специальности")
            @RequestParam(required = false) Long specialityId,
            @RequestParam(required = false) Long sectorId) {

        log.info("GET /api/users/all - Getting users with filters");

        UserFilterDTO filter = UserFilterDTO.builder()
                .id(id)
                .role(role)
                .search(search)
                .isActive(isActive)
                .isBanned(isBanned)
                .groupId(groupId)
                .specialityId(specialityId)
                .sectorId(sectorId)  // Добавьте эту строку
                .build();

        Page<UserResponseDTO> users = userService.getAllUsers(page, size, sortBy, sortDirection, filter);
        return ResponseEntity.ok(users);
    }

    /**
     * Получение пользователей по роли
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<Page<UserResponseDTO>> getUsersByRole(
            @PathVariable String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        log.info("GET /api/users/role/{} - Getting users by role", role);

        Page<UserResponseDTO> users = userService.getUsersByRole(role, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(users);
    }
}
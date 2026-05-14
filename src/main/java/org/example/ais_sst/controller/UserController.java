package org.example.ais_sst.controller;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.controller.base.BaseController;
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
public class UserController  extends BaseController {

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
            @RequestParam(required = false) Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isBanned,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long specialityId,
            @RequestParam(required = false) Long sectorId) {

        logInfo("/api/users/all", "Getting users with filters");

        UserFilterDTO filter = UserFilterDTO.builder()
                .id(id).role(role).search(search)
                .isActive(isActive).isBanned(isBanned)
                .groupId(groupId).specialityId(specialityId)
                .sectorId(sectorId).build();

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
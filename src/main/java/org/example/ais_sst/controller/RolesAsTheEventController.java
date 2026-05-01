package org.example.ais_sst.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.events.RolesAsTheEventCreateDTO;
import org.example.ais_sst.dto.events.RolesAsTheEventDTO;
import org.example.ais_sst.dto.events.RolesAsTheEventUpdateDTO;
import org.example.ais_sst.service.eventService.RolesAsTheEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RolesAsTheEventController {

    private final RolesAsTheEventService rolesAsTheEventService;

    /**
     * Создание новой роли (только для ADMIN)
     */
    @PostMapping
    public ResponseEntity<RolesAsTheEventDTO> createRole(@Valid @RequestBody RolesAsTheEventCreateDTO request) {
        log.info("POST /api/roles - Creating role: {}", request.getTitle());
        RolesAsTheEventDTO response = rolesAsTheEventService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получение роли по ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<RolesAsTheEventDTO> getRoleById(@PathVariable Long id) {
        log.info("GET /api/roles/{} - Getting role by id", id);
        RolesAsTheEventDTO role = rolesAsTheEventService.getRoleById(id);
        return ResponseEntity.ok(role);
    }

    /**
     * Получение роли по названию
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<RolesAsTheEventDTO> getRoleByTitle(@PathVariable String title) {
        log.info("GET /api/roles/title/{} - Getting role by title", title);
        RolesAsTheEventDTO role = rolesAsTheEventService.getRoleByTitle(title);
        return ResponseEntity.ok(role);
    }

    /**
     * Получение всех ролей
     */
    @GetMapping
    public ResponseEntity<List<RolesAsTheEventDTO>> getAllRoles() {
        log.info("GET /api/roles - Getting all roles");
        List<RolesAsTheEventDTO> roles = rolesAsTheEventService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

    /**
     * Получение роли по умолчанию
     */
    @GetMapping("/default")
    public ResponseEntity<RolesAsTheEventDTO> getDefaultRole() {
        log.info("GET /api/roles/default - Getting default role");
        RolesAsTheEventDTO role = rolesAsTheEventService.getDefaultRole();
        return ResponseEntity.ok(role);
    }

    /**
     * Обновление роли (только для ADMIN)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RolesAsTheEventDTO> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody RolesAsTheEventUpdateDTO request) {
        log.info("PUT /api/roles/{} - Updating role", id);
        RolesAsTheEventDTO response = rolesAsTheEventService.updateRole(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Удаление роли (только для ADMIN)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        log.info("DELETE /api/roles/{} - Deleting role", id);
        rolesAsTheEventService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}

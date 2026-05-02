package org.example.ais_sst.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.events.GlobalEventRoleCreateDTO;
import org.example.ais_sst.dto.events.GlobalEventRoleDTO;
import org.example.ais_sst.dto.events.GlobalEventRoleUpdateDTO;
import org.example.ais_sst.service.eventService.GlobalEventRolesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class GlobalEventRolesController {

    private final GlobalEventRolesService rolesAsTheEventService;

    /**
     * Создание новой роли (только для ADMIN)
     */
    @PostMapping
    public ResponseEntity<GlobalEventRoleDTO> createRole(@Valid @RequestBody GlobalEventRoleCreateDTO request) {
        log.info("POST /api/roles - Creating role: {}", request.getTitle());
        GlobalEventRoleDTO response = rolesAsTheEventService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получение роли по ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<GlobalEventRoleDTO> getRoleById(@PathVariable Long id) {
        log.info("GET /api/roles/{} - Getting role by id", id);
        GlobalEventRoleDTO role = rolesAsTheEventService.getRoleById(id);
        return ResponseEntity.ok(role);
    }

    /**
     * Получение роли по названию
     */
    @GetMapping(value = "/title/{title}", produces = "application/json;charset=UTF-8")
    public ResponseEntity<GlobalEventRoleDTO> getRoleByTitle(
            @PathVariable String title,
            HttpServletRequest request) throws UnsupportedEncodingException {

        log.info("GET /api/roles/title/{} - Getting role by title", title);

        // Декодируем URL, если нужно
        String decodedTitle = URLDecoder.decode(title, StandardCharsets.UTF_8.name());
        log.info("Decoded title: {}", decodedTitle);

        GlobalEventRoleDTO role = rolesAsTheEventService.getRoleByTitle(decodedTitle);
        return ResponseEntity.ok(role);
    }

    /**
     * Получение всех ролей
     */
    @GetMapping
    public ResponseEntity<List<GlobalEventRoleDTO>> getAllRoles() {
        log.info("GET /api/roles - Getting all roles");
        List<GlobalEventRoleDTO> roles = rolesAsTheEventService.getAllRoles();
        return ResponseEntity.ok(roles);
    }


    /**
     * Обновление роли (только для ADMIN)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalEventRoleDTO> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody GlobalEventRoleUpdateDTO request) {
        log.info("PUT /api/roles/{} - Updating role", id);
        GlobalEventRoleDTO response = rolesAsTheEventService.updateRole(id, request);
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

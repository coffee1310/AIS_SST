package org.example.ais_sst.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.event_roles.EventRoleCreateDTO;
import org.example.ais_sst.dto.event_roles.EventRoleFilterDTO;
import org.example.ais_sst.dto.event_roles.EventRoleResponseDTO;
import org.example.ais_sst.dto.event_roles.EventRoleUpdateDTO;
import org.example.ais_sst.entity.CustomUserDetails;
import org.example.ais_sst.service.eventService.EventRoleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.example.ais_sst.controller.base.BaseController;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/event-roles")
@RequiredArgsConstructor
@Tag(name = "Event Roles", description = "Управление ролями мероприятий")
public class EventRoleController extends BaseController {

    private final EventRoleService eventRoleService;

    @PostMapping
    @Operation(summary = "Создать роль мероприятия")
    public ResponseEntity<EventRoleResponseDTO> createEventRole(@Valid @RequestBody EventRoleCreateDTO dto) {
        logInfo("/api/event-roles", "Creating event role");
        EventRoleResponseDTO response = eventRoleService.createEventRole(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить роль мероприятия по ID")
    public ResponseEntity<EventRoleResponseDTO> getEventRoleById(@PathVariable Long id) {
        logInfo("/api/event-roles/{}", "Getting event role by id", id);
        EventRoleResponseDTO response = eventRoleService.getEventRoleById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить роль мероприятия")
    public ResponseEntity<EventRoleResponseDTO> updateEventRole(
            @PathVariable Long id,
            @Valid @RequestBody EventRoleUpdateDTO dto) {
        logInfo("/api/event-roles/{}", "Updating event role", id);
        EventRoleResponseDTO response = eventRoleService.updateEventRole(id, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить роль мероприятия (мягкое удаление)")
    public ResponseEntity<Void> deleteEventRole(@PathVariable Long id) {
        logInfo("/api/event-roles/{}", "Soft deleting event role", id);
        eventRoleService.deleteEventRole(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    @Operation(summary = "Полностью удалить роль мероприятия")
    public ResponseEntity<Void> hardDeleteEventRole(@PathVariable Long id) {
        logInfo("/api/event-roles/{}/hard", "Hard deleting event role", id);
        eventRoleService.hardDeleteEventRole(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Получить все роли мероприятий с фильтрами")
    public ResponseEntity<Page<EventRoleResponseDTO>> getAllEventRoles(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long globalEventRoleId,
            @RequestParam(required = false) Boolean isDeleted,
            @RequestParam(required = false) Boolean isMySector,  // НОВЫЙ ПАРАМЕТР
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection,
            @AuthenticationPrincipal CustomUserDetails userDetails) {  // Добавляем userDetails

        logInfo("/api/event-roles", "Getting event roles with filters");

        EventRoleFilterDTO filter = EventRoleFilterDTO.builder()
                .id(id)
                .eventId(eventId)
                .globalEventRoleId(globalEventRoleId)
                .deleted(isDeleted)
                .currentUserId(userDetails != null ? userDetails.getId() : null)  // Устанавливаем userId
                .isMySector(isMySector)  // Устанавливаем флаг
                .build();

        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        Page<EventRoleResponseDTO> eventRoles = eventRoleService.getAllEventRoles(filter, pageable);

        return ResponseEntity.ok(eventRoles);
    }
}
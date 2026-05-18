package org.example.ais_sst.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.controller.base.BaseController;
import org.example.ais_sst.dto.event_roles_application.RoleApplicationCreateDTO;
import org.example.ais_sst.dto.event_roles_application.RoleApplicationFilterDTO;
import org.example.ais_sst.dto.event_roles_application.RoleApplicationRejectDTO;
import org.example.ais_sst.dto.event_roles_application.RoleApplicationResponseDTO;
import org.example.ais_sst.entity.CustomUserDetails;
import org.example.ais_sst.entity.SectorParticipant;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;
import org.example.ais_sst.repository.SectorParticipantRepository;
import org.example.ais_sst.service.ApplicationsForTheRoleService.ApplicationsForTheRoleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/role-applications")
@RequiredArgsConstructor
@Tag(name = "Role Applications", description = "Управление заявками на роли")
public class ApplicationsForTheRoleController extends BaseController {

    private final ApplicationsForTheRoleService roleApplicationService;

    @PostMapping("/{eventRoleId}")
    @Operation(summary = "Подать заявку на роль")
    public ResponseEntity<RoleApplicationResponseDTO> createApplication(
            @PathVariable Long eventRoleId,
            @Valid @RequestBody(required = false) RoleApplicationCreateDTO createDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        logInfo("/api/role-applications/{}", "Creating application", eventRoleId);

        String description = createDTO != null ? createDTO.getDescription() : null;
        RoleApplicationResponseDTO response = roleApplicationService.createApplication(eventRoleId, userDetails.getId(), description);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить заявку по ID")
    public ResponseEntity<RoleApplicationResponseDTO> getApplicationById(@PathVariable Long id) {
        logInfo("/api/role-applications/{}", "Getting application", id);
        RoleApplicationResponseDTO response = roleApplicationService.getApplicationById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Одобрить заявку")
    public ResponseEntity<RoleApplicationResponseDTO> approveApplication(@PathVariable Long id) {
        logInfo("/api/role-applications/{}/approve", "Approving application", id);
        RoleApplicationResponseDTO response = roleApplicationService.approveApplication(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Отклонить заявку")
    public ResponseEntity<RoleApplicationResponseDTO> rejectApplication(
            @PathVariable Long id,
            @Valid @RequestBody RoleApplicationRejectDTO rejectDto) {

        logInfo("/api/role-applications/{}/reject", "Rejecting application", id);
        RoleApplicationResponseDTO response = roleApplicationService.rejectApplication(id, rejectDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Получить все заявки с фильтрами")
    public ResponseEntity<Page<RoleApplicationResponseDTO>> getAllApplications(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) Long sectorParticipantId,
            @RequestParam(required = false) Long eventRoleId,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) RoleApplicationStatuses status,
            @RequestParam(required = false) Boolean isReserve,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        logInfo("/api/role-applications", "Getting applications with filters");

        RoleApplicationFilterDTO filter = RoleApplicationFilterDTO.builder()
                .id(id)
                .sectorParticipantId(sectorParticipantId)
                .eventRoleId(eventRoleId)
                .eventId(eventId)
                .status(status)
                .isReserve(isReserve)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build();

        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        Page<RoleApplicationResponseDTO> applications = roleApplicationService.getAllApplications(filter, pageable);

        return ResponseEntity.ok(applications);
    }

    @GetMapping("/my")
    @Operation(summary = "Получить мои заявки")
    public ResponseEntity<Page<RoleApplicationResponseDTO>> getMyApplications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        logInfo("/api/role-applications/my", "Getting my applications");

        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        Page<RoleApplicationResponseDTO> applications = roleApplicationService.getMyApplications(userDetails.getId(), pageable);

        return ResponseEntity.ok(applications);
    }
}
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
import org.example.ais_sst.entity.EventOrganizerRequest;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;
import org.example.ais_sst.mapper.EventOrganizerRequestMapper;
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
import org.example.ais_sst.dto.event_roles_application.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/role-applications")
@RequiredArgsConstructor
@Tag(name = "Role Applications", description = "Управление заявками на роли")
public class ApplicationsForTheRoleController extends BaseController {

    private final ApplicationsForTheRoleService roleApplicationService;
    private final EventOrganizerRequestMapper eventOrganizerRequestMapper;

    @PostMapping("/{eventRoleId}")
    @Operation(summary = "Подать заявку на роль")
    public ResponseEntity<RoleApplicationResponseDTO> createApplication(
            @PathVariable Long eventRoleId,
            @Valid @RequestBody(required = false) RoleApplicationCreateDTO createDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {

        logInfo("/api/role-applications/{}", "Creating application", eventRoleId);

        String description = createDTO != null ? createDTO.getDescription() : null;
        RoleApplicationResponseDTO response = roleApplicationService.createApplication(eventRoleId, userDetails.getId(), description);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{eventRoleId}/orgainizer")
    @Operation(summary = "Подать заявку на роль организатора")
    public ResponseEntity<String> createApplicationOrganizer(
            @PathVariable Long eventRoleId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        logInfo("/api/role-applications/{}orgainizer", "Creating organizer application", eventRoleId);

        roleApplicationService.createApplicationOrganizer(eventRoleId, userDetails.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body("Заявка создана");
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
    @Operation(summary = "Получить все заявки с фильтрацией")
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
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("GET /api/role-applications - Getting all applications with filters");

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

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
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

    @GetMapping("/organizer/{id}")
    @Operation(summary = "Получить заявку на организатора по ID")
    public ResponseEntity<EventOrganizerRequestResponseDTO> getOrganizerApplicationById(@PathVariable Long id) {
        logInfo("/api/role-applications/organizer/{}", "Getting organizer application", id);

        EventOrganizerRequestResponseDTO request = roleApplicationService.getOrganizerApplicationById(id);
        return ResponseEntity.ok(request);
    }

    @PutMapping("/organizer/{id}/approve")
    @Operation(summary = "Одобрить заявку на организатора")
    public ResponseEntity<EventOrganizerRequestResponseDTO> approveOrganizerApplication(@PathVariable Long id) {
        logInfo("/api/role-applications/organizer/{}/approve", "Approving organizer application", id);

        EventOrganizerRequest request = roleApplicationService.approveOrganizerApplication(id);
        return ResponseEntity.ok(eventOrganizerRequestMapper.toResponseDto(request));
    }

    @PutMapping("/organizer/{id}/reject")
    @Operation(summary = "Отклонить заявку на организатора")
    public ResponseEntity<EventOrganizerRequestResponseDTO> rejectOrganizerApplication(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RoleApplicationRejectDTO rejectDto) {

        logInfo("/api/role-applications/organizer/{}/reject", "Rejecting organizer application", id);

        String rejectionReason = rejectDto != null ? rejectDto.getRejectionReason() : null;
        EventOrganizerRequest request = roleApplicationService.rejectOrganizerApplication(id, rejectionReason);

        return ResponseEntity.ok(eventOrganizerRequestMapper.toResponseDto(request));
    }

    @GetMapping("/organizer/event/{eventId}")
    @Operation(summary = "Получить все заявки на организатора для мероприятия")
    public ResponseEntity<List<EventOrganizerRequestResponseDTO>> getOrganizerApplicationsByEvent(
            @PathVariable Long eventId,
            @RequestParam(required = false) RoleApplicationStatuses status) {

        logInfo("/api/role-applications/organizer/event/{}", "Getting organizer applications for event", eventId);

        List<EventOrganizerRequest> requests;
        if (status != null) {
            requests = roleApplicationService.getOrganizerApplicationsByEventAndStatus(eventId, status);
        } else {
            requests = roleApplicationService.getOrganizerApplicationsByEvent(eventId);
        }

        return ResponseEntity.ok(requests.stream()
                .map(eventOrganizerRequestMapper::toResponseDto)
                .toList());
    }

    @GetMapping("/organizer/my")
    @Operation(summary = "Получить мои заявки на организатора")
    public ResponseEntity<List<EventOrganizerRequestResponseDTO>> getMyOrganizerApplications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        logInfo("/api/role-applications/organizer/my", "Getting my organizer applications");

        List<EventOrganizerRequest> requests = roleApplicationService.getMyOrganizerApplications(userDetails.getId());

        return ResponseEntity.ok(requests.stream()
                .map(eventOrganizerRequestMapper::toResponseDto)
                .toList());
    }

    @GetMapping("/organizer/my/event/{eventId}")
    @Operation(summary = "Получить мою заявку на организатора для конкретного мероприятия")
    public ResponseEntity<EventOrganizerRequestResponseDTO> getMyOrganizerApplicationForEvent(
            @PathVariable Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        logInfo("/api/role-applications/organizer/my/event/{}", "Getting my organizer application for event", eventId);

        EventOrganizerRequest request = roleApplicationService.getMyOrganizerApplicationForEvent(eventId, userDetails.getId());

        return ResponseEntity.ok(eventOrganizerRequestMapper.toResponseDto(request));
    }

    @GetMapping("/organizer/filter")
    @Operation(summary = "Фильтрация заявок на организатора")
    public ResponseEntity<List<EventOrganizerRequestResponseDTO>> filterOrganizerApplications(
            @Valid EventOrganizerRequestFilterDTO filter) {

        logInfo("/api/role-applications/organizer/filter", "Filtering organizer applications");

        List<EventOrganizerRequest> requests = roleApplicationService.filterOrganizerApplications(filter);

        return ResponseEntity.ok(requests.stream()
                .map(eventOrganizerRequestMapper::toResponseDto)
                .toList());
    }
}
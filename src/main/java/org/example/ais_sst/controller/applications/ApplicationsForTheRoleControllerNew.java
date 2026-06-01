package org.example.ais_sst.controller.applications;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ais_sst.dto.event_roles_application.*;
import org.example.ais_sst.entity.CustomUserDetails;
import org.example.ais_sst.service.ApplicationsForTheRoleService.ApplicationsForTheRoleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/role-applicationsNew")
@RequiredArgsConstructor
public class ApplicationsForTheRoleControllerNew extends BaseApplicationController<
        RoleApplicationResponseDTO,
        RoleApplicationCreateDTO,
        RoleApplicationRejectDTO,
        RoleApplicationFilterDTO> {

    private final RoleApplicationStrategy roleApplicationStrategy;
    private final ApplicationsForTheRoleService roleApplicationService;
    @Override
    protected RoleApplicationStrategy getStrategy() {
        return roleApplicationStrategy;
    }

    @Override
    protected String getApplicationName() {
        return "Заявка на роль";
    }

    // Специфичный метод создания с eventRoleId и userDetails
    @PostMapping("/{eventRoleId}")
    @Operation(summary = "Подать заявку на роль")
    public ResponseEntity<RoleApplicationResponseDTO> createApplication(
            @PathVariable Long eventRoleId,
            @Valid @RequestBody(required = false) RoleApplicationCreateDTO createDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        logInfo("/api/role-applications/{}", "Creating application", eventRoleId);

        String description = createDTO != null ? createDTO.getDescription() : null;
        RoleApplicationResponseDTO response = roleApplicationStrategy.createApplication(
                eventRoleId, userDetails.getId(), description);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Специфичный метод для заявок на организатора
    @PostMapping("/{eventRoleId}/organizer")
    @Operation(summary = "Подать заявку на роль организатора")
    public ResponseEntity<String> createOrganizerApplication(
            @PathVariable Long eventRoleId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        logInfo("/api/role-applications/{}/organizer", "Creating organizer application", eventRoleId);
        roleApplicationService.createApplicationOrganizer(eventRoleId, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body("Заявка создана");
    }

    @GetMapping("/my")
    @Operation(summary = "Получить мои заявки")
    public ResponseEntity<Page<RoleApplicationResponseDTO>> getMyApplications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        logInfo("/api/role-applications/my", "Getting my applications");
        Page<RoleApplicationResponseDTO> applications = roleApplicationService.getMyApplications(
                userDetails.getId(), pageable);
        return ResponseEntity.ok(applications);
    }

    // Методы для заявок на организатора
    @GetMapping("/organizer/{id}")
    @Operation(summary = "Получить заявку на организатора по ID")
    public ResponseEntity<EventOrganizerRequestResponseDTO> getOrganizerApplicationById(@PathVariable Long id) {
        return  ResponseEntity.status(HttpStatus.OK).body(roleApplicationService.getOrganizerApplicationById(id));
    }

    // ... остальные специфичные методы
}
package org.example.ais_sst.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTO;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTOSummary;
import org.example.ais_sst.dto.sector.SectorDTO;
import org.example.ais_sst.dto.sector.SectorParticipantResponseDTO;
import org.example.ais_sst.dto.sector.SectorUpdateDTO;
import org.example.ais_sst.dto.sector.SectorWithUserStatusDTO;
import org.example.ais_sst.entity.CustomUserDetails;
import org.example.ais_sst.entity.enums.SectorIntroductionStatus;
import org.example.ais_sst.service.sectorService.SectorIntroductionRequestService;
import org.example.ais_sst.service.sectorService.SectorService;
import org.example.ais_sst.utils.MeasurePerformance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.management.relation.RoleNotFoundException;
import java.net.http.HttpResponse;
import java.util.List;

import org.example.ais_sst.controller.base.BaseController;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sector")
public class SectorsController extends BaseController {

    private final SectorService sectorService;
    private final SectorIntroductionRequestService sectorIntroductionRequestService;

    @PostMapping
    public ResponseEntity<?> createSector(@Valid @RequestBody SectorDTO sectorDTO) throws RoleNotFoundException {
        SectorDTO sector = sectorService.createSector(sectorDTO);
        return new ResponseEntity<>(sectorDTO, HttpStatus.CREATED);
    }

    @GetMapping
    @MeasurePerformance
    public ResponseEntity<?> getSectors(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        List<SectorWithUserStatusDTO> sectors = sectorService.getSectorsWithUserStatus(customUserDetails.getId());
        return new ResponseEntity<>(sectors, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить сектор по ID")
    public ResponseEntity<SectorDTO> getSectorById(@PathVariable Long id) {
        SectorDTO sector = sectorService.getSectorById(id);
        return ResponseEntity.ok(sector);
    }

    @GetMapping("/{id}/participants")
    @Operation(summary = "Получить участников сектора")
    public ResponseEntity<Page<SectorParticipantResponseDTO>> getSectorParticipants(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "entryDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        logInfo("/api/sectors/{}/participants", "Getting participants", id);

        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        Page<SectorParticipantResponseDTO> participants = sectorService.getSectorParticipants(id, pageable);

        return ResponseEntity.ok(participants);
    }

    @GetMapping("/{id}/coordinator")
    @Operation(summary = "Получить координатора сектора")
    public ResponseEntity<SectorParticipantResponseDTO> getSectorCoordinator(@PathVariable Long id) {
        logInfo("/api/sectors/{}/coordinator", "Getting coordinator", id);

        SectorParticipantResponseDTO coordinator = sectorService.getSectorCoordinator(id);

        if (coordinator == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(coordinator);
    }

    @PostMapping("/{sector_id}")
    public ResponseEntity<?> createSectorIntroductionRequest(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long sector_id) {

        SectorIntroductionRequestDTO requestDTO = sectorIntroductionRequestService.createRequest(customUserDetails.getId(), sector_id);
        return new ResponseEntity<>(requestDTO, HttpStatus.CREATED);
    }

    @PutMapping("/accept/{id}")
    public ResponseEntity<?> acceptSectorIntroductionRequest(@PathVariable Long id) {
        SectorIntroductionRequestDTOSummary requestDTOSummary = sectorIntroductionRequestService.acceptRequest(id);

        return createSuccessResponse("Заявка принята. Член сектора создан.", requestDTOSummary);
    }

    @PutMapping("/reject/{id}")
    public ResponseEntity<?> rejectSectorIntroductionRequest(@PathVariable Long id) {
        SectorIntroductionRequestDTOSummary requestDTOSummary = sectorIntroductionRequestService.rejectRequest(id);

        return createSuccessResponse("Заявка отклонена.", requestDTOSummary);
    }

    @GetMapping("/introductions")
    public ResponseEntity<?> getSectorIntroductionRequests(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        List<SectorIntroductionRequestDTO> requestDTOList = sectorIntroductionRequestService
                .getRequestsListByCoordinator(customUserDetails.getId());

        return new ResponseEntity<>(requestDTOList, HttpStatus.OK);
    }

    @GetMapping("/introductions/filter")
    @Operation(summary = "Получить заявки на вступление в сектор с фильтром по статусу")
    public ResponseEntity<?> getSectorIntroductionRequestsWithStatus(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(required = false) SectorIntroductionStatus status) {

        logInfo("/api/sector/introductions/filter", "Getting requests with status: {} for coordinator: {}",
                status, customUserDetails.getId());

        List<SectorIntroductionRequestDTO> requestDTOList = sectorIntroductionRequestService
                .getRequestsListByCoordinatorWithStatus(customUserDetails.getId(), status);

        return new ResponseEntity<>(requestDTOList, HttpStatus.OK);
    }

    @PostMapping("/appoint_coordinator/{sector_id}")
    public ResponseEntity<?> appointACoordinator(
            @PathVariable Long sector_id,
            @RequestParam Long user_id) throws RoleNotFoundException {

        sectorService.addCoordinator(sector_id, user_id);
        return createSuccessResponse("Координатор был добавлен", null);
    }

    @DeleteMapping("/{sectorId}/coordinator/{userId}")
    public ResponseEntity<Void> removeCoordinatorFromSector(
            @PathVariable Long sectorId,
            @PathVariable Long userId) throws RoleNotFoundException {

        logInfo("/api/sector/{}/coordinator/{}", "Removing coordinator from sector", sectorId, userId);

        sectorService.removeCoordinatorFromSector(sectorId, userId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{sectorId}/coordinator")
    public ResponseEntity<Void> removeCurrentCoordinatorFromSector(
            @PathVariable Long sectorId,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws RoleNotFoundException {

        Long userId = userDetails.getId();
        logInfo("/api/sector/{}/coordinator", "Removing current user as coordinator from sector", sectorId);

        sectorService.removeCoordinatorFromSector(sectorId, userId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{sectorId}/kick/{userId}")
    public ResponseEntity<Void> kickParticipantFromSector(
            @PathVariable Long sectorId,
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws RoleNotFoundException {

        Long kick_user_id = userDetails.getId();
        logInfo("/api/sector/{}/kick/{}", "User {} kicking participant {} from sector",
                sectorId, userId, kick_user_id, userId);

        sectorService.kickParticipantFromSector(sectorId, kick_user_id, userId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{sectorId}/leave")
    @Operation(summary = "Выйти из сектора (активист)")
    public ResponseEntity<Void> leaveSector(
            @PathVariable Long sectorId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        logInfo("/api/sector/{}/leave", "User {} is leaving sector", sectorId, userDetails.getId());

        sectorService.leaveSector(sectorId, userDetails.getId());

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<SectorDTO> updateSector(
            @PathVariable Long id,
            @Valid @RequestBody SectorUpdateDTO updateDTO) throws RoleNotFoundException {

        // Проверяем, что id из URL совпадает с id в DTO
        updateDTO.setId(id);
        SectorDTO updatedSector = sectorService.updateSector(updateDTO);
        return ResponseEntity.ok(updatedSector);
    }

    @DeleteMapping("/deactivate/{id}")
    public ResponseEntity<?> deactivateSector(@PathVariable Long id) throws RoleNotFoundException {
        sectorService.deactivateSector(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/activate/{id}")
    public ResponseEntity<?> activateSector(@PathVariable Long id) {
        sectorService.activateSector(id);
        return ResponseEntity.noContent().build();
    }
}

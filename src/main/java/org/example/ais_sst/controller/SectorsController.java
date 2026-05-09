package org.example.ais_sst.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTO;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTOSummary;
import org.example.ais_sst.dto.sector.SectorDTO;
import org.example.ais_sst.dto.sector.SectorParticipantResponseDTO;
import org.example.ais_sst.dto.sector.SectorWithUserStatusDTO;
import org.example.ais_sst.entity.CustomUserDetails;
import org.example.ais_sst.entity.Sector;
import org.example.ais_sst.entity.enums.SectorIntroductionStatus;
import org.example.ais_sst.service.sectorService.SectorIntroductionRequestService;
import org.example.ais_sst.service.sectorService.SectorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.management.relation.RoleNotFoundException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sector")
public class SectorsController {

    private final SectorService sectorService;
    private final SectorIntroductionRequestService sectorIntroductionRequestService;

    @PostMapping
    public ResponseEntity<?> createSector(@Valid @RequestBody SectorDTO sectorDTO) throws RoleNotFoundException {
        SectorDTO sector = sectorService.createSector(sectorDTO);
        return new ResponseEntity<>(sectorDTO, HttpStatus.CREATED);
    }

    @GetMapping
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

    /**
     * Получить участников сектора по ID сектора
     */
    @GetMapping("/{id}/participants")
    @Operation(summary = "Получить участников сектора")
    public ResponseEntity<Page<SectorParticipantResponseDTO>> getSectorParticipants(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "entryDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info("GET /api/sectors/{}/participants - Getting participants", id);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        Page<SectorParticipantResponseDTO> participants = sectorService.getSectorParticipants(id, pageable);

        return ResponseEntity.ok(participants);
    }

    /**
     * Получить координатора сектора
     */
    @GetMapping("/{id}/coordinator")
    @Operation(summary = "Получить координатора сектора")
    public ResponseEntity<SectorParticipantResponseDTO> getSectorCoordinator(@PathVariable Long id) {
        log.info("GET /api/sectors/{}/coordinator - Getting coordinator", id);

        SectorParticipantResponseDTO coordinator = sectorService.getSectorCoordinator(id);

        if (coordinator == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(coordinator);
    }

    @PostMapping("/{sector_id}")
    public ResponseEntity<?> createSectorIntroductionRequest(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                             @PathVariable Long sector_id) {

        SectorIntroductionRequestDTO requestDTO = sectorIntroductionRequestService.createRequest(customUserDetails.getId(), sector_id);
        return new ResponseEntity<>(requestDTO, HttpStatus.CREATED);
    }

    @PutMapping("/accept/{id}")
    public ResponseEntity<?> acceptSectorIntroductionRequest(@PathVariable Long id) {

        SectorIntroductionRequestDTOSummary requestDTOSummary = sectorIntroductionRequestService.acceptRequest(id);

        return ResponseEntity.ok()
                .body(Map.of(
                        "message", "Заявка принята. Член сектора создан.",
                        "request", requestDTOSummary
                ));
    }

    @PutMapping ("/reject/{id}")
    public ResponseEntity<?> rejectSectorIntroductionRequest(@PathVariable Long id) {
        SectorIntroductionRequestDTOSummary requestDTOSummary = sectorIntroductionRequestService.rejectRequest(id);

        return ResponseEntity.ok()
                .body(Map.of(
                        "message", "Заявка принята. Член сектора создан.",
                        "request", requestDTOSummary
                ));
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

        log.info("GET /api/sector/introductions/filter - Getting requests with status: {} for coordinator: {}",
                status, customUserDetails.getId());

        List<SectorIntroductionRequestDTO> requestDTOList = sectorIntroductionRequestService
                .getRequestsListByCoordinatorWithStatus(customUserDetails.getId(), status);

        return new ResponseEntity<>(requestDTOList, HttpStatus.OK);
    }

    @PostMapping("/appoint_coordinator/{sector_id}")
    public ResponseEntity<?> appointACoordinator(@PathVariable Long sector_id, @RequestParam Long user_id) throws RoleNotFoundException {
        sectorService.addCoordinator(sector_id, user_id);
        return new ResponseEntity<>("Координтор был добавлен", HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/{sectorId}/coordinator/{userId}")
    public ResponseEntity<Void> removeCoordinatorFromSector(
            @PathVariable Long sectorId,
            @PathVariable Long userId) throws RoleNotFoundException {

        log.info("DELETE /api/sector/{}/coordinator/{} - Removing coordinator from sector", sectorId, userId);

        sectorService.removeCoordinatorFromSector(sectorId, userId);

        return ResponseEntity.noContent().build();
    }

    // Удаление координатора из текущего сектора (без передачи userId)
    @DeleteMapping("/{sectorId}/coordinator")
    public ResponseEntity<Void> removeCurrentCoordinatorFromSector(
            @PathVariable Long sectorId,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws RoleNotFoundException {

        Long userId = userDetails.getId();
        log.info("DELETE /api/sector/{}/coordinator - Removing current user as coordinator from sector", sectorId);

        sectorService.removeCoordinatorFromSector(sectorId, userId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{sectorId}/kick/{participantId}")
    public ResponseEntity<Void> kickParticipantFromSector(
            @PathVariable Long sectorId,
            @PathVariable Long participantId,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws RoleNotFoundException {

        Long coordinatorId = userDetails.getId();
        log.info("DELETE /api/sector/{}/kick/{} - Coordinator {} kicking participant {} from sector",
                sectorId, participantId, coordinatorId, participantId);

        sectorService.kickParticipantFromSector(sectorId, coordinatorId, participantId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{sectorId}/leave")
    @Operation(summary = "Выйти из сектора (активист)")
    public ResponseEntity<Void> leaveSector(
            @PathVariable Long sectorId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("DELETE /api/sector/{}/leave - User {} is leaving sector", sectorId, userDetails.getId());

        sectorService.leaveSector(sectorId, userDetails.getId());

        return ResponseEntity.noContent().build();
    }
}

package org.example.ais_sst.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTO;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTOSummary;
import org.example.ais_sst.dto.sector.SectorDTO;
import org.example.ais_sst.dto.sector.SectorWithUserStatusDTO;
import org.example.ais_sst.entity.CustomUserDetails;
import org.example.ais_sst.entity.Sector;
import org.example.ais_sst.service.sectorService.SectorIntroductionRequestService;
import org.example.ais_sst.service.sectorService.SectorService;
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
    public ResponseEntity<?> getSectorById(@PathVariable Long id) {
        SectorDTO sector = sectorService.getSectorById(id);
        return new ResponseEntity<>(sector, HttpStatus.OK);
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
}

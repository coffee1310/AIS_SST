package org.example.ais_sst.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTO;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTOSummary;
import org.example.ais_sst.dto.sector.SectorDTO;
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

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sector")
public class SectorsController {

    private final SectorService sectorService;
    private final SectorIntroductionRequestService sectorIntroductionRequestService;

    @PostMapping
    public ResponseEntity<?> createSector(@Valid @RequestBody SectorDTO sectorDTO) {
        SectorDTO sector = sectorService.createSector(sectorDTO);
        return new ResponseEntity<>(sectorDTO, HttpStatus.CREATED);
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

    @GetMapping
    public ResponseEntity<?> getSectorIntroductionRequests(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        List<SectorIntroductionRequestDTO> requestDTOList = sectorIntroductionRequestService
                .getRequestsListByCoordinator(customUserDetails.getId());

        return new ResponseEntity<>(requestDTOList, HttpStatus.OK);
    }

    @PostMapping("/appoint_coordinator/{sector_id}")
    public ResponseEntity<?> appointACoordinator(@PathVariable Long sector_id, @RequestParam Long user_id) {
        SectorDTO sectorDTO = sectorService.appointACoordinator(sector_id, user_id);
        return new ResponseEntity<>(sectorDTO, HttpStatus.ACCEPTED);
    }
}

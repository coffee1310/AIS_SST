package org.example.ais_sst.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTO;
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
}

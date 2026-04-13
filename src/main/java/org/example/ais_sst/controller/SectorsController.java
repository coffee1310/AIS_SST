package org.example.ais_sst.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ais_sst.dto.sector.SectorDTO;
import org.example.ais_sst.entity.Sector;
import org.example.ais_sst.service.sectorService.SectorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sector")
public class SectorsController {

    private final SectorService sectorService;

    @PostMapping
    public ResponseEntity<?> createSector(@Valid @RequestBody SectorDTO sectorDTO) {
        Sector sector = sectorService.createSector(sectorDTO);
        sectorDTO.setId(sector.getId());
        return new ResponseEntity<>(sectorDTO, HttpStatus.CREATED);
    }
}

package org.example.ais_sst.service.sectorService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.sector.SectorDTO;
import org.example.ais_sst.entity.Sector;
import org.example.ais_sst.mapper.SectorMapper;
import org.example.ais_sst.repository.SectorRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectorService {

    private final SectorRepository sectorRepository;
    private final SectorMapper sectorMapper;

    @Transactional
    public Sector createSector(SectorDTO sectorDTO) {
      log.info("Creating sector with id: {}", sectorDTO.getId());
      Sector sector = sectorMapper.toEntity(sectorDTO);

      return sectorRepository.save(sector);
    }
}

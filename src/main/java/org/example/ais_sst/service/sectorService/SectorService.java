package org.example.ais_sst.service.sectorService;

import lombok.RequiredArgsConstructor;
import org.example.ais_sst.repository.SectorRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SectorService {

    private final SectorRepository sectorRepository;

}

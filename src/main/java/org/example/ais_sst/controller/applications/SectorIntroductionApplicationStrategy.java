package org.example.ais_sst.controller.applications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTO;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTOSummary;
import org.example.ais_sst.service.sectorService.SectorIntroductionRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SectorIntroductionApplicationStrategy implements
        ApplicationStrategy <SectorIntroductionRequestDTO,
                SectorIntroductionRequestDTOSummary,
                SectorIntroductionRequestDTOSummary,
                Void>{

    private final SectorIntroductionRequestService sectorIntroductionRequestService;

    @Override
    public SectorIntroductionRequestDTO createApplication(SectorIntroductionRequestDTOSummary CreateDto) {
        return null;
    }

    @Override
    public SectorIntroductionRequestDTO rejectApplication(Long id, SectorIntroductionRequestDTOSummary RejectDto) {
        return null;
    }

    @Override
    public SectorIntroductionRequestDTO acceptApplication(Long id) {
        return null;
    }

    @Override
    public SectorIntroductionRequestDTO getById(Long id) {
        return null;
    }

    @Override
    public Page<SectorIntroductionRequestDTO> getAll(Void filter, Pageable page) {
        return null;
    }
}

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
        ApplicationStrategy<SectorIntroductionRequestDTOSummary,  // Изменено с SectorIntroductionRequestDTO
                SectorIntroductionRequestDTOSummary,
                SectorIntroductionRequestDTOSummary,
                Void> {

    private final SectorIntroductionRequestService sectorIntroductionRequestService;

    @Override
    public SectorIntroductionRequestDTOSummary createApplication(SectorIntroductionRequestDTOSummary createDto) {
        log.info("Creating sector introduction application");
        throw new UnsupportedOperationException("Use createApplication with userId and sectorId");
    }

    // Этот метод возвращает SectorIntroductionRequestDTO, а не SectorIntroductionRequestDTOSummary
    public SectorIntroductionRequestDTO createApplication(Long userId, Long sectorId) {
        return sectorIntroductionRequestService.createRequest(userId, sectorId);
    }

    @Override
    public SectorIntroductionRequestDTOSummary rejectApplication(Long id, SectorIntroductionRequestDTOSummary rejectDto) {
        log.info("Sector introduction request with id: {} was rejected", id);
        return sectorIntroductionRequestService.rejectRequest(id);  // Возвращает SectorIntroductionRequestDTOSummary
    }

    @Override
    public SectorIntroductionRequestDTOSummary acceptApplication(Long id) {
        log.info("Sector introduction request with id: {} was accepted", id);
        return sectorIntroductionRequestService.acceptRequest(id);  // Возвращает SectorIntroductionRequestDTOSummary
    }

    @Override
    public SectorIntroductionRequestDTOSummary getApplicationById(Long id) {
        log.info("Getting sector introduction request with id: {}", id);
        throw new UnsupportedOperationException("Method not implemented yet");
    }

    @Override
    public Page<SectorIntroductionRequestDTOSummary> getAllApplications(Void filter, Pageable pageable) {
        throw new UnsupportedOperationException("Use getRequestsListByCoordinator instead");
    }
}
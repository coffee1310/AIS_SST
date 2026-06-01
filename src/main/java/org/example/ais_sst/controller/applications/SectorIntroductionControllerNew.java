package org.example.ais_sst.controller.applications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.controller.base.BaseController;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTO;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTOSummary;
import org.example.ais_sst.entity.CustomUserDetails;
import org.example.ais_sst.entity.enums.SectorIntroductionStatus;
import org.example.ais_sst.service.sectorService.SectorIntroductionRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/sectorNew")
@RequiredArgsConstructor
public class SectorIntroductionControllerNew extends BaseController {

    private final SectorIntroductionApplicationStrategy sectorIntroductionStrategy;
    private final SectorIntroductionRequestService sectorIntroductionRequestService;

    @PostMapping("/{sectorId}/introduction")
    public ResponseEntity<SectorIntroductionRequestDTO> createIntroductionRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sectorId) {

        SectorIntroductionRequestDTO request = sectorIntroductionStrategy.createApplication(
                userDetails.getId(), sectorId);
        return new ResponseEntity<>(request, HttpStatus.CREATED);
    }

    @PutMapping("/introduction/{id}/accept")
    public ResponseEntity<?> acceptIntroductionRequest(@PathVariable Long id) {
        log.info("Accepting sector introduction request with id: {}", id);
        SectorIntroductionRequestDTOSummary request = sectorIntroductionStrategy.acceptApplication(id);
        return createSuccessResponse("Заявка принята. Член сектора создан.", request);
    }

    @PutMapping("/introduction/{id}/reject")
    public ResponseEntity<?> rejectIntroductionRequest(@PathVariable Long id) {
        log.info("Rejecting sector introduction request with id: {}", id);
        SectorIntroductionRequestDTOSummary request = sectorIntroductionStrategy.rejectApplication(id, null);
        return createSuccessResponse("Заявка отклонена.", request);
    }

    @GetMapping("/introductions")
    public ResponseEntity<List<SectorIntroductionRequestDTO>> getIntroductionRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<SectorIntroductionRequestDTO> requests = sectorIntroductionRequestService
                .getRequestsListByCoordinator(userDetails.getId());
        return new ResponseEntity<>(requests, HttpStatus.OK);
    }

    @GetMapping("/introductions/filter")
    public ResponseEntity<List<SectorIntroductionRequestDTO>> getIntroductionRequestsWithStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) SectorIntroductionStatus status) {

        log.info("Getting requests with status: {} for coordinator: {}", status, userDetails.getId());

        List<SectorIntroductionRequestDTO> requests = sectorIntroductionRequestService
                .getRequestsListByCoordinatorWithStatus(userDetails.getId(), status);
        return new ResponseEntity<>(requests, HttpStatus.OK);
    }
}
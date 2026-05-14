package org.example.ais_sst.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.controller.base.BaseController;
import org.example.ais_sst.entity.SocialStatus;
import org.example.ais_sst.service.socialStatusService.SocialStatusService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/social_status")
@RequiredArgsConstructor
public class SocialStatusController extends BaseController {

    private final SocialStatusService socialStatusService;

    @GetMapping
    public ResponseEntity<?> getSocialStatuses() {
        logInfo("/api/social_status", "Getting all social statuses");

        List<SocialStatus> socialStatuses = socialStatusService.getSocialStatuses();
        return createSuccessResponse(socialStatuses);
    }
}

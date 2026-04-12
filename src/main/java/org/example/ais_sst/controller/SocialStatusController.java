package org.example.ais_sst.controller;

import lombok.RequiredArgsConstructor;
import org.example.ais_sst.entity.SocialStatus;
import org.example.ais_sst.service.socialStatusService.SocialStatusService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/social_status")
@RequiredArgsConstructor
public class SocialStatusController {

    private final SocialStatusService socialStatusService;

    @GetMapping
    public ResponseEntity<?> getSocial_statuses() {
        List<SocialStatus> socialStatuses = socialStatusService.getSocialStatuses();
        return new ResponseEntity<>(socialStatuses, HttpStatus.OK);
    }
}

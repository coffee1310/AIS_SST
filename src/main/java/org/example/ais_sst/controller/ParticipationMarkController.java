package org.example.ais_sst.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.controller.base.BaseController;
import org.example.ais_sst.dto.event_participation.*;
import org.example.ais_sst.service.eventService.ParticipationMarkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/events/participation")
@RequiredArgsConstructor
@Tag(name = "Event Participation", description = "Управление отметками участников мероприятий")
public class ParticipationMarkController extends BaseController {

    private final ParticipationMarkService participationMarkService;

    @PostMapping("/mark")
    @Operation(summary = "Отметить участников, организаторов и записи об участии")
    public ResponseEntity<ParticipationMarkResponseDTO> markParticipation(
            @Valid @RequestBody ParticipationMarkRequestDTO request) {

        log.info("POST /api/events/participation/mark - Marking participation for event: {}", request.getEventId());
        ParticipationMarkResponseDTO response = participationMarkService.markParticipation(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{eventId}/mark-all-participants")
    @Operation(summary = "Отметить всех участников мероприятия")
    public ResponseEntity<ParticipationMarkResponseDTO> markAllParticipants(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "true") Boolean present) {

        log.info("POST /api/events/participation/{}/mark-all-participants - present: {}", eventId, present);
        ParticipationMarkResponseDTO response = participationMarkService.markAllParticipants(eventId, present);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{eventId}/mark-all-organizers")
    @Operation(summary = "Отметить всех организаторов мероприятия")
    public ResponseEntity<ParticipationMarkResponseDTO> markAllOrganizers(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "true") Boolean present) {

        log.info("POST /api/events/participation/{}/mark-all-organizers - present: {}", eventId, present);
        ParticipationMarkResponseDTO response = participationMarkService.markAllOrganizers(eventId, present);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{eventId}/stats")
    @Operation(summary = "Получить статистику по отметкам для мероприятия")
    public ResponseEntity<ParticipationStatsDTO> getParticipationStats(@PathVariable Long eventId) {
        log.info("GET /api/events/participation/{}/stats - Getting participation stats", eventId);
        ParticipationStatsDTO stats = participationMarkService.getParticipationStats(eventId);
        return ResponseEntity.ok(stats);
    }

    @PutMapping("/points")
    @Operation(summary = "Обновить баллы для сущности")
    public ResponseEntity<UpdatePointsResponseDTO> updatePoints(
            @Valid @RequestBody UpdatePointsRequestDTO request) {

        log.info("PUT /api/events/participation/points - Updating points for entity: {}, type: {}",
                request.getEntityId(), request.getEntityType());
        UpdatePointsResponseDTO response = participationMarkService.updatePoints(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/points/record/{recordId}")
    @Operation(summary = "Обновить баллы записи об участии (event_participation_records)")
    public ResponseEntity<UpdatePointsResponseDTO> updateParticipationRecordPoints(
            @PathVariable Long recordId,
            @RequestParam Integer points,
            @RequestParam(required = false) String reason) {

        log.info("PUT /api/events/participation/points/record/{} - Updating points to: {}", recordId, points);
        UpdatePointsResponseDTO response = participationMarkService.updateParticipationRecordPoints(
                recordId, points, reason);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset/participants/{eventId}")
    @Operation(summary = "Сбросить баллы всех участников мероприятия к значению по умолчанию")
    public ResponseEntity<UpdatePointsResponseDTO.BulkUpdateResponse> resetAllParticipantPoints(
            @PathVariable Long eventId) {

        log.info("POST /api/events/participation/reset/participants/{} - Resetting all participant points", eventId);
        UpdatePointsResponseDTO.BulkUpdateResponse response =
                participationMarkService.resetAllParticipantPoints(eventId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset/organizers/{eventId}")
    @Operation(summary = "Сбросить баллы всех организаторов мероприятия к значению по умолчанию")
    public ResponseEntity<UpdatePointsResponseDTO.BulkUpdateResponse> resetAllOrganizerPoints(
            @PathVariable Long eventId) {

        log.info("POST /api/events/participation/reset/organizers/{} - Resetting all organizer points", eventId);
        UpdatePointsResponseDTO.BulkUpdateResponse response =
                participationMarkService.resetAllOrganizerPoints(eventId);
        return ResponseEntity.ok(response);
    }
}
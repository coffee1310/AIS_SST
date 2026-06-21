package org.example.ais_sst.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.controller.base.BaseController;
import org.example.ais_sst.dto.event_participation.BulkUpdatePointsRequestDTO;
import org.example.ais_sst.dto.event_participation.UpdatePointsResponseDTO;
import org.example.ais_sst.service.eventService.ParticipationMarkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/events/points")
@RequiredArgsConstructor
@Tag(name = "Event Points", description = "Управление баллами участников мероприятий")
public class EventPointsController extends BaseController {

    private final ParticipationMarkService participationMarkService;

    @PutMapping("/participant/{participantId}")
    @Operation(summary = "Изменить баллы участника")
    public ResponseEntity<UpdatePointsResponseDTO> updateParticipantPoints(
            @PathVariable Long participantId,
            @RequestParam Integer points,
            @RequestParam(required = false) String reason) {

        log.info("PUT /api/events/points/participant/{} - Updating points to: {}", participantId, points);
        UpdatePointsResponseDTO response = participationMarkService.updateParticipantPoints(
                participantId, points, reason);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/organizer/{organizerId}")
    @Operation(summary = "Изменить баллы организатора")
    public ResponseEntity<UpdatePointsResponseDTO> updateOrganizerPoints(
            @PathVariable Long organizerId,
            @RequestParam Integer points,
            @RequestParam(required = false) String reason) {

        log.info("PUT /api/events/points/organizer/{} - Updating points to: {}", organizerId, points);
        UpdatePointsResponseDTO response = participationMarkService.updateOrganizerPoints(
                organizerId, points, reason);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/participation_record/{participation_record_id}")
    @Operation(summary = "Изменить баллы исполнителю")
    public ResponseEntity<UpdatePointsResponseDTO> updateParticipationRecordPoints(
            @PathVariable Long participation_record_id,
            @RequestParam Integer points,
            @RequestParam(required = false) String reason) {

        log.info("PUT /api/events/points/role/{} - Updating points to: {}", participation_record_id, points);
        UpdatePointsResponseDTO response = participationMarkService.updateParticipationRecordPoints(
                participation_record_id, points, reason);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/bulk/participants")
    @Operation(summary = "Массовое обновление баллов для участников мероприятия")
    public ResponseEntity<UpdatePointsResponseDTO.BulkUpdateResponse> bulkUpdateParticipantPoints(
            @Valid @RequestBody BulkUpdatePointsRequestDTO request) {

        log.info("PUT /api/events/points/bulk/participants - Bulk updating participant points for event: {}",
                request.getEventId());
        UpdatePointsResponseDTO.BulkUpdateResponse response =
                participationMarkService.bulkUpdateParticipantPoints(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/bulk/organizers")
    @Operation(summary = "Массовое обновление баллов для организаторов мероприятия")
    public ResponseEntity<UpdatePointsResponseDTO.BulkUpdateResponse> bulkUpdateOrganizerPoints(
            @Valid @RequestBody BulkUpdatePointsRequestDTO request) {

        log.info("PUT /api/events/points/bulk/organizers - Bulk updating organizer points for event: {}",
                request.getEventId());
        UpdatePointsResponseDTO.BulkUpdateResponse response =
                participationMarkService.bulkUpdateOrganizerPoints(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/bulk/roles")
    @Operation(summary = "Массовое обновление баллов для ролей мероприятия")
    public ResponseEntity<UpdatePointsResponseDTO.BulkUpdateResponse> bulkUpdateEventRolePoints(
            @Valid @RequestBody BulkUpdatePointsRequestDTO request) {

        log.info("PUT /api/events/points/bulk/roles - Bulk updating role points for event: {}",
                request.getEventId());
        UpdatePointsResponseDTO.BulkUpdateResponse response =
                participationMarkService.bulkUpdateEventRolePoints(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset/participants/{eventId}")
    @Operation(summary = "Сбросить баллы всех участников мероприятия к значению по умолчанию")
    public ResponseEntity<UpdatePointsResponseDTO.BulkUpdateResponse> resetAllParticipantPoints(
            @PathVariable Long eventId) {

        log.info("POST /api/events/points/reset/participants/{} - Resetting all participant points", eventId);
        UpdatePointsResponseDTO.BulkUpdateResponse response =
                participationMarkService.resetAllParticipantPoints(eventId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset/organizers/{eventId}")
    @Operation(summary = "Сбросить баллы всех организаторов мероприятия к значению по умолчанию")
    public ResponseEntity<UpdatePointsResponseDTO.BulkUpdateResponse> resetAllOrganizerPoints(
            @PathVariable Long eventId) {

        log.info("POST /api/events/points/reset/organizers/{} - Resetting all organizer points", eventId);
        UpdatePointsResponseDTO.BulkUpdateResponse response =
                participationMarkService.resetAllOrganizerPoints(eventId);
        return ResponseEntity.ok(response);
    }
}
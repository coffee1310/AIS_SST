package org.example.ais_sst.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.controller.base.BaseController;
import org.example.ais_sst.dto.event_participation.*;
import org.example.ais_sst.service.eventService.EventParticipantService;
import org.example.ais_sst.service.eventService.EventParticipantsFilterService;
import org.example.ais_sst.service.eventService.ParticipationMarkService;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.ais_sst.dto.event_participation.EventParticipantFilterDTO;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/events/participation")
@RequiredArgsConstructor
@Tag(name = "Event Participation", description = "Управление отметками участников мероприятий")
public class ParticipationMarkController extends BaseController {

    private final ParticipationMarkService participationMarkService;
    private final EventParticipantService eventParticipantService;
    private final EventParticipantsFilterService filterService;

    @PostMapping("/mark")
    @Operation(summary = "Отметить участников, организаторов и записи об участии")
    public ResponseEntity<ParticipationMarkResponseDTO> markParticipation(
            @Valid @RequestBody ParticipationMarkRequestDTO request) {

        log.info("POST /api/events/participation/mark - Marking participation for event: {}, records: {}",
                request.getEventId(),
                request.getParticipationRecordIds() != null ? request.getParticipationRecordIds().size() : 0);

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

    @GetMapping("/filter")
    @Operation(summary = "Получить участников мероприятия с фильтрацией (GET)")
    public ResponseEntity<Page<EventParticipantInfoDTO>> getEventParticipants(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) Integer minPoints,
            @RequestParam(required = false) Integer maxPoints,
            @RequestParam(required = false) Boolean wasPresent,
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        log.info("GET /api/events/participation/filter - Filtering participants with params");

        // Если eventId не передан, возвращаем пустую страницу
        if (eventId == null) {
            log.warn("eventId is null, returning empty page");
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
            return ResponseEntity.ok(new PageImpl<>(Collections.emptyList(), pageable, 0));
        }

        EventParticipantFilterDTO filter = EventParticipantFilterDTO.builder()
                .eventId(eventId)
                .role(role)
                .fullName(fullName)
                .minPoints(minPoints)
                .maxPoints(maxPoints)
                .wasPresent(wasPresent)
                .entityType(entityType)
                .build();

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        try {
            Page<EventParticipantInfoDTO> result = filterService.getEventParticipantsWithFilters(filter, pageable);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error filtering participants: {}", e.getMessage());
            // В случае ошибки возвращаем пустую страницу
            return ResponseEntity.ok(new PageImpl<>(Collections.emptyList(), pageable, 0));
        }
    }

    @PostMapping("/filter")
    @Operation(summary = "Получить участников мероприятия с фильтрацией (POST)")
    public ResponseEntity<Page<EventParticipantInfoDTO>> getEventParticipantsWithFilters(
            @RequestBody(required = false) EventParticipantFilterDTO filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        log.info("POST /api/events/participation/filter - Filtering participants with body");

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Если filter не передан или eventId не указан, возвращаем пустую страницу
        if (filter == null || filter.getEventId() == null) {
            log.warn("Filter is null or eventId is null, returning empty page");
            return ResponseEntity.ok(new PageImpl<>(Collections.emptyList(), pageable, 0));
        }

        try {
            Page<EventParticipantInfoDTO> result = filterService.getEventParticipantsWithFilters(filter, pageable);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error filtering participants: {}", e.getMessage());
            // В случае ошибки возвращаем пустую страницу
            return ResponseEntity.ok(new PageImpl<>(Collections.emptyList(), pageable, 0));
        }
    }
}
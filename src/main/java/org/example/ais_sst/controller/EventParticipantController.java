package org.example.ais_sst.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.controller.base.BaseController;
import org.example.ais_sst.dto.event_participant.EventParticipantResponseDTO;
import org.example.ais_sst.dto.event_participation.CreateParticipantRequest;
import org.example.ais_sst.dto.event_participation.CreateParticipationRecordRequest;
import org.example.ais_sst.dto.event_participation.CreateParticipationRecordResponse;
import org.example.ais_sst.entity.CustomUserDetails;
import org.example.ais_sst.entity.EventParticipant;
import org.example.ais_sst.service.eventService.EventParticipantService;
import org.example.ais_sst.service.eventService.EventParticipationRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/events/participants")
@RequiredArgsConstructor
public class EventParticipantController extends BaseController {

    private final EventParticipantService eventParticipantService;
    private final EventParticipationRecordService participationRecordService;

    @PostMapping("/{eventId}/join")
    @Operation(summary = "Стать участником мероприятия")
    public ResponseEntity<?> joinEvent(
            @PathVariable Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        EventParticipantResponseDTO response = eventParticipantService.becomeEventParticipant(eventId, userDetails.getId());
        return createSuccessResponse("Вы успешно зарегистрировались на мероприятие", response);
    }

    @DeleteMapping("/{eventId}/leave")
    @Operation(summary = "Отменить участие в мероприятии")
    public ResponseEntity<?> leaveEvent(
            @PathVariable Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        eventParticipantService.cancelParticipation(eventId, userDetails.getId());
        return createSuccessResponse("Вы отменили участие в мероприятии");
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Получить всех участников мероприятия")
    public ResponseEntity<List<EventParticipantResponseDTO>> getEventParticipants(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventParticipantService.getEventParticipants(eventId));
    }

    @GetMapping("/my-events")
    @Operation(summary = "Получить мероприятия текущего пользователя")
    public ResponseEntity<List<EventParticipantResponseDTO>> getMyEvents(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(eventParticipantService.getUserEvents(userDetails.getId()));
    }

    @GetMapping("/{eventId}/check")
    @Operation(summary = "Проверить участие в мероприятии")
    public ResponseEntity<Boolean> isParticipant(
            @PathVariable Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(eventParticipantService.isParticipant(eventId, userDetails.getId()));
    }

    @GetMapping("/{eventId}/slots")
    @Operation(summary = "Информация о свободных местах")
    public ResponseEntity<Map<String, Object>> getAvailableSlots(@PathVariable Long eventId) {
        long participantsCount = eventParticipantService.getParticipantsCount(eventId);
        int availableSlots = eventParticipantService.getAvailableSlots(eventId);
        String info = eventParticipantService.getAvailableSlotsInfo(eventId);

        Map<String, Object> response = new HashMap<>();
        response.put("currentParticipants", participantsCount);
        response.put("availableSlots", availableSlots);
        response.put("info", info);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{participantId}/soft")
    @Operation(summary = "Мягкое удаление участника")
    public ResponseEntity<Void> softDeleteParticipant(@PathVariable Long participantId) {
        log.info("DELETE /api/events/participants/{}/soft - Soft deleting participant", participantId);
        eventParticipantService.softDeleteParticipant(participantId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{participantId}/restore")
    @Operation(summary = "Восстановление участника")
    public ResponseEntity<Void> restoreParticipant(@PathVariable Long participantId) {
        log.info("POST /api/events/participants/{}/restore - Restoring participant", participantId);
        eventParticipantService.restoreParticipant(participantId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/soft/bulk")
    @Operation(summary = "Массовое мягкое удаление участников")
    public ResponseEntity<Void> softDeleteParticipants(@RequestParam List<Long> ids) {
        log.info("DELETE /api/events/participants/soft/bulk - Soft deleting {} participants", ids.size());
        eventParticipantService.softDeleteParticipants(ids);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/manual/{eventId}/{userId}")
    @Operation(summary = "Ручное добавление участника (без заявки)")
    public ResponseEntity<EventParticipant> addParticipantManually(
            @PathVariable Long eventId,
            @PathVariable Long userId) {
        log.info("POST /api/events/participants/manual/{}/{} - Adding participant manually", eventId, userId);
        EventParticipant participant = eventParticipantService.addParticipantManually(eventId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(participant);
    }

    @PostMapping("/manual/bulk")
    @Operation(summary = "Массовое ручное добавление участников")
    public ResponseEntity<List<EventParticipant>> addParticipantsManually(
            @RequestParam Long eventId,
            @RequestParam List<Long> userIds) {
        log.info("POST /api/events/participants/manual/bulk - Adding {} participants to event {}", userIds.size(), eventId);
        List<EventParticipant> participants = eventParticipantService.addParticipantsManually(eventId, userIds);
        return ResponseEntity.status(HttpStatus.CREATED).body(participants);
    }

    @PostMapping("/participants")
    @Operation(summary = "Создать или восстановить участника мероприятия")
    public ResponseEntity<EventParticipantResponseDTO> createOrRestoreParticipant(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateParticipantRequest request) {

        log.info("POST /api/events/{}/participants - Creating or restoring participant", eventId);
        EventParticipantResponseDTO response = eventParticipantService.createOrRestoreParticipant(
                eventId, request.getUserId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/participants/bulk")
    @Operation(summary = "Массовое создание или восстановление участников")
    public ResponseEntity<List<EventParticipantResponseDTO>> createOrRestoreParticipants(
            @PathVariable Long eventId,
            @RequestBody List<Long> userIds) {

        log.info("POST /api/events/{}/participants/bulk - Creating or restoring {} participants",
                eventId, userIds.size());
        List<EventParticipantResponseDTO> responses = eventParticipantService.createOrRestoreParticipants(
                eventId, userIds);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/participants/{userId}")
    @Operation(summary = "Мягкое удаление участника")
    public ResponseEntity<Void> softDeleteParticipant(
            @PathVariable Long eventId,
            @PathVariable Long userId) {

        log.info("DELETE /api/events/{}/participants/{} - Soft deleting participant", eventId, userId);
        eventParticipantService.cancelParticipation(eventId, userId);
        return ResponseEntity.noContent().build();
    }

    // ============ ИСПОЛНИТЕЛИ (EventParticipationRecord) ============

    @PostMapping("/participation-records")
    @Operation(summary = "Создать или восстановить запись об участии (исполнителя)")
    public ResponseEntity<CreateParticipationRecordResponse> createOrRestoreParticipationRecord(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateParticipationRecordRequest request) {

        log.info("POST /api/events/{}/participation-records - Creating or restoring participation record", eventId);
        CreateParticipationRecordResponse response = participationRecordService.createOrRestoreParticipationRecord(
                eventId,
                request.getSectorParticipantId(),
                request.getEventRoleId(),
                request.getComment()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/participation-records/bulk")
    @Operation(summary = "Массовое создание или восстановление записей об участии")
    public ResponseEntity<List<CreateParticipationRecordResponse>> createOrRestoreParticipationRecords(
            @PathVariable Long eventId,
            @RequestParam Long eventRoleId,
            @RequestBody List<Long> sectorParticipantIds,
            @RequestParam(required = false) String comment) {

        log.info("POST /api/events/{}/participation-records/bulk - Creating or restoring {} records for role {}",
                eventId, sectorParticipantIds.size(), eventRoleId);
        List<CreateParticipationRecordResponse> responses = participationRecordService.createOrRestoreParticipationRecords(
                eventId, sectorParticipantIds, eventRoleId, comment);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/participation-records/{recordId}")
    @Operation(summary = "Мягкое удаление записи об участии")
    public ResponseEntity<Void> softDeleteParticipationRecord(
            @PathVariable Long recordId) {

        log.info("DELETE /api/events/participation-records/{} - Soft deleting participation record",
                 recordId);
        participationRecordService.softDeleteParticipationRecord(recordId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/participation-records/{recordId}/restore")
    @Operation(summary = "Восстановление записи об участии")
    public ResponseEntity<CreateParticipationRecordResponse> restoreParticipationRecord(
            @PathVariable Long recordId) {

        log.info("POST /api/events/participation-records/{}/restore - Restoring participation record",
                 recordId);
        CreateParticipationRecordResponse response = participationRecordService.restoreParticipationRecord(recordId);
        return ResponseEntity.ok(response);
    }
}
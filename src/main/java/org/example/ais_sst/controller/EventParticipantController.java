package org.example.ais_sst.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.controller.base.BaseController;
import org.example.ais_sst.dto.event_participant.EventParticipantResponseDTO;
import org.example.ais_sst.entity.CustomUserDetails;
import org.example.ais_sst.entity.EventParticipant;
import org.example.ais_sst.service.eventService.EventParticipantService;
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
}
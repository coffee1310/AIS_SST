package org.example.ais_sst.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.example.ais_sst.controller.base.BaseController;
import org.example.ais_sst.dto.event_participant.EventParticipantResponseDTO;
import org.example.ais_sst.entity.CustomUserDetails;
import org.example.ais_sst.service.eventService.EventParticipantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
}
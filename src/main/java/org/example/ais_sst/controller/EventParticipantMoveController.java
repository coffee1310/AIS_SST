package org.example.ais_sst.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.controller.base.BaseController;
import org.example.ais_sst.dto.event_participant.EventParticipationRecordResponseDTO;
import org.example.ais_sst.dto.event_participant.MoveParticipantDTO;
import org.example.ais_sst.dto.event_roles.RoleOccupancyInfo;
import org.example.ais_sst.entity.CustomUserDetails;
import org.example.ais_sst.entity.EventParticipationRecord;
import org.example.ais_sst.service.eventService.EventParticipantMoveService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/participants/move")
@RequiredArgsConstructor
@Tag(name = "Event Participants Move", description = "Перемещение участников между основным составом и резервом")
public class EventParticipantMoveController extends BaseController {

    private final EventParticipantMoveService moveService;

    @PostMapping
    @Operation(summary = "Переместить участника (указать isReserve)")
    public ResponseEntity<EventParticipationRecordResponseDTO> moveParticipant(
            @Valid @RequestBody MoveParticipantDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        logInfo("/api/participants/move", "Moving participant by user: {}, recordId: {}, toReserve: {}",
                userDetails != null ? userDetails.getId() : null,
                dto.getParticipationRecordId(),
                dto.getIsReserve());

        EventParticipationRecordResponseDTO record = moveService.moveParticipant(dto);
        return ResponseEntity.ok(record);
    }

    @PostMapping("/to-main")
    @Operation(summary = "Переместить участника в основной состав")
    public ResponseEntity<EventParticipationRecordResponseDTO> moveToMain(
            @Valid @RequestBody MoveParticipantDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        logInfo("/api/participants/move/to-main", "Moving participant to main by user: {}",
                userDetails != null ? userDetails.getId() : null);

        EventParticipationRecordResponseDTO record = moveService.moveToMain(dto);
        return ResponseEntity.ok(record);
    }

    @PostMapping("/to-reserve")
    @Operation(summary = "Переместить участника в резерв")
    public ResponseEntity<EventParticipationRecordResponseDTO> moveToReserve(
            @Valid @RequestBody MoveParticipantDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        logInfo("/api/participants/move/to-reserve", "Moving participant to reserve by user: {}",
                userDetails != null ? userDetails.getId() : null);

        EventParticipationRecordResponseDTO record = moveService.moveToReserve(dto);
        return ResponseEntity.ok(record);
    }
}
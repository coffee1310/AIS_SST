package org.example.ais_sst.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.events.EventCreateDTO;
import org.example.ais_sst.dto.events.EventResponseDTO;
import org.example.ais_sst.dto.events.EventUpdateDTO;
import org.example.ais_sst.entity.CustomUserDetails;
import org.example.ais_sst.service.eventService.EventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Управление мероприятиями")
public class EventController {

    private final EventService eventService;

    @PostMapping
    @Operation(summary = "Создать мероприятие")
    public ResponseEntity<EventResponseDTO> createEvent(
            @Valid @RequestBody EventCreateDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("POST /api/events - Creating event");
        EventResponseDTO response = eventService.createEvent(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить мероприятие по ID")
    public ResponseEntity<EventResponseDTO> getEventById(@PathVariable Long id) {
        log.info("GET /api/events/{} - Getting event by id", id);
        EventResponseDTO event = eventService.getEventById(id);
        return ResponseEntity.ok(event);
    }

    @GetMapping
    @Operation(summary = "Универсальный поиск мероприятий с фильтрами")
    public ResponseEntity<Page<EventResponseDTO>> getEvents(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String venue,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(required = false) Boolean isDraft,
            @RequestParam(required = false) Boolean isCompleted,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("GET /api/events - Getting events with filters");

        // Временно без сортировки
        Pageable pageable = PageRequest.of(page, size);

        Long filterCreatorId = creatorId != null ? creatorId : (userDetails != null ? userDetails.getId() : null);

        Page<EventResponseDTO> events = eventService.getEventsWithFilters(
                title, venue, dateFrom, dateTo, isPublic, isDraft, isCompleted, isActive, filterCreatorId, pageable);

        return ResponseEntity.ok(events);
    }

    @GetMapping("/creator/me")
    @Operation(summary = "Получить мероприятия, созданные текущим пользователем")
    public ResponseEntity<Page<EventResponseDTO>> getMyEvents(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info("GET /api/events/creator/me - Getting events created by user");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        Page<EventResponseDTO> events = eventService.getEventsByCreator(userDetails.getId(), pageable);
        return ResponseEntity.ok(events);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить мероприятие")
    public ResponseEntity<EventResponseDTO> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventUpdateDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("PUT /api/events/{} - Updating event", id);
        EventResponseDTO response = eventService.updateEvent(id, request, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/organizers/{organizerId}")
    @Operation(summary = "Добавить организатора")
    public ResponseEntity<EventResponseDTO> addOrganizer(
            @PathVariable Long id,
            @PathVariable Long organizerId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("POST /api/events/{}/organizers/{} - Adding organizer", id, organizerId);
        EventResponseDTO response = eventService.addOrganizer(id, organizerId, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/organizers/{organizerId}")
    @Operation(summary = "Удалить организатора")
    public ResponseEntity<EventResponseDTO> removeOrganizer(
            @PathVariable Long id,
            @PathVariable Long organizerId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("DELETE /api/events/{}/organizers/{} - Removing organizer", id, organizerId);
        EventResponseDTO response = eventService.removeOrganizer(id, organizerId, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить мероприятие")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("DELETE /api/events/{} - Deleting event", id);
        eventService.deleteEvent(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Завершить мероприятие")
    public ResponseEntity<EventResponseDTO> completeEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("POST /api/events/{}/complete - Completing event", id);
        EventResponseDTO response = eventService.completeEvent(id, userDetails.getId());
        return ResponseEntity.ok(response);
    }
}
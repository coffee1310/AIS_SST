package org.example.ais_sst.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.controller.base.BaseController;
import org.example.ais_sst.dto.task_request.CreateTaskRequestDTO;
import org.example.ais_sst.dto.task_request.ProcessTaskRequestDTO;
import org.example.ais_sst.dto.task_request.TaskRequestFilterDTO;
import org.example.ais_sst.dto.task_request.TaskRequestResponseDTO;
import org.example.ais_sst.entity.CustomUserDetails;
import org.example.ais_sst.entity.enums.TaskRequestStatus;

import org.example.ais_sst.service.taskService.TaskRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/task-requests")
@RequiredArgsConstructor
@Tag(name = "Task Requests", description = "Управление заявками на задачи")
public class TaskRequestController extends BaseController {

    private final TaskRequestService taskRequestService;

    @PostMapping
    @Operation(summary = "Подать заявку на задачу")
    public ResponseEntity<TaskRequestResponseDTO> createTaskRequest(
            @Valid @RequestBody CreateTaskRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("POST /api/task-requests - Creating task request for task: {}", request.getTaskId());
        TaskRequestResponseDTO response = taskRequestService.createTaskRequest(request, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{requestId}/approve")
    @Operation(summary = "Одобрить заявку (только для создателя задачи)")
    public ResponseEntity<TaskRequestResponseDTO> approveTaskRequest(
            @PathVariable Integer requestId,
            @Valid @RequestBody(required = false) ProcessTaskRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("POST /api/task-requests/{}/approve - Approving task request", requestId);

        ProcessTaskRequestDTO processRequest = request != null ? request : new ProcessTaskRequestDTO();
        processRequest.setRequestId(requestId);

        TaskRequestResponseDTO response = taskRequestService.approveTaskRequest(processRequest, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{requestId}/reject")
    @Operation(summary = "Отклонить заявку (только для создателя задачи)")
    public ResponseEntity<TaskRequestResponseDTO> rejectTaskRequest(
            @PathVariable Integer requestId,
            @Valid @RequestBody(required = false) ProcessTaskRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("POST /api/task-requests/{}/reject - Rejecting task request", requestId);

        ProcessTaskRequestDTO processRequest = request != null ? request : new ProcessTaskRequestDTO();
        processRequest.setRequestId(requestId);

        TaskRequestResponseDTO response = taskRequestService.rejectTaskRequest(processRequest, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{requestId}")
    @Operation(summary = "Отменить заявку (только для автора, если заявка на рассмотрении)")
    public ResponseEntity<Void> cancelTaskRequest(
            @PathVariable Integer requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("DELETE /api/task-requests/{} - Cancelling task request", requestId);
        taskRequestService.cancelTaskRequest(requestId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "Получить все заявки на задачу")
    public ResponseEntity<List<TaskRequestResponseDTO>> getRequestsByTaskId(@PathVariable Integer taskId) {
        log.info("GET /api/task-requests/task/{} - Getting requests for task", taskId);
        List<TaskRequestResponseDTO> response = taskRequestService.getRequestsByTaskId(taskId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/task/{taskId}/status/{status}")
    @Operation(summary = "Получить заявки на задачу по статусу")
    public ResponseEntity<List<TaskRequestResponseDTO>> getRequestsByTaskIdAndStatus(
            @PathVariable Integer taskId,
            @PathVariable String status) {

        log.info("GET /api/task-requests/task/{}/status/{} - Getting requests for task by status", taskId, status);
        TaskRequestStatus requestStatus = TaskRequestStatus.fromDisplayName(status);
        List<TaskRequestResponseDTO> response = taskRequestService.getRequestsByTaskIdAndStatus(taskId, requestStatus);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @Operation(summary = "Получить мои заявки")
    public ResponseEntity<List<TaskRequestResponseDTO>> getMyRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("GET /api/task-requests/my - Getting my requests");
        List<TaskRequestResponseDTO> response = taskRequestService.getRequestsByStudentId(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my/status/{status}")
    @Operation(summary = "Получить мои заявки по статусу")
    public ResponseEntity<List<TaskRequestResponseDTO>> getMyRequestsByStatus(
            @PathVariable String status,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("GET /api/task-requests/my/status/{} - Getting my requests by status", status);
        TaskRequestStatus requestStatus = TaskRequestStatus.fromDisplayName(status);
        List<TaskRequestResponseDTO> response = taskRequestService.getRequestsByStudentIdAndStatus(
                userDetails.getId(), requestStatus);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "Получить заявки студента")
    public ResponseEntity<List<TaskRequestResponseDTO>> getRequestsByStudentId(@PathVariable Long studentId) {
        log.info("GET /api/task-requests/student/{} - Getting requests for student", studentId);
        List<TaskRequestResponseDTO> response = taskRequestService.getRequestsByStudentId(studentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Получить заявки с фильтрацией")
    public ResponseEntity<Page<TaskRequestResponseDTO>> getTaskRequestsWithFilters(
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) Integer taskId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant filingDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant filingDateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant reviewedAtFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant reviewedAtTo,
            @RequestParam(required = false) String taskTitle,
            @RequestParam(required = false) String taskDescription,
            @RequestParam(required = false) Integer taskMaxPeopleCount,
            @RequestParam(required = false) Integer taskCountOfPoints,
            @RequestParam(required = false) Boolean taskIsCompleted,
            @RequestParam(required = false) Boolean taskIsPreassigned,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) String studentSurname,
            @RequestParam(required = false) String studentEmail,
            @RequestParam(required = false) Boolean myTasks,
            @RequestParam(required = false) Boolean myRequests,
            @RequestParam(required = false) Boolean pendingOnly,
            @RequestParam(required = false) Boolean reviewedOnly,
            @RequestParam(required = false) Boolean approvedOnly,
            @RequestParam(required = false) Boolean rejectedOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "filingDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        logInfo("/api/task-requests", "Getting task requests with filters");

        // Преобразуем статус из строки в enum
        TaskRequestStatus requestStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                requestStatus = TaskRequestStatus.fromDisplayName(status);
            } catch (IllegalArgumentException e) {
                // Если статус не распознан, оставляем null
                log.warn("Unknown status: {}", status);
            }
        }

        TaskRequestFilterDTO filter = TaskRequestFilterDTO.builder()
                .id(id)
                .taskId(taskId)
                .studentId(studentId)
                .currentUserId(userDetails != null ? userDetails.getId() : null)
                .status(requestStatus)
                .filingDateFrom(filingDateFrom)
                .filingDateTo(filingDateTo)
                .reviewedAtFrom(reviewedAtFrom)
                .reviewedAtTo(reviewedAtTo)
                .taskTitle(taskTitle)
                .taskDescription(taskDescription)
                .taskMaxPeopleCount(taskMaxPeopleCount)
                .taskCountOfPoints(taskCountOfPoints)
                .taskIsCompleted(taskIsCompleted)
                .taskIsPreassigned(taskIsPreassigned)
                .studentName(studentName)
                .studentSurname(studentSurname)
                .studentEmail(studentEmail)
                .myTasks(myTasks)
                .myRequests(myRequests)
                .pendingOnly(pendingOnly)
                .reviewedOnly(reviewedOnly)
                .approvedOnly(approvedOnly)
                .rejectedOnly(rejectedOnly)
                .build();

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TaskRequestResponseDTO> requests = taskRequestService.getTaskRequestsWithFilters(filter, pageable);
        return ResponseEntity.ok(requests);
    }
}
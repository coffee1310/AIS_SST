package org.example.ais_sst.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.controller.base.BaseController;
import org.example.ais_sst.dto.tasks.*;
import org.example.ais_sst.entity.CustomUserDetails;
import org.example.ais_sst.service.taskService.TaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Управление задачами")
public class TaskController extends BaseController {

    private final TaskService taskService;

    /**
     * Получить ID текущего пользователя из SecurityContext
     */
    @PostMapping
    @Operation(summary = "Создать новую задачу (creator_id подставляется автоматически из токена)")
    public ResponseEntity<TaskResponseDTO> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("POST /api/tasks - Creating new task: title={}", request.getTitle());
        TaskResponseDTO response = taskService.createTask(request, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Получить задачи с фильтрацией")
    public ResponseEntity<Page<TaskResponseDTO>> getTasks(
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long creatorId,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant deadlineFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant deadlineTo,

            @RequestParam(required = false) Integer maxPeopleCount,
            @RequestParam(required = false) Integer countOfPoints,
            @RequestParam(required = false) Boolean isCompleted,
            @RequestParam(required = false) Boolean isDeleted,
            @RequestParam(required = false) Boolean isPreassigned,

            // Новые фильтры
            @RequestParam(required = false) Boolean createdByMe,    // Задачи созданные мной
            @RequestParam(required = false) Boolean assignedToMe,   // Задачи назначенные мне

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,

            @AuthenticationPrincipal CustomUserDetails userDetails) {

        logInfo("/api/tasks", "Getting tasks with filters");

        TaskFilterDTO filter = TaskFilterDTO.builder()
                .id(id)
                .title(title)
                .description(description)
                .creatorId(creatorId)
                .currentUserId(userDetails != null ? userDetails.getId() : null)
                .deadlineFrom(deadlineFrom)
                .deadlineTo(deadlineTo)
                .maxPeopleCount(maxPeopleCount)
                .countOfPoints(countOfPoints)
                .isCompleted(isCompleted)
                .isDeleted(isDeleted)
                .isPreassigned(isPreassigned)
                .createdByMe(createdByMe)
                .assignedToMe(assignedToMe)
                .build();

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TaskResponseDTO> tasks = taskService.getTasksWithFilters(filter, pageable);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Получить задачу по ID")
    public ResponseEntity<TaskResponseDTO> getTaskById(@PathVariable Long taskId) {
        log.info("GET /api/tasks/{} - Getting task", taskId);
        TaskResponseDTO response = taskService.getTaskById(taskId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/creator/{userId}")
    @Operation(summary = "Получить задачи созданные пользователем")
    public ResponseEntity<List<TaskResponseDTO>> getTasksByCreator(@PathVariable Long userId) {
        log.info("GET /api/tasks/creator/{} - Getting tasks created by user", userId);
        List<TaskResponseDTO> response = taskService.getTasksByCreator(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Получить задачи назначенные пользователю")
    public ResponseEntity<List<TaskResponseDTO>> getTasksByUser(@PathVariable Long userId) {
        log.info("GET /api/tasks/user/{} - Getting tasks assigned to user", userId);
        List<TaskResponseDTO> response = taskService.getTasksByUser(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}/active")
    @Operation(summary = "Получить активные задачи пользователя")
    public ResponseEntity<List<TaskResponseDTO>> getActiveTasksByUser(@PathVariable Long userId) {
        log.info("GET /api/tasks/user/{}/active - Getting active tasks for user", userId);
        List<TaskResponseDTO> response = taskService.getActiveTasksByUser(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/expired")
    @Operation(summary = "Получить задачи с истекшим дедлайном")
    public ResponseEntity<List<TaskResponseDTO>> getTasksWithExpiredDeadline() {
        log.info("GET /api/tasks/expired - Getting tasks with expired deadline");
        List<TaskResponseDTO> response = taskService.getTasksWithExpiredDeadline();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "Мягкое удаление задачи (только для создателя)")
    public ResponseEntity<Void> softDeleteTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("DELETE /api/tasks/{} - Soft deleting task", taskId);
        taskService.softDeleteTask(taskId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{taskId}/completion/executor")
    @Operation(summary = "Исполнитель отмечает выполнение задачи для себя")
    public ResponseEntity<TaskCompletionResponse> markTaskCompletedByExecutor(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskCompletionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("PUT /api/tasks/{}/completion/executor - Executor marking task completion", taskId);
        TaskCompletionResponse response = taskService.markTaskCompletedByExecutor(
                taskId, request, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{taskId}/completion/creator")
    @Operation(summary = "Создатель отмечает задачу как выполненную")
    public ResponseEntity<TaskCompletionResponse> markTaskCompletedByCreator(
            @PathVariable Long taskId,
            @RequestParam Boolean isCompleted,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("PUT /api/tasks/{}/completion/creator - Creator marking task as completed: {}", taskId, isCompleted);
        TaskCompletionResponse response = taskService.markTaskCompletedByCreator(
                taskId, isCompleted, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{taskId}/completion/executor/{userId}")
    @Operation(summary = "Получить статус выполнения задачи для исполнителя")
    public ResponseEntity<TaskCompletionResponse> getTaskCompletionStatus(
            @PathVariable Long taskId,
            @PathVariable Long userId) {

        log.info("GET /api/tasks/{}/completion/executor/{} - Getting task completion status for executor", taskId, userId);
        TaskCompletionResponse response = taskService.getTaskCompletionStatus(taskId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{taskId}/completion/executor/all")
    @Operation(summary = "Получить статусы выполнения всех исполнителей задачи")
    public ResponseEntity<List<TaskCompletionResponse>> getAllTaskCompletionStatuses(
            @PathVariable Long taskId) {

        log.info("GET /api/tasks/{}/completion/executor/all - Getting all task completion statuses", taskId);
        List<TaskCompletionResponse> response = taskService.getAllTaskCompletionStatuses(taskId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{taskId}/completion/creator")
    @Operation(summary = "Получить статус задачи (только для создателя)")
    public ResponseEntity<TaskCompletionResponse> getTaskStatus(
            @PathVariable Long taskId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("GET /api/tasks/{}/completion/creator - Getting task status", taskId);
        TaskCompletionResponse response = taskService.getTaskStatus(taskId, userDetails.getId());
        return ResponseEntity.ok(response);
    }
}
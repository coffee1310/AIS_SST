package org.example.ais_sst.service.taskService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.tasks.*;
import org.example.ais_sst.dto.task_request.*;
import org.example.ais_sst.entity.Task;
import org.example.ais_sst.entity.TaskRequest;
import org.example.ais_sst.entity.TaskUser;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.entity.enums.TaskRequestStatus;
import org.example.ais_sst.exception.UserDoesNotExistException;
import org.example.ais_sst.exception.ValidationException;
import org.example.ais_sst.mapper.TaskRequestMapper;
import org.example.ais_sst.repository.TaskRepository;
import org.example.ais_sst.repository.TaskRequestRepository;
import org.example.ais_sst.repository.TaskUserRepository;
import org.example.ais_sst.repository.UserRepository;
import org.example.ais_sst.specification.TaskRequestSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskRequestService {

    private final TaskRequestRepository taskRequestRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskUserRepository taskUserRepository;
    private final TaskRequestMapper taskRequestMapper;

    @Transactional
    public TaskRequestResponseDTO createTaskRequest(CreateTaskRequestDTO request, Long studentId) {
        log.info("Creating task request: taskId={}, studentId={}", request.getTaskId(), studentId);

        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ValidationException("Задача не найдена: " + request.getTaskId()));

        if (Boolean.TRUE.equals(task.getIsDeleted())) {
            throw new ValidationException("Задача удалена");
        }

        if (Boolean.TRUE.equals(task.getIsCompleted())) {
            throw new ValidationException("Задача уже выполнена");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден: " + studentId));

        if (task.getCreator().getId().equals(studentId)) {
            throw new ValidationException("Создатель задачи не может подать заявку на свою задачу");
        }

        if (taskUserRepository.existsByTaskIdAndUserIdAndIsDeletedFalse(task.getId(), studentId)) {
            throw new ValidationException("Вы уже назначены на эту задачу");
        }

        if (taskRequestRepository.existsByTaskIdAndStudentIdAndStatusAndIsDeletedFalse(
                task.getId(), studentId, TaskRequestStatus.НА_РАССМОТРЕНИИ)) {
            throw new ValidationException("Вы уже подали заявку на эту задачу");
        }

        if (task.getMaxPeopleCount() > 0) {
            long currentAssigned = taskUserRepository.countByTaskIdAndIsDeletedFalse(task.getId());
            long pendingRequests = taskRequestRepository.countByTaskIdAndStatus(task.getId(), TaskRequestStatus.НА_РАССМОТРЕНИИ);

            if (currentAssigned + pendingRequests >= task.getMaxPeopleCount()) {
                throw new ValidationException(
                        String.format("Достигнут лимит участников для задачи (%d). Текущих участников: %d, заявок на рассмотрении: %d",
                                task.getMaxPeopleCount(), currentAssigned, pendingRequests)
                );
            }
        }

        TaskRequest taskRequest = TaskRequest.builder()
                .task(task)
                .student(student)
                .status(TaskRequestStatus.НА_РАССМОТРЕНИИ)
                .filingDate(Instant.now())
                .isDeleted(false)
                .build();

        taskRequest = taskRequestRepository.save(taskRequest);
        log.info("Task request created with id: {}", taskRequest.getId());

        return taskRequestMapper.toResponseDto(taskRequest);
    }

    @Transactional
    public TaskRequestResponseDTO approveTaskRequest(ProcessTaskRequestDTO request, Long reviewerId) {
        log.info("Approving task request: requestId={}, reviewerId={}", request.getRequestId(), reviewerId);

        TaskRequest taskRequest = taskRequestRepository.findById(request.getRequestId())
                .orElseThrow(() -> new ValidationException("Заявка не найдена: " + request.getRequestId()));

        if (Boolean.TRUE.equals(taskRequest.getIsDeleted())) {
            throw new ValidationException("Заявка удалена");
        }

        if (taskRequest.getStatus() != TaskRequestStatus.НА_РАССМОТРЕНИИ) {
            throw new ValidationException("Заявка уже обработана. Статус: " + taskRequest.getStatus().getDisplayName());
        }

        Task task = taskRequest.getTask();

        if (!task.getCreator().getId().equals(reviewerId)) {
            throw new ValidationException("Только создатель задачи может одобрить заявку");
        }

        if (Boolean.TRUE.equals(task.getIsDeleted())) {
            throw new ValidationException("Задача удалена");
        }

        if (Boolean.TRUE.equals(task.getIsCompleted())) {
            throw new ValidationException("Задача уже выполнена");
        }

        if (task.getMaxPeopleCount() > 0) {
            long currentAssigned = taskUserRepository.countByTaskIdAndIsDeletedFalse(task.getId());

            if (currentAssigned >= task.getMaxPeopleCount()) {
                throw new ValidationException(
                        String.format("Достигнут лимит участников для задачи (%d). Текущих участников: %d",
                                task.getMaxPeopleCount(), currentAssigned)
                );
            }
        }

        if (taskUserRepository.existsByTaskIdAndUserIdAndIsDeletedFalse(task.getId(), taskRequest.getStudent().getId())) {
            taskRequest.setStatus(TaskRequestStatus.ОТКЛОНЕНО);
            taskRequest.setReviewedAt(Instant.now());
            taskRequestRepository.save(taskRequest);
            throw new ValidationException("Пользователь уже назначен на эту задачу");
        }

        taskRequest.setStatus(TaskRequestStatus.ПРИНЯТО);
        taskRequest.setReviewedAt(Instant.now());
        taskRequest = taskRequestRepository.save(taskRequest);
        log.info("Task request {} approved", request.getRequestId());

        TaskUser taskUser = TaskUser.builder()
                .task(task)
                .user(taskRequest.getStudent())
                .isAssigned(true)
                .isCompleted(false)
                .assignedAt(Instant.now())
                .build();

        taskUserRepository.save(taskUser);
        log.info("User {} assigned to task {}", taskRequest.getStudent().getId(), task.getId());

        return taskRequestMapper.toResponseDto(taskRequest);
    }

    @Transactional
    public TaskRequestResponseDTO rejectTaskRequest(ProcessTaskRequestDTO request, Long reviewerId) {
        log.info("Rejecting task request: requestId={}, reviewerId={}", request.getRequestId(), reviewerId);

        TaskRequest taskRequest = taskRequestRepository.findById(request.getRequestId())
                .orElseThrow(() -> new ValidationException("Заявка не найдена: " + request.getRequestId()));

        if (Boolean.TRUE.equals(taskRequest.getIsDeleted())) {
            throw new ValidationException("Заявка удалена");
        }

        if (taskRequest.getStatus() != TaskRequestStatus.НА_РАССМОТРЕНИИ) {
            throw new ValidationException("Заявка уже обработана. Статус: " + taskRequest.getStatus().getDisplayName());
        }

        Task task = taskRequest.getTask();

        if (!task.getCreator().getId().equals(reviewerId)) {
            throw new ValidationException("Только создатель задачи может отклонить заявку");
        }

        taskRequest.setStatus(TaskRequestStatus.ОТКЛОНЕНО);
        taskRequest.setReviewedAt(Instant.now());
        taskRequest = taskRequestRepository.save(taskRequest);
        log.info("Task request {} rejected", request.getRequestId());

        return taskRequestMapper.toResponseDto(taskRequest);
    }

    @Transactional(readOnly = true)
    public Page<TaskRequestResponseDTO> getTaskRequestsWithFilters(TaskRequestFilterDTO filter, Pageable pageable) {
        Specification<TaskRequest> spec = TaskRequestSpecification.withFilter(filter);
        return taskRequestRepository.findAll(spec, pageable)
                .map(taskRequestMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public List<TaskRequestResponseDTO> getRequestsByTaskId(Integer taskId) {
        taskRepository.findById(taskId)
                .orElseThrow(() -> new ValidationException("Задача не найдена: " + taskId));

        return taskRequestRepository.findByTaskIdAndIsDeletedFalse(taskId).stream()
                .map(taskRequestMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskRequestResponseDTO> getRequestsByStudentId(Long studentId) {
        userRepository.findById(studentId)
                .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден: " + studentId));

        return taskRequestRepository.findByStudentIdAndIsDeletedFalse(studentId).stream()
                .map(taskRequestMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskRequestResponseDTO> getRequestsByStudentIdAndStatus(Long studentId, TaskRequestStatus status) {
        userRepository.findById(studentId)
                .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден: " + studentId));

        return taskRequestRepository.findByStudentIdAndStatus(studentId, status).stream()
                .map(taskRequestMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskRequestResponseDTO> getRequestsByTaskIdAndStatus(Integer taskId, TaskRequestStatus status) {
        taskRepository.findById(taskId)
                .orElseThrow(() -> new ValidationException("Задача не найдена: " + taskId));

        return taskRequestRepository.findByTaskIdAndStatus(taskId, status).stream()
                .map(taskRequestMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelTaskRequest(Integer requestId, Long studentId) {
        TaskRequest taskRequest = taskRequestRepository.findById(requestId)
                .orElseThrow(() -> new ValidationException("Заявка не найдена: " + requestId));

        if (Boolean.TRUE.equals(taskRequest.getIsDeleted())) {
            throw new ValidationException("Заявка удалена");
        }

        if (!taskRequest.getStudent().getId().equals(studentId)) {
            throw new ValidationException("Только автор заявки может ее отменить");
        }

        if (taskRequest.getStatus() != TaskRequestStatus.НА_РАССМОТРЕНИИ) {
            throw new ValidationException("Нельзя отменить обработанную заявку");
        }

        taskRequest.setIsDeleted(true);
        taskRequestRepository.save(taskRequest);
        log.info("Task request {} cancelled by user {}", requestId, studentId);
    }
}
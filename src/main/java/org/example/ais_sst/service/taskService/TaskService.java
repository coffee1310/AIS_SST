package org.example.ais_sst.service.taskService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.tasks.*;
import org.example.ais_sst.entity.Task;
import org.example.ais_sst.entity.TaskUser;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.event.tasks.TaskCompletedEvent;
import org.example.ais_sst.event.tasks.TaskCreatedEvent;
import org.example.ais_sst.exception.UserDoesNotExistException;
import org.example.ais_sst.exception.ValidationException;
import org.example.ais_sst.mapper.TaskMapper;
import org.example.ais_sst.repository.TaskRepository;
import org.example.ais_sst.repository.TaskUserRepository;
import org.example.ais_sst.repository.UserRepository;
import org.example.ais_sst.specification.TaskSpecification;
import org.springframework.context.ApplicationEventPublisher;
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
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskUserRepository taskUserRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    private final ApplicationEventPublisher eventPublisher;   // ← Внедряем публикатор

    /**
     * Создание новой задачи
     */
    @Transactional
    public TaskResponseDTO createTask(CreateTaskRequest request, Long creatorId) {
        log.info("Creating new task: title={}, isPreassigned={}, creatorId={}",
                request.getTitle(), request.getIsPreassigned(), creatorId);

        // Получаем создателя
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new UserDoesNotExistException("Создатель не найден: " + creatorId));

        // Если isPreassigned = true, проверяем, что указаны пользователи
        if (Boolean.TRUE.equals(request.getIsPreassigned())) {
            if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
                throw new ValidationException("При isPreassigned=true необходимо указать список пользователей");
            }
        }

        // Проверяем максимальное количество людей
        if (request.getMaxPeopleCount() > 0 && request.getUserIds() != null) {
            if (request.getUserIds().size() > request.getMaxPeopleCount()) {
                throw new ValidationException(
                        String.format("Количество пользователей (%d) превышает максимальное (%d)",
                                request.getUserIds().size(), request.getMaxPeopleCount())
                );
            }
        }

        // Создаем задачу с creator
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .deadline(request.getDeadline())
                .maxPeopleCount(request.getMaxPeopleCount())
                .countOfPoints(request.getCountOfPoints())
                .isPreassigned(request.getIsPreassigned())
                .isCompleted(false)
                .isDeleted(false)
                .creator(creator)
                .build();

        task = taskRepository.save(task);
        log.info("Task created with id: {} by user: {}", task.getId(), creatorId);

        // Если isPreassigned = true, добавляем пользователей
        if (Boolean.TRUE.equals(request.getIsPreassigned()) && request.getUserIds() != null) {
            addUsersToTask(task, request.getUserIds());
        }

        return taskMapper.toResponseDto(task);
    }

    /**
     * Добавление пользователей к задаче
     */
    @Transactional
    public void addUsersToTask(Task task, List<Long> userIds) {
        for (Long userId : userIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден: " + userId));

            // Проверяем, не существует ли уже такой связи
            if (taskUserRepository.findByTaskIdAndUserId(task.getId(), userId).isPresent()) {
                log.warn("User {} already assigned to task {}", userId, task.getId());
                continue;
            }

            // Проверяем лимит пользователей
            if (task.getMaxPeopleCount() > 0) {
                long currentCount = taskMapper.mapAssignedUsersCount(task);
                if (currentCount >= task.getMaxPeopleCount()) {
                    throw new ValidationException(
                            String.format("Достигнут лимит пользователей для задачи (%d)", task.getMaxPeopleCount())
                    );
                }
            }

            TaskUser taskUser = TaskUser.builder()
                    .task(task)
                    .user(user)
                    .isAssigned(false)
                    .isCompleted(false)
                    .build();

            taskUserRepository.save(taskUser);
            log.info("User {} assigned to task {}", userId, task.getId());
        }
    }

    /**
     * Получить задачу по ID
     */
    @Transactional(readOnly = true)
    public TaskResponseDTO getTaskById(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ValidationException("Задача не найдена: " + taskId));

        if (Boolean.TRUE.equals(task.getIsDeleted())) {
            throw new ValidationException("Задача удалена: " + taskId);
        }

        return taskMapper.toResponseDto(task);
    }

    /**
     * Получить задачи с фильтрацией и пагинацией
     */
    @Transactional(readOnly = true)
    public Page<TaskResponseDTO> getTasksWithFilters(TaskFilterDTO filter, Pageable pageable) {
        Specification<Task> spec = TaskSpecification.withFilter(filter);
        return taskRepository.findAll(spec, pageable)
                .map(taskMapper::toResponseDto);
    }

    /**
     * Получить все активные задачи
     */
    @Transactional(readOnly = true)
    public List<TaskResponseDTO> getAllActiveTasks() {
        return taskRepository.findByIsDeletedFalse().stream()
                .map(taskMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Получить задачи созданные пользователем
     */
    @Transactional(readOnly = true)
    public List<TaskResponseDTO> getTasksByCreator(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден: " + userId));

        return taskRepository.findByCreatorIdAndIsDeletedFalse(userId).stream()
                .map(taskMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Получить задачи пользователя (назначенные ему)
     */
    @Transactional(readOnly = true)
    public List<TaskResponseDTO> getTasksByUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден: " + userId));

        return taskRepository.findTasksByUserId(userId).stream()
                .map(taskMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Получить активные задачи пользователя
     */
    @Transactional(readOnly = true)
    public List<TaskResponseDTO> getActiveTasksByUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден: " + userId));

        return taskRepository.findActiveTasksByUserId(userId).stream()
                .map(taskMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Получить задачи с истекшим дедлайном
     */
    @Transactional(readOnly = true)
    public List<TaskResponseDTO> getTasksWithExpiredDeadline() {
        return taskRepository.findByIsDeletedFalseAndDeadlineBefore(Instant.now()).stream()
                .map(taskMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Мягкое удаление задачи (только для создателя)
     */
    @Transactional
    public void softDeleteTask(Long taskId, Long currentUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ValidationException("Задача не найдена: " + taskId));

        // Проверяем, что пользователь является создателем
        if (!task.getCreator().getId().equals(currentUserId)) {
            throw new ValidationException("Только создатель может удалить задачу");
        }

        task.setIsDeleted(true);
        taskRepository.save(task);
        log.info("Task {} soft deleted by user {}", taskId, currentUserId);
    }

    /**
     * Обновление статуса выполнения задачи
     */
    @Transactional
    public TaskResponseDTO completeTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ValidationException("Задача не найдена: " + taskId));

        if (Boolean.TRUE.equals(task.getIsDeleted())) {
            throw new ValidationException("Задача удалена: " + taskId);
        }

        task.setIsCompleted(true);
        task = taskRepository.save(task);
        log.info("Task {} completed", taskId);



        return taskMapper.toResponseDto(task);
    }

    /**
     * Обновление задачи (только для создателя)
     */
    @Transactional
    public TaskResponseDTO updateTask(Long taskId, CreateTaskRequest request, Long currentUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ValidationException("Задача не найдена: " + taskId));

        if (Boolean.TRUE.equals(task.getIsDeleted())) {
            throw new ValidationException("Задача удалена: " + taskId);
        }

        // Проверяем, что пользователь является создателем
        if (!task.getCreator().getId().equals(currentUserId)) {
            throw new ValidationException("Только создатель может обновить задачу");
        }

        // Обновляем поля
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getDeadline() != null) {
            task.setDeadline(request.getDeadline());
        }
        if (request.getMaxPeopleCount() != null) {
            task.setMaxPeopleCount(request.getMaxPeopleCount());
        }
        if (request.getCountOfPoints() != null) {
            task.setCountOfPoints(request.getCountOfPoints());
        }
        if (request.getIsPreassigned() != null) {
            task.setIsPreassigned(request.getIsPreassigned());
        }

        // Если isPreassigned = true и передан список пользователей, обновляем назначения
        if (Boolean.TRUE.equals(task.getIsPreassigned()) && request.getUserIds() != null) {
            // Удаляем старых пользователей
            taskUserRepository.softDeleteByTaskId(taskId);

            // Добавляем новых
            addUsersToTask(task, request.getUserIds());
        }

        task = taskRepository.save(task);
        log.info("Task {} updated by user {}", taskId, currentUserId);

        return taskMapper.toResponseDto(task);
    }

    /**
     * Обновить статус выполнения задачи для текущего пользователя (исполнитель для себя)
     */
    @Transactional
    public TaskCompletionResponse updateTaskCompletionByExecutor(Long taskId, UpdateTaskCompletionRequest request, Long currentUserId) {
        log.info("Updating task completion by executor: taskId={}, isCompleted={}, currentUserId={}",
                taskId, request.getIsCompleted(), currentUserId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ValidationException("Задача не найдена: " + taskId));

        if (Boolean.TRUE.equals(task.getIsDeleted())) {
            throw new ValidationException("Задача удалена");
        }

        // Проверяем, что пользователь назначен на задачу
        TaskUser taskUser = taskUserRepository.findActiveByTaskIdAndUserId(taskId, currentUserId)
                .orElseThrow(() -> new ValidationException(
                        String.format("Пользователь %d не назначен на задачу %d", currentUserId, taskId)));

        // Проверяем, что задача не выполнена полностью
        if (Boolean.TRUE.equals(task.getIsCompleted()) && Boolean.TRUE.equals(request.getIsCompleted())) {
            throw new ValidationException("Задача уже выполнена полностью");
        }

        // Обновляем статус выполнения
        taskUser.setIsCompleted(request.getIsCompleted());

        if (Boolean.TRUE.equals(request.getIsCompleted())) {
            taskUser.setCompletedAt(Instant.now());
            taskUser.setCompletedBy(currentUserId);
            log.info("Executor {} marked task {} as completed", currentUserId, taskId);
        } else {
            taskUser.setCompletedAt(null);
            taskUser.setCompletedBy(null);
            log.info("Executor {} unmarked task {} as completed", currentUserId, taskId);
        }

        taskUser = taskUserRepository.save(taskUser);

        // Если все исполнители выполнили задачу, отмечаем задачу как выполненную
        updateTaskCompletionStatus(task);

        return buildCompletionResponse(taskUser);
    }

    /**
     * Обновить статус выполнения задачи для любого пользователя (только для создателя)
     */
    @Transactional
    public TaskCompletionResponse updateTaskCompletionByCreator(Long taskId, Long userId, UpdateTaskCompletionRequest request, Long currentUserId) {
        log.info("Updating task completion by creator: taskId={}, userId={}, isCompleted={}, currentUserId={}",
                taskId, userId, request.getIsCompleted(), currentUserId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ValidationException("Задача не найдена: " + taskId));

        if (Boolean.TRUE.equals(task.getIsDeleted())) {
            throw new ValidationException("Задача удалена");
        }

        // Проверяем, что пользователь является создателем
        if (!task.getCreator().getId().equals(currentUserId)) {
            throw new ValidationException("Только создатель задачи может изменять статус выполнения для других пользователей");
        }

        // Проверяем существование пользователя
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден: " + userId));

        // Находим запись о назначении
        TaskUser taskUser = taskUserRepository.findActiveByTaskIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ValidationException(
                        String.format("Пользователь %d не назначен на задачу %d", userId, taskId)));

        // Обновляем статус выполнения
        taskUser.setIsCompleted(request.getIsCompleted());

        if (Boolean.TRUE.equals(request.getIsCompleted())) {
            taskUser.setCompletedAt(Instant.now());
            taskUser.setCompletedBy(currentUserId);
            log.info("Creator {} marked user {} task {} as completed", currentUserId, userId, taskId);
        } else {
            taskUser.setCompletedAt(null);
            taskUser.setCompletedBy(null);
            log.info("Creator {} unmarked user {} task {} as completed", currentUserId, userId, taskId);
        }

        taskUser = taskUserRepository.save(taskUser);

        // Если все исполнители выполнили задачу, отмечаем задачу как выполненную
        updateTaskCompletionStatus(task);

        return buildCompletionResponse(taskUser);
    }

    /**
     * Универсальный метод для обновления статуса выполнения
     * Автоматически определяет роль пользователя
     */
    @Transactional
    public TaskCompletionResponse markTaskCompletedByExecutor(Long taskId, UpdateTaskCompletionRequest request, Long currentUserId) {
        log.info("Executor marking task completion: taskId={}, isCompleted={}, executorId={}",
                taskId, request.getIsCompleted(), currentUserId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ValidationException("Задача не найдена: " + taskId));

        if (Boolean.TRUE.equals(task.getIsDeleted())) {
            throw new ValidationException("Задача удалена");
        }

        // Проверяем, что пользователь назначен на задачу
        TaskUser taskUser = taskUserRepository.findActiveByTaskIdAndUserId(taskId, currentUserId)
                .orElseThrow(() -> new ValidationException(
                        String.format("Пользователь %d не назначен на задачу %d", currentUserId, taskId)));

        // Обновляем статус выполнения у пользователя
        taskUser.setIsCompleted(request.getIsCompleted());

        if (Boolean.TRUE.equals(request.getIsCompleted())) {
            taskUser.setCompletedAt(Instant.now());
            taskUser.setCompletedBy(currentUserId);
            log.info("Executor {} marked task {} as completed", currentUserId, taskId);
        } else {
            taskUser.setCompletedAt(null);
            taskUser.setCompletedBy(null);
            log.info("Executor {} unmarked task {} as completed", currentUserId, taskId);
        }

        taskUser = taskUserRepository.save(taskUser);

        return buildCompletionResponse(taskUser);
    }

    /**
     * Создатель отмечает задачу как выполненную (обновляется tasks)
     */
    @Transactional
    public TaskCompletionResponse markTaskCompletedByCreator(Long taskId, Boolean isCompleted, Long currentUserId) {
        log.info("Creator marking task completion: taskId={}, isCompleted={}, creatorId={}",
                taskId, isCompleted, currentUserId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ValidationException("Задача не найдена: " + taskId));

        if (Boolean.TRUE.equals(task.getIsDeleted())) {
            throw new ValidationException("Задача удалена");
        }

        // Проверяем, что пользователь является создателем
        if (!task.getCreator().getId().equals(currentUserId)) {
            throw new ValidationException("Только создатель может отметить задачу как выполненную");
        }

        // Обновляем статус задачи
        task.setIsCompleted(isCompleted);
        task = taskRepository.save(task);
        log.info("Creator {} marked task {} as completed: {}", currentUserId, taskId, isCompleted);

        // Возвращаем информацию о создателе

        eventPublisher.publishEvent(new TaskCompletedEvent(
                task.getId(),           // Long
                task.getTitle(),
                currentUserId
        ));

        return TaskCompletionResponse.builder()
                .taskId(task.getId())
                .taskTitle(task.getTitle())
                .userId(task.getCreator().getId())
                .userName(task.getCreator().getName())
                .userSurname(task.getCreator().getSurname())
                .isCompleted(task.getIsCompleted())
                .build();
    }

    /**
     * Получить статус выполнения задачи для пользователя (из task_users)
     */
    @Transactional(readOnly = true)
    public TaskCompletionResponse getTaskCompletionStatus(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ValidationException("Задача не найдена: " + taskId));

        TaskUser taskUser = taskUserRepository.findActiveByTaskIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ValidationException(
                        String.format("Пользователь %d не назначен на задачу %d", userId, taskId)));

        return buildCompletionResponse(taskUser);
    }

    /**
     * Получить все статусы выполнения для задачи (всех исполнителей из task_users)
     */
    @Transactional(readOnly = true)
    public List<TaskCompletionResponse> getAllTaskCompletionStatuses(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ValidationException("Задача не найдена: " + taskId));

        List<TaskUser> taskUsers = taskUserRepository.findActiveByTaskId(taskId);

        return taskUsers.stream()
                .map(this::buildCompletionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Получить статус задачи (из tasks) - только для создателя
     */
    @Transactional(readOnly = true)
    public TaskCompletionResponse getTaskStatus(Long taskId, Long currentUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ValidationException("Задача не найдена: " + taskId));

        if (Boolean.TRUE.equals(task.getIsDeleted())) {
            throw new ValidationException("Задача удалена");
        }

        // Проверяем, что пользователь является создателем
        if (!task.getCreator().getId().equals(currentUserId)) {
            throw new ValidationException("Только создатель может просматривать статус задачи");
        }

        return TaskCompletionResponse.builder()
                .taskId(task.getId())
                .taskTitle(task.getTitle())
                .userId(task.getCreator().getId())
                .userName(task.getCreator().getName())
                .userSurname(task.getCreator().getSurname())
                .isCompleted(task.getIsCompleted())
                .build();
    }

    /**
     * Построить ответ о статусе выполнения
     */
    private TaskCompletionResponse buildCompletionResponse(TaskUser taskUser) {
        String completedByName = null;
        if (taskUser.getCompletedBy() != null) {
            completedByName = userRepository.findById(taskUser.getCompletedBy())
                    .map(user -> user.getName() + " " + user.getSurname())
                    .orElse(null);
        }

        return TaskCompletionResponse.builder()
                .taskUserId(taskUser.getId())
                .taskId(taskUser.getTask().getId())
                .taskTitle(taskUser.getTask().getTitle())
                .userId(taskUser.getUser().getId())
                .userName(taskUser.getUser().getName())
                .userSurname(taskUser.getUser().getSurname())
                .isCompleted(taskUser.getIsCompleted())
                .completedAt(taskUser.getCompletedAt())
                .completedBy(taskUser.getCompletedBy())
                .completedByName(completedByName)
                .build();
    }

    private void updateTaskCompletionStatus(Task task) {
        List<TaskUser> incompleteUsers = taskUserRepository.findActiveIncompleteByTaskId(task.getId());

        if (incompleteUsers.isEmpty()) {
            if (!Boolean.TRUE.equals(task.getIsCompleted())) {
                task.setIsCompleted(true);
                taskRepository.save(task);
                log.info("Task {} automatically marked as completed", task.getId());
            }
        } else {
            if (Boolean.TRUE.equals(task.getIsCompleted())) {
                task.setIsCompleted(false);
                taskRepository.save(task);
                log.info("Task {} automatically unmarked as completed because some users haven't completed it", task.getId());
            }
        }
    }
}
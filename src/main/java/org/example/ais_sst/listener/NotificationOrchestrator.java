package org.example.ais_sst.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.entity.Task;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.event.tasks.*;
import org.example.ais_sst.repository.SectorParticipantRepository;
import org.example.ais_sst.repository.TaskRepository;
import org.example.ais_sst.repository.UserRepository;
import org.example.ais_sst.service.notificationService.EmailService;
import org.example.ais_sst.service.notificationService.PushNotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOrchestrator {

    private final EmailService emailService;
    private final PushNotificationService inAppService;
;
    private final SectorParticipantRepository sectorParticipantRepository; // твой репозиторий
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    //    @EventListener
//    public void onEventCreated(EventCreatedEvent event) {
//        // Логика принятия решения
//        inAppService.sendToAll(...);
//        emailService.sendToAdmins(...);
//    }
//
//    @EventListener
//    public void onPointsAwarded(PointsAwardedEvent event) {
//        inAppService.sendToUser(event.getUserId(), ...);
//        // emailService.send(...) — если нужно
//    }

    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskRequestSubmitted(TaskRequestSubmittedEvent event) {
        // Отправляем только создателю задачи (не заявителю)
        if (!event.getApplicantId().equals(event.getTaskCreatorId())) {
            inAppService.sendToUser(
                    "user" + event.getTaskCreatorId(),
                    "Новая заявка на задачу: " + event.getTaskTitle(),
                    "INFO"
            );
        }
    }

    // ==================== ЗАЯВКА ОДОБРЕНА ====================
    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskRequestApproved(TaskRequestApprovedEvent event) {
        inAppService.sendToUser(
                "user" + event.getApplicantId(),
                "Ваша заявка на задачу \"" + event.getTaskTitle() + "\" одобрена",
                "SUCCESS"
        );
    }

    // ==================== ЗАЯВКА ОТКЛОНЕНА ====================
    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskRequestRejected(TaskRequestRejectedEvent event) {
        inAppService.sendToUser(
                "user" + event.getApplicantId(),
                "Ваша заявка на задачу \"" + event.getTaskTitle() + "\" отклонена",
                "WARNING"
        );
    }

    // ==================== ЗАДАЧА ВЫПОЛНЕНА ====================
    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCompleted(TaskCompletedEvent event) {

        // Получаем задачу
        Task task = taskRepository.findById(event.getTaskId())
                .orElse(null);

        if (task == null || task.getCreator() == null) {
            log.warn("Task or creator not found for taskId={}", event.getTaskId());
            return;
        }

        Long creatorId = task.getCreator().getId();

        // Не отправляем создателю, если он сам отметил задачу выполненной
        if (creatorId.equals(event.getCompletedByUserId())) {
            log.debug("Creator marked their own task as completed. Skipping notification. taskId={}", event.getTaskId());
            return;
        }

        // Получаем ФИО того, кто отметил задачу
        User completedBy = userRepository.findById(event.getCompletedByUserId())
                .orElse(null);

        String completedByName = getFullName(completedBy);

        // Формируем сообщение
        String message = String.format(
                "%s отметил задачу \"%s\" как выполненную",
                completedByName,
                event.getTaskTitle()
        );

        // Отправляем только создателю задачи
        inAppService.sendToUser(
                "user" + creatorId,
                message,
                "SUCCESS"
        );

        log.info("Sent TaskCompleted notification to creator userId={}: {}", creatorId, message);
    }

    private String getFullName(User user) {
        if (user == null) return "Пользователь";

        StringBuilder sb = new StringBuilder();
        if (user.getSurname() != null) sb.append(user.getSurname()).append(" ");
        if (user.getName() != null) sb.append(user.getName());
        if (user.getPatronymic() != null && !user.getPatronymic().isBlank()) {
            sb.append(" ").append(user.getPatronymic());
        }
        return sb.toString().trim();
    }

    // ==================== НАЗНАЧЕН ИСПОЛНИТЕЛЬ ====================
    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskAssigned(TaskAssignedEvent event) {
        inAppService.sendToUser(
                "user" + event.getAssignedUserId(),
                "Вас назначили исполнителем на задачу: " + event.getTaskTitle(),
                "INFO"
        );
    }
}

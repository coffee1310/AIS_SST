package org.example.ais_sst.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.controller.base.BaseController;
import org.example.ais_sst.dto.events.EventReportDTO;
import org.example.ais_sst.dto.user.UserRatingDTO;
import org.example.ais_sst.service.eventService.EventService;
import org.example.ais_sst.service.userService.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Контроллер для генерации отчетов в JSON формате.
 * Поддерживает отчеты по пользователям (с рейтингом), мероприятиям и секторам.
 * Пока реализован отчет по пользователям с использованием существующей рейтинговой системы.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController extends BaseController {

    private final UserService userService;
    private final EventService eventService;

    /**
     * Отчет по пользователям: выводит список пользователей отсортированный по убыванию баллов (рейтингу).
     * Каждый пользователь имеет: позицию в рейтинге (rank), ФИО, роль, баллы.
     * Поддерживает фильтрацию по параметрам.
     *
     * @param role      фильтр по роли (например, STUDENT, COORDINATOR) - частичное совпадение, case-insensitive
     * @param minPoints минимальное количество баллов для включения в отчет
     * @param limit     ограничить количество записей в отчете (топ N)
     * @return JSON список UserRatingDTO с заполненными position, fio, role, totalPoints
     */
    @GetMapping("/users")
    @Operation(summary = "Отчет по пользователям с рейтингом",
            description = "Генерирует JSON отчет по всем пользователям (не удаленным). " +
                    "Включает последовательный рейтинг (позицию), ФИО, роль и баллы. " +
                    "Рейтинг рассчитывается по всем источникам: участие в секторах, организация, участие в мероприятиях, выполненные задачи. " +
                    "Поддерживает параметры фильтрации.")
    public ResponseEntity<List<UserRatingDTO>> getUsersReport(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long minPoints,
            @RequestParam(required = false) Integer limit) {

        logInfo("GET /api/reports/users",
                String.format("Генерация отчета по пользователям. role=%s, minPoints=%s, limit=%s",
                        role, minPoints, limit));

        List<UserRatingDTO> report = userService.getAllUsersRankedByPoints();

        // Фильтрация по роли (если указана)
        if (role != null && !role.isBlank()) {
            String roleFilter = role.trim().toUpperCase();
            report = report.stream()
                    .filter(u -> u.getRole() != null && u.getRole().toUpperCase().contains(roleFilter))
                    .collect(Collectors.toList());
        }

        // Фильтрация по минимальным баллам
        if (minPoints != null && minPoints > 0) {
            report = report.stream()
                    .filter(u -> u.getTotalPoints() != null && u.getTotalPoints() >= minPoints)
                    .collect(Collectors.toList());
        }

        // Ограничение по limit (топ N после фильтров)
        if (limit != null && limit > 0 && limit < report.size()) {
            report = report.subList(0, limit);
        }

        logInfo("GET /api/reports/users", "Отчет сгенерирован. Количество записей: " + report.size());
        return ResponseEntity.ok(report);
    }

    /**
     * Заглушка для отчета по мероприятиям.
     */
    @GetMapping("/events")
    @Operation(summary = "Отчет по мероприятиям",
            description = "Возвращает JSON со статистикой по всем мероприятиям (не удалённым). " +
                    "Включает количество организаторов, участников, исполнителей (через роли/сектора) и общее число людей.")
    public ResponseEntity<List<EventReportDTO>> getEventsReport(
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(required = false) Boolean isFree,
            @RequestParam(required = false) Integer limit) {

        logInfo("GET /api/reports/events",
                String.format("Генерация отчета по мероприятиям. completed=%s, isPublic=%s, isFree=%s, limit=%s",
                        completed, isPublic, isFree, limit));

        List<EventReportDTO> report = eventService.getEventsReport();

        // Применяем фильтры если указаны
        if (completed != null) {
            report = report.stream()
                    .filter(e -> e.getIsCompleted() != null && e.getIsCompleted().equals(completed))
                    .collect(Collectors.toList());
        }
        if (isPublic != null) {
            report = report.stream()
                    .filter(e -> e.getIsPublic() != null && e.getIsPublic().equals(isPublic))
                    .collect(Collectors.toList());
        }
        if (isFree != null) {
            report = report.stream()
                    .filter(e -> e.getIsFreeEvent() != null && e.getIsFreeEvent().equals(isFree))
                    .collect(Collectors.toList());
        }
        if (limit != null && limit > 0 && limit < report.size()) {
            report = report.subList(0, limit);
        }

        logInfo("GET /api/reports/events", "Отчет по мероприятиям сгенерирован. Записей: " + report.size());
        return ResponseEntity.ok(report);
    }


    /**
     * Заглушка для отчета по секторам.
     */
    @GetMapping("/sectors")
    @Operation(summary = "Отчет по секторам (в разработке)")
    public ResponseEntity<String> getSectorsReport() {
        logInfo("GET /api/reports/sectors", "Запрос отчета по секторам");
        return ResponseEntity.ok("Отчет по секторам пока в разработке. " +
                "Будет включать статистику по секторам, участникам, мероприятиям и т.д.");
    }
}
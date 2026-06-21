package org.example.ais_sst.specification;

import jakarta.persistence.criteria.*;
import org.example.ais_sst.dto.tasks.TaskFilterDTO;
import org.example.ais_sst.entity.Task;
import org.example.ais_sst.entity.TaskUser;
import org.example.ais_sst.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TaskSpecification {

    public static Specification<Task> withFilter(TaskFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<Predicate> orPredicates = new ArrayList<>();

            // Фильтр по ID
            if (filter.getId() != null) {
                predicates.add(cb.equal(root.get("id"), filter.getId()));
            }

            // Фильтр по названию (частичное совпадение)
            if (filter.getTitle() != null && !filter.getTitle().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + filter.getTitle().toLowerCase() + "%"));
            }

            // Фильтр по описанию (частичное совпадение)
            if (filter.getDescription() != null && !filter.getDescription().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("description")), "%" + filter.getDescription().toLowerCase() + "%"));
            }

            // Фильтр по создателю
            if (filter.getCreatorId() != null) {
                predicates.add(cb.equal(root.get("creator").get("id"), filter.getCreatorId()));
            }

            // Фильтр по дедлайну (от)
            if (filter.getDeadlineFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("deadline"), filter.getDeadlineFrom()));
            }

            // Фильтр по дедлайну (до)
            if (filter.getDeadlineTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("deadline"), filter.getDeadlineTo()));
            }

            // Фильтр по максимальному количеству людей
            if (filter.getMaxPeopleCount() != null) {
                predicates.add(cb.equal(root.get("maxPeopleCount"), filter.getMaxPeopleCount()));
            }

            // Фильтр по количеству баллов
            if (filter.getCountOfPoints() != null) {
                predicates.add(cb.equal(root.get("countOfPoints"), filter.getCountOfPoints()));
            }

            // Фильтр по статусу выполнения
            if (filter.getIsCompleted() != null) {
                predicates.add(cb.equal(root.get("isCompleted"), filter.getIsCompleted()));
            }

            // Фильтр по статусу удаления
            if (filter.getIsDeleted() != null) {
                predicates.add(cb.equal(root.get("isDeleted"), filter.getIsDeleted()));
            }

            // Фильтр по признаку предварительного назначения
            if (filter.getIsPreassigned() != null) {
                predicates.add(cb.equal(root.get("isPreassigned"), filter.getIsPreassigned()));
            }

            // ===== НОВЫЕ ФИЛЬТРЫ =====

            // 1. Задачи созданные мной (createdByMe = true)
            if (filter.getCreatedByMe() != null && filter.getCreatedByMe() && filter.getCurrentUserId() != null) {
                predicates.add(cb.equal(root.get("creator").get("id"), filter.getCurrentUserId()));
            }

            // 2. Задачи назначенные мне (assignedToMe = true) - которые я выполняю
            if (filter.getAssignedToMe() != null && filter.getAssignedToMe() && filter.getCurrentUserId() != null) {
                Join<Task, TaskUser> taskUsersJoin = root.join("taskUsers", JoinType.INNER);
                predicates.add(cb.equal(taskUsersJoin.get("user").get("id"), filter.getCurrentUserId()));
                predicates.add(cb.isFalse(taskUsersJoin.get("isDeleted")));
                query.distinct(true);
            }

            // 3. Комбинированный фильтр: задачи созданные мной ИЛИ назначенные мне
            // (если оба флага true, показываем все задачи, где пользователь участвует)
            if (filter.getCreatedByMe() != null && filter.getAssignedToMe() != null
                    && filter.getCreatedByMe() && filter.getAssignedToMe()
                    && filter.getCurrentUserId() != null) {

                // Создаем два подзапроса для OR условия
                // Это позволяет показывать задачи, где пользователь И создатель, И исполнитель
                // Используем OR для объединения
                Predicate createdByMePredicate = cb.equal(root.get("creator").get("id"), filter.getCurrentUserId());

                Join<Task, TaskUser> taskUsersJoin = root.join("taskUsers", JoinType.LEFT);
                Predicate assignedToMePredicate = cb.and(
                        cb.equal(taskUsersJoin.get("user").get("id"), filter.getCurrentUserId()),
                        cb.isFalse(taskUsersJoin.get("isDeleted"))
                );

                // Очищаем предыдущие предикаты для этих фильтров и добавляем OR
                predicates.removeIf(p -> {
                    // Удаляем предыдущие предикаты для createdByMe и assignedToMe
                    return p.toString().contains("creator.id") || p.toString().contains("taskUsers");
                });

                predicates.add(cb.or(createdByMePredicate, assignedToMePredicate));
                query.distinct(true);
            }

            // Если не указан фильтр по isDeleted, показываем только не удаленные
            if (filter.getIsDeleted() == null) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
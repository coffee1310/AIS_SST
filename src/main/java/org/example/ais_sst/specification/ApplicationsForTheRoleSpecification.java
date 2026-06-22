package org.example.ais_sst.specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.example.ais_sst.dto.applications.ApplicationsForTheRoleFilterDTO;
import org.example.ais_sst.entity.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ApplicationsForTheRoleSpecification {

    public static Specification<ApplicationsForTheRole> withFilter(ApplicationsForTheRoleFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // ⭐ JOIN'ы для доступа к связанным полям
            Join<ApplicationsForTheRole, SectorParticipant> sectorParticipantJoin =
                    root.join("sectorParticipant", JoinType.LEFT);
            Join<SectorParticipant, User> userJoin =
                    sectorParticipantJoin.join("student", JoinType.LEFT);
            Join<SectorParticipant, Sector> sectorJoin =
                    sectorParticipantJoin.join("sector", JoinType.LEFT);
            Join<ApplicationsForTheRole, EventRole> eventRoleJoin =
                    root.join("eventRole", JoinType.LEFT);
            Join<EventRole, GlobalEventRole> globalEventRoleJoin =
                    eventRoleJoin.join("globalEventRole", JoinType.LEFT);

            // ⭐ ПРАВИЛЬНО: Event получаем через EventRole, а не через GlobalEventRole!
            Join<EventRole, Event> eventJoin =
                    eventRoleJoin.join("event", JoinType.LEFT);

            // ⭐ Фильтрация по ID заявки
            if (filter.getId() != null) {
                predicates.add(cb.equal(root.get("id"), filter.getId()));
            }

            // ⭐ Фильтрация по ID сектор-участника
            if (filter.getSectorParticipantId() != null) {
                predicates.add(cb.equal(root.get("sectorParticipant").get("id"), filter.getSectorParticipantId()));
            }

            // ⭐ Фильтрация по ID роли события
            if (filter.getEventRoleId() != null) {
                predicates.add(cb.equal(root.get("eventRole").get("id"), filter.getEventRoleId()));
            }

            // ⭐ Фильтрация по ID сектора
            if (filter.getSectorId() != null) {
                predicates.add(cb.equal(sectorJoin.get("id"), filter.getSectorId()));
            }

            // ⭐ Фильтрация по ID события - ПРАВИЛЬНО через eventJoin
            if (filter.getEventId() != null) {
                predicates.add(cb.equal(eventJoin.get("id"), filter.getEventId()));
            }

            // ⭐ Фильтрация по ID пользователя
            if (filter.getUserId() != null) {
                predicates.add(cb.equal(userJoin.get("id"), filter.getUserId()));
            }

            // ⭐ Фильтрация по статусу резерва
            if (filter.getIsReserve() != null) {
                predicates.add(cb.equal(root.get("isReserve"), filter.getIsReserve()));
            }

            // ⭐ Фильтрация по статусу заявки
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            // ⭐ Фильтрация по причине отклонения (частичное совпадение)
            if (filter.getRejectionReason() != null && !filter.getRejectionReason().isEmpty()) {
                predicates.add(cb.like(
                        cb.lower(root.get("rejectionReason")),
                        "%" + filter.getRejectionReason().toLowerCase() + "%"
                ));
            }

            // ⭐ Фильтрация по описанию (частичное совпадение)
            if (filter.getDescription() != null && !filter.getDescription().isEmpty()) {
                predicates.add(cb.like(
                        cb.lower(root.get("description")),
                        "%" + filter.getDescription().toLowerCase() + "%"
                ));
            }

            // ⭐ Фильтрация по дате создания
            if (filter.getCreatedAtFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedAtFrom()));
            }
            if (filter.getCreatedAtTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getCreatedAtTo()));
            }

            // ⭐ Фильтрация по дате обновления
            if (filter.getUpdatedAtFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), filter.getUpdatedAtFrom()));
            }
            if (filter.getUpdatedAtTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("updatedAt"), filter.getUpdatedAtTo()));
            }

            // ⭐ Добавляем DISTINCT чтобы избежать дубликатов при JOIN'ах
            query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
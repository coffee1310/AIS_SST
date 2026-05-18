package org.example.ais_sst.specification;

import jakarta.persistence.criteria.*;
import org.example.ais_sst.dto.event_roles_application.RoleApplicationFilterDTO;
import org.example.ais_sst.entity.*;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class RoleApplicationSpecification {

    public static Specification<ApplicationsForTheRole> withFilter(RoleApplicationFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Фильтр по ID заявки
            if (filter.getId() != null) {
                predicates.add(cb.equal(root.get("id"), filter.getId()));
            }

            // Фильтр по ID участника сектора
            if (filter.getSectorParticipantId() != null) {
                predicates.add(cb.equal(root.get("sectorParticipant").get("id"), filter.getSectorParticipantId()));
            }

            // Фильтр по ID роли мероприятия
            if (filter.getEventRoleId() != null) {
                predicates.add(cb.equal(root.get("eventRole").get("id"), filter.getEventRoleId()));
            }

            // Фильтр по ID мероприятия (через связь eventRole -> event)
            if (filter.getEventId() != null) {
                Join<ApplicationsForTheRole, EventRole> eventRoleJoin = root.join("eventRole", JoinType.INNER);
                Join<EventRole, Event> eventJoin = eventRoleJoin.join("event", JoinType.INNER);
                predicates.add(cb.equal(eventJoin.get("id"), filter.getEventId()));
            }

            // Фильтр по статусу заявки
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            // Фильтр по резерву
            if (filter.getIsReserve() != null) {
                predicates.add(cb.equal(root.get("isReserve"), filter.getIsReserve()));
            }

            // Фильтр по дате создания (от)
            if (filter.getDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getDateFrom()));
            }

            // Фильтр по дате создания (до)
            if (filter.getDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getDateTo()));
            }

            // Фильтр по описанию (поиск по частичному совпадению)
            if (filter.getDescription() != null && !filter.getDescription().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("description")), "%" + filter.getDescription().toLowerCase() + "%"));
            }

            // Фильтр по студенту (через sectorParticipant -> student)
            if (filter.getStudentId() != null) {
                Join<ApplicationsForTheRole, SectorParticipant> sectorParticipantJoin = root.join("sectorParticipant", JoinType.INNER);
                Join<SectorParticipant, User> studentJoin = sectorParticipantJoin.join("student", JoinType.INNER);
                predicates.add(cb.equal(studentJoin.get("id"), filter.getStudentId()));
            }

            // Добавляем DISTINCT для избежания дубликатов при JOIN
            query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

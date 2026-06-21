package org.example.ais_sst.specification;

import jakarta.persistence.criteria.*;
import org.example.ais_sst.dto.event_participation.EventParticipantFilterDTO;
import org.example.ais_sst.entity.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class EventParticipantSpecification {

    public static Specification<EventParticipationRecord> withFilter(EventParticipantFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Фильтр по мероприятию
            if (filter.getEventId() != null) {
                predicates.add(cb.equal(root.get("eventRole").get("event").get("id"), filter.getEventId()));
            }

            // Фильтр по ФИО (через SectorParticipant -> User)
            if (filter.getFullName() != null && !filter.getFullName().isEmpty()) {
                Join<EventParticipationRecord, SectorParticipant> sectorParticipantJoin = root.join("sectorParticipant");
                Join<SectorParticipant, User> userJoin = sectorParticipantJoin.join("student");

                String searchTerm = "%" + filter.getFullName().toLowerCase() + "%";
                Predicate namePredicate = cb.or(
                        cb.like(cb.lower(userJoin.get("name")), searchTerm),
                        cb.like(cb.lower(userJoin.get("surname")), searchTerm),
                        cb.like(cb.lower(cb.concat(cb.concat(userJoin.get("name"), " "), userJoin.get("surname"))), searchTerm)
                );
                predicates.add(namePredicate);
            }

            // Фильтр по баллам
            if (filter.getMinPoints() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalPoints"), filter.getMinPoints()));
            }
            if (filter.getMaxPoints() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalPoints"), filter.getMaxPoints()));
            }

            // Фильтр по присутствию
            if (filter.getWasPresent() != null) {
                predicates.add(cb.equal(root.get("wasPresent"), filter.getWasPresent()));
            }

            // Фильтр по роли (через EventRole -> GlobalEventRole)
            if (filter.getRole() != null && !filter.getRole().isEmpty()) {
                Join<EventParticipationRecord, EventRole> eventRoleJoin = root.join("eventRole");
                Join<EventRole, GlobalEventRole> globalEventRoleJoin = eventRoleJoin.join("globalEventRole");
                predicates.add(cb.like(cb.lower(globalEventRoleJoin.get("title")),
                        "%" + filter.getRole().toLowerCase() + "%"));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Спецификация для EventOrganizer
    public static Specification<EventOrganizer> organizerWithFilter(EventParticipantFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getEventId() != null) {
                predicates.add(cb.equal(root.get("event").get("id"), filter.getEventId()));
            }

            if (filter.getFullName() != null && !filter.getFullName().isEmpty()) {
                Join<EventOrganizer, User> userJoin = root.join("user");
                String searchTerm = "%" + filter.getFullName().toLowerCase() + "%";
                Predicate namePredicate = cb.or(
                        cb.like(cb.lower(userJoin.get("name")), searchTerm),
                        cb.like(cb.lower(userJoin.get("surname")), searchTerm),
                        cb.like(cb.lower(cb.concat(cb.concat(userJoin.get("name"), " "), userJoin.get("surname"))), searchTerm)
                );
                predicates.add(namePredicate);
            }

            if (filter.getMinPoints() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalPoints"), filter.getMinPoints()));
            }
            if (filter.getMaxPoints() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalPoints"), filter.getMaxPoints()));
            }
            if (filter.getWasPresent() != null) {
                predicates.add(cb.equal(root.get("wasPresent"), filter.getWasPresent()));
            }

            // Организаторы - это всегда роль "ОРГАНИЗАТОР"
            if (filter.getRole() != null && !filter.getRole().isEmpty()) {
                predicates.add(cb.like(cb.literal("ОРГАНИЗАТОР"), "%" + filter.getRole().toLowerCase() + "%"));
            }

            predicates.add(cb.isFalse(root.get("isDeleted")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Спецификация для EventParticipant
    public static Specification<EventParticipant> participantWithFilter(EventParticipantFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getEventId() != null) {
                predicates.add(cb.equal(root.get("event").get("id"), filter.getEventId()));
            }

            if (filter.getFullName() != null && !filter.getFullName().isEmpty()) {
                Join<EventParticipant, User> userJoin = root.join("user");
                String searchTerm = "%" + filter.getFullName().toLowerCase() + "%";
                Predicate namePredicate = cb.or(
                        cb.like(cb.lower(userJoin.get("name")), searchTerm),
                        cb.like(cb.lower(userJoin.get("surname")), searchTerm),
                        cb.like(cb.lower(cb.concat(cb.concat(userJoin.get("name"), " "), userJoin.get("surname"))), searchTerm)
                );
                predicates.add(namePredicate);
            }

            if (filter.getMinPoints() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalPoints"), filter.getMinPoints()));
            }
            if (filter.getMaxPoints() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalPoints"), filter.getMaxPoints()));
            }
            if (filter.getWasPresent() != null) {
                predicates.add(cb.equal(root.get("wasPresent"), filter.getWasPresent()));
            }

            // Участники - это всегда роль "УЧАСТНИК"
            if (filter.getRole() != null && !filter.getRole().isEmpty()) {
                predicates.add(cb.like(cb.literal("УЧАСТНИК"), "%" + filter.getRole().toLowerCase() + "%"));
            }

            predicates.add(cb.isFalse(root.get("isDeleted")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
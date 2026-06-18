package org.example.ais_sst.specification;

import jakarta.persistence.criteria.*;
import org.example.ais_sst.dto.events.EventFilterDTO;
import org.example.ais_sst.entity.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class EventSpecification {

    public static Specification<Event> withFilter(EventFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getId() != null) {
                predicates.add(cb.equal(root.get("id"), filter.getId()));
            }
            if (filter.getTitle() != null && !filter.getTitle().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + filter.getTitle().toLowerCase() + "%"));
            }
            if (filter.getDescription() != null && !filter.getDescription().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("description")), "%" + filter.getDescription().toLowerCase() + "%"));
            }
            if (filter.getVenue() != null && !filter.getVenue().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("venue")), "%" + filter.getVenue().toLowerCase() + "%"));
            }
            if (filter.getReferenceToPosition() != null && !filter.getReferenceToPosition().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("referenceToPosition")), "%" + filter.getReferenceToPosition().toLowerCase() + "%"));
            }
            if (filter.getDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateOfEvent"), filter.getDateFrom()));
            }
            if (filter.getDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateOfEvent"), filter.getDateTo()));
            }
            if (filter.getStartTimeFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), filter.getStartTimeFrom()));
            }
            if (filter.getStartTimeTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startTime"), filter.getStartTimeTo()));
            }
            if (filter.getEndTimeFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endTime"), filter.getEndTimeFrom()));
            }
            if (filter.getEndTimeTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endTime"), filter.getEndTimeTo()));
            }
            if (filter.getIsPublic() != null) {
                predicates.add(cb.equal(root.get("isPublic"), filter.getIsPublic()));
            }
            if (filter.getIsDraft() != null) {
                predicates.add(cb.equal(root.get("isDraft"), filter.getIsDraft()));
            }
            if (filter.getIsCompleted() != null) {
                predicates.add(cb.equal(root.get("isCompleted"), filter.getIsCompleted()));
            }
            if (filter.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), filter.getIsActive()));
            }
            if (filter.getCreatorId() != null) {
                predicates.add(cb.equal(root.get("eventCreator").get("id"), filter.getCreatorId()));
            }
            if (filter.getIsDeleted() != null) {
                predicates.add(cb.equal(root.get("isDeleted"), filter.getIsDeleted()));
            }

            if (filter.getIsOrganizer() != null && filter.getIsOrganizer() && filter.getCurrentUserId() != null) {
                System.out.println("Applying organizer filter for user: " + filter.getCurrentUserId());
                // Используем INNER JOIN
                Join<Event, EventOrganizer> organizersJoin = root.join("organizers", JoinType.INNER);
                predicates.add(cb.equal(organizersJoin.get("user").get("id"), filter.getCurrentUserId()));
                query.distinct(true);
            } else {
                System.out.println("Organizer filter NOT applied");
            }

            // Фильтрация по секторам, где пользователь является участником
            if (filter.getIsResponsibleSector() != null &&
                    filter.getIsResponsibleSector() &&
                    filter.getCurrentUserId() != null) {

                // Создаем JOIN'ы как в вашем SQL запросе
                Join<Event, EventRole> eventRoles = root.join("eventRoles", JoinType.LEFT);
                Join<EventRole, GlobalEventRole> globalEventRoles = eventRoles.join("globalEventRole", JoinType.LEFT);
                Join<GlobalEventRole, Sector> sectors = globalEventRoles.join("sector", JoinType.LEFT);
                Join<Sector, SectorParticipant> sectorParticipants = sectors.join("sectorParticipants", JoinType.LEFT);
                Join<SectorParticipant, User> users = sectorParticipants.join("student", JoinType.LEFT);

                // Добавляем условие по пользователю
                predicates.add(cb.equal(users.get("id"), filter.getCurrentUserId()));

                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
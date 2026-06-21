package org.example.ais_sst.specification;

import jakarta.persistence.criteria.*;
import org.example.ais_sst.dto.task_request.TaskRequestFilterDTO;
import org.example.ais_sst.entity.Task;
import org.example.ais_sst.entity.TaskRequest;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.entity.enums.TaskRequestStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TaskRequestSpecification {

    public static Specification<TaskRequest> withFilter(TaskRequestFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (filter.getId() != null) {
                predicates.add(cb.equal(root.get("id"), filter.getId()));
            }

            if (filter.getTaskId() != null) {
                predicates.add(cb.equal(root.get("task").get("id"), filter.getTaskId()));
            }

            if (filter.getStudentId() != null) {
                predicates.add(cb.equal(root.get("student").get("id"), filter.getStudentId()));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getFilingDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("filingDate"), filter.getFilingDateFrom()));
            }

            if (filter.getFilingDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("filingDate"), filter.getFilingDateTo()));
            }

            if (filter.getReviewedAtFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("reviewedAt"), filter.getReviewedAtFrom()));
            }

            if (filter.getReviewedAtTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("reviewedAt"), filter.getReviewedAtTo()));
            }

            Join<TaskRequest, Task> taskJoin = root.join("task", JoinType.INNER);

            if (filter.getTaskTitle() != null && !filter.getTaskTitle().isEmpty()) {
                predicates.add(cb.like(cb.lower(taskJoin.get("title")), "%" + filter.getTaskTitle().toLowerCase() + "%"));
            }

            if (filter.getTaskDescription() != null && !filter.getTaskDescription().isEmpty()) {
                predicates.add(cb.like(cb.lower(taskJoin.get("description")), "%" + filter.getTaskDescription().toLowerCase() + "%"));
            }

            if (filter.getTaskMaxPeopleCount() != null) {
                predicates.add(cb.equal(taskJoin.get("maxPeopleCount"), filter.getTaskMaxPeopleCount()));
            }

            if (filter.getTaskCountOfPoints() != null) {
                predicates.add(cb.equal(taskJoin.get("countOfPoints"), filter.getTaskCountOfPoints()));
            }

            if (filter.getTaskIsCompleted() != null) {
                predicates.add(cb.equal(taskJoin.get("isCompleted"), filter.getTaskIsCompleted()));
            }

            if (filter.getTaskIsPreassigned() != null) {
                predicates.add(cb.equal(taskJoin.get("isPreassigned"), filter.getTaskIsPreassigned()));
            }

            Join<TaskRequest, User> studentJoin = root.join("student", JoinType.INNER);

            if (filter.getStudentName() != null && !filter.getStudentName().isEmpty()) {
                predicates.add(cb.like(cb.lower(studentJoin.get("name")), "%" + filter.getStudentName().toLowerCase() + "%"));
            }

            if (filter.getStudentSurname() != null && !filter.getStudentSurname().isEmpty()) {
                predicates.add(cb.like(cb.lower(studentJoin.get("surname")), "%" + filter.getStudentSurname().toLowerCase() + "%"));
            }

            if (filter.getStudentEmail() != null && !filter.getStudentEmail().isEmpty()) {
                predicates.add(cb.like(cb.lower(studentJoin.get("studentEmail")), "%" + filter.getStudentEmail().toLowerCase() + "%"));
            }

            if (filter.getMyTasks() != null && filter.getMyTasks() && filter.getCurrentUserId() != null) {
                predicates.add(cb.equal(taskJoin.get("creator").get("id"), filter.getCurrentUserId()));
            }

            if (filter.getMyRequests() != null && filter.getMyRequests() && filter.getCurrentUserId() != null) {
                predicates.add(cb.equal(studentJoin.get("id"), filter.getCurrentUserId()));
            }

            if (filter.getPendingOnly() != null && filter.getPendingOnly()) {
                predicates.add(cb.equal(root.get("status"), TaskRequestStatus.НА_РАССМОТРЕНИИ));
            }

            if (filter.getReviewedOnly() != null && filter.getReviewedOnly()) {
                predicates.add(cb.notEqual(root.get("status"), TaskRequestStatus.НА_РАССМОТРЕНИИ));
            }

            if (filter.getApprovedOnly() != null && filter.getApprovedOnly()) {
                predicates.add(cb.equal(root.get("status"), TaskRequestStatus.ПРИНЯТО));
            }

            if (filter.getRejectedOnly() != null && filter.getRejectedOnly()) {
                predicates.add(cb.equal(root.get("status"), TaskRequestStatus.ОТКЛОНЕНО));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
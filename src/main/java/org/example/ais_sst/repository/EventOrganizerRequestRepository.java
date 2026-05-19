package org.example.ais_sst.repository;

import org.example.ais_sst.entity.Event;
import org.example.ais_sst.entity.EventOrganizerRequest;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface EventOrganizerRequestRepository extends
        JpaRepository<EventOrganizerRequest, Long>,
        JpaSpecificationExecutor<EventOrganizerRequest> {
    boolean existsByUser_Id(Long userId);

    boolean existsByUser_IdAndEvent_Id(Long userId, Long eventId);

    List<EventOrganizerRequest> findByUser(User user);

    List<EventOrganizerRequest> findByStatus(RoleApplicationStatuses status);

    Optional<EventOrganizerRequest> findByUserAndEvent(User user, Event event);

    List<EventOrganizerRequest> findByEventAndStatus(Event event, RoleApplicationStatuses status);
}

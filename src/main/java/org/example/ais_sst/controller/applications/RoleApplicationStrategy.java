package org.example.ais_sst.controller.applications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.event_roles_application.*;
import org.example.ais_sst.service.ApplicationsForTheRoleService.ApplicationsForTheRoleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleApplicationStrategy implements
        ApplicationStrategy<RoleApplicationResponseDTO,
                RoleApplicationCreateDTO,
                RoleApplicationRejectDTO,
                RoleApplicationFilterDTO> {

    private final ApplicationsForTheRoleService roleApplicationService;

    @Override
    public RoleApplicationResponseDTO createApplication(RoleApplicationCreateDTO createDto) {
        log.info("Creating role application");
        throw new UnsupportedOperationException("Use createApplication with eventRoleId and userId");
    }

    public RoleApplicationResponseDTO createApplication(Long eventRoleId, Long userId, String description) {
        return roleApplicationService.createApplication(eventRoleId, userId, description);
    }

    @Override
    public RoleApplicationResponseDTO rejectApplication(Long id, RoleApplicationRejectDTO rejectDto) {
        log.info("Role application with id: {} was rejected", id);
        return roleApplicationService.rejectApplication(id, rejectDto);
    }

    @Override
    public RoleApplicationResponseDTO acceptApplication(Long id) {
        log.info("Role application with id: {} was accepted", id);
        return roleApplicationService.approveApplication(id);
    }

    @Override
    public RoleApplicationResponseDTO getApplicationById(Long id) {
        log.info("Getting role application with id: {}", id);
        return roleApplicationService.getApplicationById(id);
    }

    @Override
    public Page<RoleApplicationResponseDTO> getAllApplications(RoleApplicationFilterDTO filter, Pageable pageable) {
        return roleApplicationService.getAllApplications(filter, pageable);
    }
}
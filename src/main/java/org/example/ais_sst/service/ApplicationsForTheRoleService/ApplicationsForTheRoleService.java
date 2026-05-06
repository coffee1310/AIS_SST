package org.example.ais_sst.service.ApplicationsForTheRoleService;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.event_roles_application.RoleApplicationFilterDTO;
import org.example.ais_sst.dto.event_roles_application.RoleApplicationRejectDTO;
import org.example.ais_sst.dto.event_roles_application.RoleApplicationResponseDTO;
import org.example.ais_sst.entity.ApplicationsForTheRole;
import org.example.ais_sst.entity.EventRole;
import org.example.ais_sst.entity.SectorParticipant;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;
import org.example.ais_sst.exception.ApplicationDoesNotExist;
import org.example.ais_sst.exception.DuplicateApplicationException;
import org.example.ais_sst.exception.EventRoleDoesNotFoundException;
import org.example.ais_sst.exception.SectorParticipantNotFoundException;
import org.example.ais_sst.mapper.RoleApplicationMapper;
import org.example.ais_sst.repository.EventRoleRepository;
import org.example.ais_sst.repository.ApplicationsForTheRoleRepository;
import org.example.ais_sst.repository.SectorParticipantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationsForTheRoleService {

    private final ApplicationsForTheRoleRepository roleApplicationRepository;
    private final SectorParticipantRepository sectorParticipantRepository;
    private final EventRoleRepository eventRoleRepository;
    private final RoleApplicationMapper roleApplicationMapper;

    /**
     * Подача заявки на роль
     */
    @Transactional
    public RoleApplicationResponseDTO createApplication(Long eventRoleId, Long userId) {
        log.info("Creating application for user: {}, eventRole: {}", userId, eventRoleId);

        // Получаем первый сектор пользователя (или можно передавать sectorParticipantId)
        List<SectorParticipant> userSectors = sectorParticipantRepository.findByStudentId(userId);

        if (userSectors.isEmpty()) {
            throw new SectorParticipantNotFoundException("Участник сектора не найден для пользователя с id: " + userId);
        }

        // Берем первый сектор (или можно добавить выбор сектора)
        SectorParticipant sectorParticipant = userSectors.get(0);

        EventRole eventRole = eventRoleRepository.findById(eventRoleId)
                .orElseThrow(() -> new EventRoleDoesNotFoundException("Роль мероприятия не найдена с id: " + eventRoleId));

        // Проверяем, не подавал ли уже заявку
        if (roleApplicationRepository.existsBySectorParticipantIdAndEventRoleId(sectorParticipant.getId(), eventRoleId)) {
            throw new DuplicateApplicationException("Вы уже подали заявку на эту роль");
        }

        // Подсчитываем количество уже одобренных заявок
        long approvedCount = roleApplicationRepository.countApprovedByEventRoleId(eventRoleId);
        int maxSlots = eventRole.getCapacity() != null ? eventRole.getCapacity() : Integer.MAX_VALUE;
        boolean isReserve = approvedCount >= maxSlots;

        ApplicationsForTheRole application = ApplicationsForTheRole.builder()
                .sectorParticipant(sectorParticipant)
                .eventRole(eventRole)
                .isReserve(isReserve)
                .status(RoleApplicationStatuses.НА_РАССМОТРЕНИИ)
                .build();

        ApplicationsForTheRole savedApplication = roleApplicationRepository.save(application);
        log.info("Application created with id: {}, isReserve: {}", savedApplication.getId(), isReserve);

        return roleApplicationMapper.toResponseDto(savedApplication);
    }

    /**
     * Получение заявки по ID
     */
    @Transactional(readOnly = true)
    public RoleApplicationResponseDTO getApplicationById(Long id) {
        log.info("Getting application by id: {}", id);

        ApplicationsForTheRole application = roleApplicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationDoesNotExist("Заявка не найдена с id: " + id));

        return roleApplicationMapper.toResponseDto(application);
    }

    /**
     * Одобрение заявки
     */
    @Transactional
    public RoleApplicationResponseDTO approveApplication(Long id) {
        log.info("Approving application: {}", id);

        ApplicationsForTheRole application = roleApplicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationDoesNotExist("Заявка не найдена с id: " + id));

        if (application.getStatus() != RoleApplicationStatuses.НА_РАССМОТРЕНИИ) {
            throw new IllegalStateException("Заявка уже обработана. Текущий статус: " + application.getStatus());
        }

        long approvedCount = roleApplicationRepository.countApprovedByEventRoleId(application.getEventRole().getId());
        int maxSlots = application.getEventRole().getCapacity() != null
                ? application.getEventRole().getCapacity()
                : Integer.MAX_VALUE;

        if (approvedCount < maxSlots) {
            application.setStatus(RoleApplicationStatuses.ОДОБРЕНА);
            log.info("Application approved as regular member");
        } else if (application.getIsReserve()) {
            application.setStatus(RoleApplicationStatuses.ОДОБРЕНА);
            log.info("Application approved as reserve member");
        } else {
            throw new IllegalStateException("Нет свободных мест для одобрения заявки. Максимум мест: " + maxSlots);
        }

        ApplicationsForTheRole savedApplication = roleApplicationRepository.save(application);
        return roleApplicationMapper.toResponseDto(savedApplication);
    }

    /**
     * Отклонение заявки
     */
    @Transactional
    public RoleApplicationResponseDTO rejectApplication(Long id, RoleApplicationRejectDTO rejectDto) {
        log.info("Rejecting application: {}", id);

        ApplicationsForTheRole application = roleApplicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationDoesNotExist("Заявка не найдена с id: " + id));

        if (application.getStatus() != RoleApplicationStatuses.НА_РАССМОТРЕНИИ) {
            throw new IllegalStateException("Заявка уже обработана. Текущий статус: " + application.getStatus());
        }

        application.setStatus(RoleApplicationStatuses.ОТКЛОНЕНА);
        application.setRejectionReason(rejectDto.getRejectionReason());

        ApplicationsForTheRole savedApplication = roleApplicationRepository.save(application);
        log.info("Application rejected: {}", id);

        return roleApplicationMapper.toResponseDto(savedApplication);
    }

    /**
     * Получение всех заявок с фильтрами
     */
    @Transactional(readOnly = true)
    public Page<RoleApplicationResponseDTO> getAllApplications(RoleApplicationFilterDTO filter, Pageable pageable) {
        log.info("Getting applications with filters: {}", filter);

        Page<ApplicationsForTheRole> applications = roleApplicationRepository.findAllWithFilters(
                filter.getId(),
                filter.getSectorParticipantId(),
                filter.getEventRoleId(),
                filter.getEventId(),
                filter.getStatus(),
                filter.getIsReserve(),
                filter.getDateFrom(),
                filter.getDateTo(),
                pageable);

        return applications.map(roleApplicationMapper::toResponseDto);
    }

    /**
     * Получение заявок текущего пользователя
     */
    @Transactional(readOnly = true)
    public Page<RoleApplicationResponseDTO> getMyApplications(Long userId, Pageable pageable) {
        log.info("Getting applications for user: {}", userId);

        // Получаем все sectorParticipantId пользователя
        List<Long> sectorParticipantIds = sectorParticipantRepository.findByStudentId(userId)
                .stream()
                .map(SectorParticipant::getId)
                .toList();

        if (sectorParticipantIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<ApplicationsForTheRole> applications = roleApplicationRepository.findBySectorParticipantIdIn(sectorParticipantIds, pageable);
        return applications.map(roleApplicationMapper::toResponseDto);
    }
}
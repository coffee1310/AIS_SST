package org.example.ais_sst.service.ApplicationsForTheRoleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.event_roles_application.RoleApplicationFilterDTO;
import org.example.ais_sst.dto.event_roles_application.RoleApplicationRejectDTO;
import org.example.ais_sst.dto.event_roles_application.RoleApplicationResponseDTO;
import org.example.ais_sst.entity.*;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;
import org.example.ais_sst.exception.*;
import org.example.ais_sst.mapper.RoleApplicationMapper;
import org.example.ais_sst.repository.*;
import org.example.ais_sst.specification.RoleApplicationSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.ais_sst.dto.event_roles_application.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationsForTheRoleService {

    private final ApplicationsForTheRoleRepository roleApplicationRepository;
    private final SectorParticipantRepository sectorParticipantRepository;
    private final EventRoleRepository eventRoleRepository;
    private final RoleApplicationMapper roleApplicationMapper;
    private final EventOrganizerRequestRepository eventOrganizerRequestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventOrganizerRepository eventOrganizerRepository;
    /**
     * Подача заявки на роль
     */
    @Transactional
    public RoleApplicationResponseDTO createApplication(Long eventRoleId, Long userId, String description) {
        log.info("Creating application for user: {}, eventRole: {}", userId, eventRoleId);

        // Получаем первый сектор пользователя
        List<SectorParticipant> userSectors = sectorParticipantRepository.findByStudentId(userId);

        if (userSectors.isEmpty()) {
            throw new SectorParticipantNotFoundException("Участник сектора не найден для пользователя с id: " + userId);
        }

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
                .description(description)  // НОВОЕ ПОЛЕ
                .build();

        ApplicationsForTheRole savedApplication = roleApplicationRepository.save(application);
        log.info("Application created with id: {}, isReserve: {}, description: {}",
                savedApplication.getId(), isReserve, description);

        return roleApplicationMapper.toResponseDto(savedApplication);
    }

    // Перегруженный метод для обратной совместимости
    @Transactional
    public RoleApplicationResponseDTO createApplication(Long eventRoleId, Long userId) {
        return createApplication(eventRoleId, userId, null);
    }

    @Transactional
    public EventOrganizerRequest createApplicationOrganizer(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException(String.format("Мероприятие с id %s не существует", eventId)));

        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new UserDoesNotExistException(String.format("Пользователя с id %s не существует", userId)));

        if (eventOrganizerRepository.existsByUser_IdAndEvent_Id(userId, eventId))
            throw new UserAlreadyOrganizerException("Пользователь уже является организатором мероприятия");

        if (eventOrganizerRequestRepository.existsByUser_IdAndEvent_Id(userId, eventId))
            throw new EventOrganizerRequestAlreadyExistsException("Пользователь уже подал заявку на роль организатора для этого мероприятия");

        EventOrganizerRequest request = EventOrganizerRequest.builder()
                .user(user)
                .event(event)
                .createdAt(LocalDateTime.now())
                .build();

        return eventOrganizerRequestRepository.save(request);
    }

    /**
     * Получение заявки по ID
     */
    @Transactional(readOnly = true)
    public RoleApplicationResponseDTO getApplicationById(Long id) {
        log.info("Getting application by id: {}", id);

        ApplicationsForTheRole application = roleApplicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationDoesNotExistException("Заявка не найдена с id: " + id));

        return roleApplicationMapper.toResponseDto(application);
    }

    /**
     * Одобрение заявки
     */
    @Transactional
    public RoleApplicationResponseDTO approveApplication(Long id) {
        log.info("Approving application: {}", id);

        ApplicationsForTheRole application = roleApplicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationDoesNotExistException("Заявка не найдена с id: " + id));

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

    @Transactional
    public EventOrganizerRequest approveOrganizerApplication(Long applicationId) {
        log.info("Approving organizer application: {}", applicationId);

        EventOrganizerRequest request = eventOrganizerRequestRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationDoesNotExistException("Заявка на роль организатора не найдена с id: " + applicationId));

        if (request.getStatus() != RoleApplicationStatuses.НА_РАССМОТРЕНИИ) {
            throw new IllegalStateException("Заявка уже обработана. Текущий статус: " + request.getStatus());
        }

        // Проверяем, не существует ли уже запись в event_organizers
        if (eventOrganizerRepository.existsByEventAndUser(request.getEvent(), request.getUser())) {
            throw new IllegalStateException("Пользователь уже является организатором этого мероприятия");
        }

        // Создаём запись в event_organizers
        EventOrganizer eventOrganizer = EventOrganizer.builder()
                .event(request.getEvent())
                .user(request.getUser())
                .build();

        eventOrganizerRepository.save(eventOrganizer);
        log.info("Created event organizer record for event: {}, user: {}",
                request.getEvent().getId(), request.getUser().getId());

        // Обновляем статус заявки
        request.setStatus(RoleApplicationStatuses.ОДОБРЕНА);
        EventOrganizerRequest savedRequest = eventOrganizerRequestRepository.save(request);

        log.info("Organizer application approved: {}", applicationId);
        return savedRequest;
    }

        /**
         * Отклонение заявки на роль организатора
         */
    @Transactional
    public EventOrganizerRequest rejectOrganizerApplication(Long applicationId, String rejectionReason) {
        log.info("Rejecting organizer application: {}, reason: {}", applicationId, rejectionReason);

        EventOrganizerRequest request = eventOrganizerRequestRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationDoesNotExistException("Заявка на роль организатора не найдена с id: " + applicationId));

        if (request.getStatus() != RoleApplicationStatuses.НА_РАССМОТРЕНИИ.НА_РАССМОТРЕНИИ) {
            throw new IllegalStateException("Заявка уже обработана. Текущий статус: " + request.getStatus());
        }

        request.setStatus(RoleApplicationStatuses.ОТКЛОНЕНА);
        // Если нужно сохранить причину отказа, добавьте поле rejectionReason в сущность EventOrganizerRequest

        EventOrganizerRequest savedRequest = eventOrganizerRequestRepository.save(request);

        log.info("Organizer application rejected: {}", applicationId);
        return savedRequest;
    }

    /**
     * Получение всех заявок на роль организатора для мероприятия
     */
    @Transactional(readOnly = true)
    public List<EventOrganizerRequest> getOrganizerApplicationsByEvent(Long eventId) {
        log.info("Getting organizer applications for event: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException(String.format("Мероприятие с id %s не существует", eventId)));

        return eventOrganizerRequestRepository.findByEventAndStatus(event, RoleApplicationStatuses.НА_РАССМОТРЕНИИ);
    }

    /**
     * Получение всех заявок на роль организатора от пользователя
     */
    @Transactional(readOnly = true)
    public List<EventOrganizerRequest> getOrganizerApplicationsByUser(Long userId) {
        log.info("Getting organizer applications for user: {}", userId);

        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new UserDoesNotExistException(String.format("Пользователя с id %s не существует", userId)));

        return eventOrganizerRequestRepository.findByUser(user);
    }

    /**
     * Получение заявки на роль организатора по ID
     */
    @Transactional(readOnly = true)
    public EventOrganizerRequestResponseDTO getOrganizerApplicationById(Long applicationId) {
        log.info("Getting organizer application by id: {}", applicationId);

        EventOrganizerRequest request = eventOrganizerRequestRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationDoesNotExistException("Заявка на роль организатора не найдена с id: " + applicationId));

        return roleApplicationMapper.toResponseDto(request);
    }

    /**
     * Получение заявки текущего пользователя на роль организатора для конкретного мероприятия
     */
    @Transactional(readOnly = true)
    public EventOrganizerRequest getMyOrganizerApplication(Long eventId, Long userId) {
        log.info("Getting my organizer application for user: {}, event: {}", userId, eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException(String.format("Мероприятие с id %s не существует", eventId)));

        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new UserDoesNotExistException(String.format("Пользователя с id %s не существует", userId)));

        return eventOrganizerRequestRepository.findByUserAndEvent(user, event)
                .orElseThrow(() -> new ApplicationDoesNotExistException("Заявка на роль организатора не найдена"));
    }

    /**
     * Получение заявок на роль организатора по статусу
     */
    @Transactional(readOnly = true)
    public List<EventOrganizerRequest> getOrganizerApplicationsByStatus(RoleApplicationStatuses status) {
        log.info("Getting organizer applications by status: {}", status);
        return eventOrganizerRequestRepository.findByStatus(status);
    }

    /**
     * Получение заявок текущего пользователя на роль организатора
     */
    @Transactional(readOnly = true)
    public List<EventOrganizerRequest> getMyOrganizerApplications(Long userId) {
        log.info("Getting my organizer applications for user: {}", userId);

        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new UserDoesNotExistException(String.format("Пользователя с id %s не существует", userId)));

        return eventOrganizerRequestRepository.findByUser(user);
    }

    /**
     * Отклонение заявки
     */
    @Transactional
    public RoleApplicationResponseDTO rejectApplication(Long id, RoleApplicationRejectDTO rejectDto) {
        log.info("Rejecting application: {}", id);

        ApplicationsForTheRole application = roleApplicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationDoesNotExistException("Заявка не найдена с id: " + id));

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
     * Получение заявок текущего пользователя
     */
    @Transactional(readOnly = true)
    public Page<RoleApplicationResponseDTO> getMyApplications(Long userId, Pageable pageable) {
        log.info("Getting applications for user: {}", userId);

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

    @Transactional(readOnly = true)
    public Page<RoleApplicationResponseDTO> getAllApplications(RoleApplicationFilterDTO filter, Pageable pageable) {
        log.info("Getting applications with filters: {}", filter);

        Specification<ApplicationsForTheRole> spec = RoleApplicationSpecification.withFilter(filter);

        Page<ApplicationsForTheRole> applications = roleApplicationRepository.findAll(spec, pageable);

        return applications.map(roleApplicationMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public List<EventOrganizerRequest> getOrganizerApplicationsByEventAndStatus(Long eventId, RoleApplicationStatuses status) {
        log.info("Getting organizer applications for event: {} and status: {}", eventId, status);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException(String.format("Мероприятие с id %s не существует", eventId)));

        return eventOrganizerRequestRepository.findByEventAndStatus(event, status);
    }

    /**
     * Получение моей заявки на организатора для мероприятия
     */
    @Transactional(readOnly = true)
    public EventOrganizerRequest getMyOrganizerApplicationForEvent(Long eventId, Long userId) {
        log.info("Getting my organizer application for user: {}, event: {}", userId, eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventDoesNotExistException(String.format("Мероприятие с id %s не существует", eventId)));

        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new UserDoesNotExistException(String.format("Пользователя с id %s не существует", userId)));

        return eventOrganizerRequestRepository.findByUserAndEvent(user, event)
                .orElseThrow(() -> new ApplicationDoesNotExistException("Заявка на роль организатора не найдена"));
    }

    /**
     * Фильтрация заявок на организатора
     */
    @Transactional(readOnly = true)
    public List<EventOrganizerRequest> filterOrganizerApplications(EventOrganizerRequestFilterDTO filter) {
        log.info("Filtering organizer applications with filter: {}", filter);

        Specification<EventOrganizerRequest> spec = Specification.where(null);

        if (filter.getUserId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("user").get("id"), filter.getUserId()));
        }

        if (filter.getEventId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("event").get("id"), filter.getEventId()));
        }

        if (filter.getStatus() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), filter.getStatus()));
        }

        if (filter.getDateFrom() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getDateFrom()));
        }

        if (filter.getDateTo() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("createdAt"), filter.getDateTo()));
        }

        return eventOrganizerRequestRepository.findAll(spec);
    }
}
package org.example.ais_sst.service.ApplicationsForTheRoleService;


//@Slf4j
//@Service
//@RequiredArgsConstructor
public class ApplicationsForTheRoleService {
//
//    private final ApplicationsForTheRoleRepository applicationsForTheRoleRepository;
//    private final UserRepository userRepository;
//    private final EventRoleRepository eventRoleRepository;
//    private final ApplicationsForTheRoleMapper applicationsForTheRoleMapper;
//
//    @Transactional
//    public RoleApplicationResponseDTO createRoleApplication(Long eventRoleId, Long studentId) {
//        log.info("Creating role application for student: {}, eventRole: {}", studentId, eventRoleId);
//
//        User student = userRepository.findById(studentId)
//                .orElseThrow(() -> new EntityNotFoundException("Студент не найден"));
//
//        EventRole eventRole = eventRoleRepository.findById(eventRoleId)
//                .orElseThrow(() -> new EntityNotFoundException("Роль мероприятия не найдена"));
//
//        if (applicationsForTheRoleRepository.existsByStudentIdAndEventRoleId(studentId, eventRoleId)) {
//            throw new DuplicateApplicationException("Вы уже подали заявку на эту роль");
//        }
//
//        long approvedCount = applicationsForTheRoleRepository.countApprovedByEventRoleId(eventRoleId);
//        int maxSlots = eventRole.getMaxSlots() != null ? eventRole.getMaxSlots() : Integer.MAX_VALUE;
//        boolean isReserve = approvedCount >= maxSlots;
//
//        ApplicationsForTheRole application = ApplicationsForTheRole.builder()
//                .student(student)
//                .eventRole(eventRole)
//                .isReserve(isReserve)
//                .status(RoleApplicationStatuses.PENDING)
//                .build();
//
//        ApplicationsForTheRole savedApplication = applicationsForTheRoleRepository.save(application);
//        log.info("Role application created with id: {}, isReserve: {}", savedApplication.getId(), isReserve);
//
//        return applicationsForTheRoleMapper.toResponseDto(savedApplication);
//    }
//
//    @Transactional(readOnly = true)
//    public RoleApplicationResponseDTO getRoleApplicationById(Long id) {
//        log.info("Getting role application by id: {}", id);
//
//        ApplicationsForTheRole application = applicationsForTheRoleRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Заявка не найдена"));
//
//        return applicationsForTheRoleMapper.toResponseDto(application);
//    }
//
//    @Transactional
//    public RoleApplicationResponseDTO approveRoleApplication(Long id) {
//        log.info("Approving role application: {}", id);
//
//        ApplicationsForTheRole application = applicationsForTheRoleRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Заявка не найдена"));
//
//        if (application.getStatus() != RoleApplicationStatuses.PENDING) {
//            throw new IllegalStateException("Заявка уже обработана. Текущий статус: " + application.getStatus());
//        }
//
//        long approvedCount = applicationsForTheRoleRepository.countApprovedByEventRoleId(application.getEventRole().getId());
//        int maxSlots = application.getEventRole().getMaxSlots() != null
//                ? application.getEventRole().getMaxSlots()
//                : Integer.MAX_VALUE;
//
//        if (approvedCount < maxSlots) {
//            application.setStatus(RoleApplicationStatuses.APPROVED);
//            log.info("Role application approved as regular member");
//        } else if (application.getIsReserve()) {
//            application.setStatus(RoleApplicationStatuses.APPROVED);
//            log.info("Role application approved as reserve member");
//        } else {
//            throw new IllegalStateException("Нет свободных мест для одобрения заявки");
//        }
//
//        ApplicationsForTheRole savedApplication = applicationsForTheRoleRepository.save(application);
//        return applicationsForTheRoleMapper.toResponseDto(savedApplication);
//    }
//
//    @Transactional
//    public RoleApplicationResponseDTO rejectRoleApplication(Long id, RoleApplicationRejectDTO rejectDto) {
//        log.info("Rejecting role application: {}", id);
//
//        ApplicationsForTheRole application = applicationsForTheRoleRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Заявка не найдена"));
//
//        if (application.getStatus() != RoleApplicationStatuses.PENDING) {
//            throw new IllegalStateException("Заявка уже обработана. Текущий статус: " + application.getStatus());
//        }
//
//        application.setStatus(RoleApplicationStatuses.REJECTED);
//        application.setRejectionReason(rejectDto.getRejectionReason());
//
//        ApplicationsForTheRole savedApplication = applicationsForTheRoleRepository.save(application);
//        log.info("Role application rejected: {}", id);
//
//        return applicationsForTheRoleMapper.toResponseDto(savedApplication);
//    }
//
//    @Transactional(readOnly = true)
//    public Page<RoleApplicationResponseDTO> getAllRoleApplications(RoleApplicationFilterDTO filter, Pageable pageable) {
//        log.info("Getting role applications with filters: {}", filter);
//
//        Page<ApplicationsForTheRole> applications = applicationsForTheRoleRepository.findAllWithFilters(
//                filter.getId(),
//                filter.getStudentId(),
//                filter.getEventRoleId(),
//                filter.getEventId(),
//                filter.getStatus(),
//                filter.getIsReserve(),
//                filter.getDateFrom(),
//                filter.getDateTo(),
//                pageable);
//
//        return applications.map(applicationsForTheRoleMapper::toResponseDto);
//    }
//
//    @Transactional(readOnly = true)
//    public Page<RoleApplicationResponseDTO> getMyRoleApplications(Long studentId, Pageable pageable) {
//        log.info("Getting role applications for student: {}", studentId);
//
//        Page<ApplicationsForTheRole> applications = applicationsForTheRoleRepository.findByStudentId(studentId, pageable);
//        return applications.map(applicationsForTheRoleMapper::toResponseDto);
//    }
}
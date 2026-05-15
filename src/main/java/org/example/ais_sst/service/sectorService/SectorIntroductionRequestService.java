package org.example.ais_sst.service.sectorService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTO;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTOSummary;
import org.example.ais_sst.dto.sector.SectorDTO;
import org.example.ais_sst.dto.sector.SectorParticipantDTO;
import org.example.ais_sst.entity.Sector;
import org.example.ais_sst.entity.SectorIntroductionRequest;
import org.example.ais_sst.entity.SectorParticipant;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.entity.enums.SectorIntroductionStatus;
import org.example.ais_sst.entity.enums.SectorParticipantStatuses;
import org.example.ais_sst.exception.*;
import org.example.ais_sst.mapper.SectorIntroductionRequestMapper;
import org.example.ais_sst.mapper.SectorMapper;
import org.example.ais_sst.mapper.SectorParticipantMapper;
import org.example.ais_sst.repository.SectorIntroductionRequestRepository;
import org.example.ais_sst.repository.SectorParticipantRepository;
import org.example.ais_sst.repository.SectorRepository;
import org.example.ais_sst.repository.UserRepository;
import org.example.ais_sst.service.base.BaseEntityService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectorIntroductionRequestService extends BaseEntityService {

    private final SectorIntroductionRequestMapper mapper;
    private final SectorIntroductionRequestRepository repository;
    private final UserRepository userRepository;
    private final SectorRepository sectorRepository;
    private final SectorParticipantRepository participantRepository;
    private final SectorParticipantService participantService;

    @Transactional
    public SectorIntroductionRequestDTO createRequest(Long userId, Long sectorId) {
        return executeWithLogging(() -> {
            User user = findEntityOrThrow(userId, userRepository::findUserById,
                    () -> new UserDoesNotExistException("Пользователь не найден"), "User");

            Sector sector = findEntityOrThrow(sectorId, sectorRepository::findSectorById,
                    () -> new SectorDoesNotExistException("Сектор не найден"), "Sector");

            // Проверка на активную заявку
            validateState(
                    repository.findByUserIdAndSectorIdAndStatusIn(userId, sectorId,
                            List.of(SectorIntroductionStatus.НА_РАССМОТРЕНИИ)).isEmpty(),
                    () -> new SectorIntroductionRequestAlreadyExistsException(
                            "У вас уже есть активная заявка на вступление в этот сектор"),
                    "User already has active request"
            );

            // Проверка на существующего участника
            return participantRepository.findByStudentIdAndSectorId(userId, sectorId)
                    .map(participant -> handleExistingParticipant(user, sector, participant))
                    .orElseGet(() -> createNewRequest(user, sector));

        }, "createRequest", userId, sectorId);
    }

    private SectorIntroductionRequestDTO handleExistingParticipant(User user, Sector sector, SectorParticipant participant) {
        if (participant.getStatus() == SectorParticipantStatuses.Активный) {
            throw new UserIsAlreadyInThisSectorException("Вы уже являетесь участником этого сектора");
        }

        // Восстанавливаем участника
        participant.setStatus(SectorParticipantStatuses.Активный);
        participant.setEntryDate(LocalDate.now());
        participantRepository.save(participant);

        return createNewRequest(user, sector);
    }

    private SectorIntroductionRequestDTO createNewRequest(User user, Sector sector) {
        SectorIntroductionRequest request = SectorIntroductionRequest.builder()
                .user(user)
                .sector(sector)
                .status(SectorIntroductionStatus.НА_РАССМОТРЕНИИ)
                .build();

        return mapper.toSectorIntroductionRequestDTO(repository.save(request));
    }

    @Transactional
    public SectorIntroductionRequestDTOSummary acceptRequest(Long requestId) {
        return executeWithLogging(() -> {
            SectorIntroductionRequest request = findEntityOrThrow(requestId, repository::findById,
                    () -> new SectorIntroductionRequestDoesNotExistException("Заявка не найдена"), "Request");

            validateState(request.getStatus() != SectorIntroductionStatus.ОТКЛОНЕНА,
                    () -> new SectorIntroductionRequestAlreadyProcessedException("Заявка уже обработана"),
                    "Request already rejected");

            Long userId = request.getUser().getId();
            Long sectorId = request.getSector().getId();

            // Обработка участника
            SectorParticipant participant = participantRepository
                    .findByStudentIdAndSectorId(userId, sectorId)
                    .map(this::restoreParticipant)
                    .orElseGet(() -> participantService.createParticipant(request));

            // Обновляем статус заявки
            request.setStatus(SectorIntroductionStatus.ОДОБРЕНА);
            repository.save(request);

            // Отклоняем другие активные заявки
            rejectOtherRequests(requestId, userId, sectorId);

            return mapper.toSummary(request);

        }, "acceptRequest", requestId);
    }

    private SectorParticipant restoreParticipant(SectorParticipant participant) {
        participant.setStatus(SectorParticipantStatuses.Активный);
        participant.setEntryDate(LocalDate.now());
        participant.setIsCoordinator(false);
        return participantRepository.save(participant);
    }

    private void rejectOtherRequests(Long currentRequestId, Long userId, Long sectorId) {
        List<SectorIntroductionRequest> otherRequests = repository
                .findByUserIdAndSectorIdAndStatusIn(userId, sectorId,
                        List.of(SectorIntroductionStatus.НА_РАССМОТРЕНИИ));

        otherRequests.stream()
                .filter(req -> !req.getId().equals(currentRequestId))
                .forEach(req -> {
                    req.setStatus(SectorIntroductionStatus.ОТКЛОНЕНА);
                    repository.save(req);
                    log.info("Rejected duplicate request {}", req.getId());
                });
    }

    @Transactional
    public SectorIntroductionRequestDTOSummary rejectRequest(Long requestId) {
        return executeWithLogging(() -> {
            SectorIntroductionRequest request = findEntityOrThrow(requestId, repository::findById,
                    () -> new SectorIntroductionRequestDoesNotExistException("Заявка не найдена"), "Request");

            validateState(request.getStatus() != SectorIntroductionStatus.ОДОБРЕНА,
                    () -> new SectorIntroductionRequestAlreadyProcessedException("Заявка уже обработана"),
                    "Request already approved");

            request.setStatus(SectorIntroductionStatus.ОТКЛОНЕНА);
            repository.save(request);

            return mapper.toSummary(request);

        }, "rejectRequest", requestId);
    }

    @Transactional
    public List<SectorIntroductionRequestDTO> getRequestsListByCoordinator(Long coordinatorId) {
        return executeWithLogging(() -> {
            List<SectorParticipant> coordinatorParticipants = participantRepository
                    .findSectorsWhereUserIsCoordinator(coordinatorId);

            if (coordinatorParticipants.isEmpty()) {
                log.warn("User {} is not a coordinator in any sector", coordinatorId);
                return new ArrayList<>();
            }

            return coordinatorParticipants.stream()
                    .flatMap(participant -> repository
                            .getSectorIntroductionRequestsBySector_Id(participant.getSector().getId())
                            .stream())
                    .map(mapper::toSectorIntroductionRequestDTO)
                    .toList();

        }, "getRequestsListByCoordinator", coordinatorId);
    }

    @Transactional
    public List<SectorIntroductionRequestDTO> getRequestsListByCoordinatorWithStatus(
            Long coordinatorId, SectorIntroductionStatus status) {

        return executeWithLogging(() -> {
            List<SectorParticipant> coordinatorParticipants = participantRepository
                    .findSectorsWhereUserIsCoordinator(coordinatorId);

            if (coordinatorParticipants.isEmpty()) {
                log.warn("User {} is not a coordinator in any sector", coordinatorId);
                return new ArrayList<>();
            }

            List<SectorIntroductionRequest> requests;
            if (status == null) {
                requests = repository.findRequestsByCoordinatorId(coordinatorId);
            } else {
                requests = repository.findRequestsByCoordinatorIdAndStatus(coordinatorId, status.getDbValue());
            }

            return requests.stream()
                    .map(mapper::toSectorIntroductionRequestDTO)
                    .toList();

        }, "getRequestsListByCoordinatorWithStatus", coordinatorId, status);
    }
}

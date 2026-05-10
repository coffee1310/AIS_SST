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
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectorIntroductionRequestService {

    private final SectorIntroductionRequestMapper sectorIntroductionRequestMapper;
    private final SectorIntroductionRequestRepository sectorIntroductionRequestRepository;

    private final UserRepository userRepository;
    private final SectorRepository sectorRepository;
    private final SectorParticipantRepository sectorParticipantRepository;

    private final SectorParticipantMapper sectorParticipantMapper;
    private final SectorParticipantService sectorParticipantService;

    private final SectorMapper sectorMapper;

    @Transactional
    public SectorIntroductionRequestDTO createRequest(Long userId, Long sectorId) {
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new UserDoesNotExistException(
                        String.format("Пользователь с id: %d не найден", userId)));

        Sector sector = sectorRepository.findSectorById(sectorId)
                .orElseThrow(() -> new SectorDoesNotExistException(
                        String.format("Сектор с таким id: %d не найден", sectorId)));

        // Проверяем, есть ли уже запись в sector_participants
        java.util.Optional<SectorParticipant> existingParticipant = sectorParticipantRepository
                .findByStudentIdAndSectorId(userId, sectorId);

        // Если пользователь уже является участником сектора
        if (existingParticipant.isPresent()) {
            SectorParticipant participant = existingParticipant.get();

            // Если статус "Вышедший" - можно восстановить
            if (participant.getStatus() == SectorParticipantStatuses.Вышедший) {
                // Восстанавливаем участника
                participant.setStatus(SectorParticipantStatuses.Активный);
                sectorParticipantRepository.save(participant);

                // Создаем заявку как одобренную
                SectorIntroductionRequest request = SectorIntroductionRequest.builder()
                        .user(user)
                        .sector(sector)
                        .status(SectorIntroductionStatus.ОЖИДАНИЕ)
                        .build();
                request = sectorIntroductionRequestRepository.save(request);

                log.info("User {} was restored to sector {} (was 'Вышедший')", userId, sectorId);
                return sectorIntroductionRequestMapper.toSectorIntroductionRequestDTO(request);
            }

            // Если пользователь активный участник
            throw new UserIsAlreadyInThisSectorException(
                    String.format("Пользователь с id: %d уже является активным участником сектора", userId));
        }

        // Если записи нет, создаем новую заявку
        SectorIntroductionRequest request = SectorIntroductionRequest.builder()
                .user(user)
                .sector(sector)
                .build();

        request = sectorIntroductionRequestRepository.save(request);

        return sectorIntroductionRequestMapper.toSectorIntroductionRequestDTO(request);
    }

    @Transactional
    public SectorIntroductionRequestDTOSummary acceptRequest(Long request_id) {

        SectorIntroductionRequest request = sectorIntroductionRequestRepository.findById(request_id)
                .orElseThrow(() -> new SectorIntroductionRequestDoesNotExistException(
                        String.format("Заявка на вступление в сектор с id: %d не найдена", request_id)));

        if (request.getStatus() == SectorIntroductionStatus.ОТКЛОНЕНА)
            throw new SectorIntroductionRequestAlreadyProcessedException("Заявка уже обработана!");

        Long userId = request.getUser().getId();
        Long sectorId = request.getSector().getId();

        // Проверяем, существует ли уже запись участника (ЛЮБАЯ, не только активная)
        java.util.Optional<SectorParticipant> existingParticipant = sectorParticipantRepository
                .findByStudentIdAndSectorId(userId, sectorId);

        SectorParticipant sectorParticipant;

        if (existingParticipant.isPresent()) {
            SectorParticipant participant = existingParticipant.get();

            // Если участник уже активный
            if (participant.getStatus() == SectorParticipantStatuses.Активный) {
                throw new UserIsAlreadyInThisSectorException(
                        String.format("Пользователь с id: %d уже является активным участником сектора", userId));
            }

            // Если участник не активный (Вышедший, Заблокирован и т.д.) - восстанавливаем
            log.info("Restoring user {} to sector {} from status: {}",
                    userId, sectorId, participant.getStatus());

            participant.setStatus(SectorParticipantStatuses.Активный);
            participant.setEntryDate(LocalDate.now()); // Обновляем дату вступления
            // Сбрасываем координаторство, если нужно
            if (participant.getIsCoordinator()) {
                participant.setIsCoordinator(false);
            }
            sectorParticipant = sectorParticipantRepository.save(participant);
            log.info("User {} restored to sector {} (previous status: {})",
                    userId, sectorId, participant.getStatus());

        } else {
            // Создаем нового участника
            sectorParticipant = sectorParticipantService.createParticipant(request);
            log.info("New participant created for user {} in sector {}", userId, sectorId);
        }

        SectorParticipantDTO sectorParticipantDTO = sectorParticipantMapper.toSectorParticipantDTO(sectorParticipant);

        // Обновляем статус заявки
        request.setStatus(SectorIntroductionStatus.ОДОБРЕНА);
        sectorIntroductionRequestRepository.save(request);

        // ОТКЛОНЯЕМ все другие активные заявки этого пользователя в этот сектор
        List<SectorIntroductionRequest> otherRequests = sectorIntroductionRequestRepository
                .findByUserIdAndSectorIdAndStatusIn(userId, sectorId,
                        List.of(SectorIntroductionStatus.НА_РАССМОТРЕНИИ, SectorIntroductionStatus.ОЖИДАНИЕ));

        for (SectorIntroductionRequest otherRequest : otherRequests) {
            if (!otherRequest.getId().equals(request_id)) {
                otherRequest.setStatus(SectorIntroductionStatus.ОТКЛОНЕНА);
                sectorIntroductionRequestRepository.save(otherRequest);
                log.info("Rejected duplicate request {} for user {} in sector {}",
                        otherRequest.getId(), userId, sectorId);
            }
        }

        return sectorIntroductionRequestMapper.toSummary(request);
    }

    @Transactional
    public SectorIntroductionRequestDTOSummary rejectRequest(Long request_id) {

        SectorIntroductionRequest request = sectorIntroductionRequestRepository.findById(request_id)
                .orElseThrow(() -> new SectorIntroductionRequestDoesNotExistException(
                        String.format("Заявка на вступление в сектор с id: %d не найдена", request_id)));

        if (request.getStatus() == SectorIntroductionStatus.ОДОБРЕНА)
            throw new SectorIntroductionRequestAlreadyProcessedException("Заявка уже обработана!");

        request.setStatus(SectorIntroductionStatus.ОТКЛОНЕНА);
        sectorIntroductionRequestRepository.save(request);

        return sectorIntroductionRequestMapper.toSummary(request);
    }

    @Transactional
    public List<SectorIntroductionRequestDTO> getRequestsListByCoordinator(Long coordinatorId) {
        log.info("Getting requests for coordinator id: {}", coordinatorId);

        // Используем правильный метод из репозитория
        List<SectorParticipant> coordinatorParticipants = sectorParticipantRepository
                .findSectorsWhereUserIsCoordinator(coordinatorId);

        log.info("Found {} sectors where user is coordinator", coordinatorParticipants.size());

        if (coordinatorParticipants.isEmpty()) {
            log.warn("User {} is not a coordinator in any sector", coordinatorId);
            return new ArrayList<>();
        }

        // Собираем все заявки из секторов, где пользователь координатор
        List<SectorIntroductionRequest> allRequests = new ArrayList<>();
        for (SectorParticipant participant : coordinatorParticipants) {
            List<SectorIntroductionRequest> requests = sectorIntroductionRequestRepository
                    .getSectorIntroductionRequestsBySector_Id(participant.getSector().getId());
            log.info("Sector {} has {} requests", participant.getSector().getId(), requests.size());
            allRequests.addAll(requests);
        }

        return allRequests.stream()
                .map(sectorIntroductionRequestMapper::toSectorIntroductionRequestDTO)
                .toList();

    }

    @Transactional
    public List<SectorIntroductionRequestDTO> getRequestsListByCoordinatorWithStatus(
            Long coordinatorId,
            SectorIntroductionStatus status) {

        log.info("Getting requests for coordinator id: {} with status: {}", coordinatorId, status);

        // Проверяем, является ли пользователь координатором
        List<SectorParticipant> coordinatorParticipants = sectorParticipantRepository
                .findSectorsWhereUserIsCoordinator(coordinatorId);

        if (coordinatorParticipants.isEmpty()) {
            log.warn("User {} is not a coordinator in any sector", coordinatorId);
            return new ArrayList<>();
        }

        List<SectorIntroductionRequest> requests;

        if (status == null) {
            // Если статус не указан, получаем все заявки
            requests = sectorIntroductionRequestRepository
                    .findRequestsByCoordinatorId(coordinatorId);
        } else {
            // Если статус указан, получаем заявки с фильтром
            String statusValue = status.getDbValue();
            requests = sectorIntroductionRequestRepository
                    .findRequestsByCoordinatorIdAndStatus(coordinatorId, statusValue);
        }

        log.info("Found {} requests with status: {}", requests.size(), status);

        return requests.stream()
                .map(sectorIntroductionRequestMapper::toSectorIntroductionRequestDTO)
                .toList();
    }
}

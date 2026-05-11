package org.example.ais_sst.service.sectorService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.sector.SectorDTO;
import org.example.ais_sst.dto.sector.SectorParticipantResponseDTO;
import org.example.ais_sst.dto.sector.SectorWithUserStatusDTO;
import org.example.ais_sst.entity.*;
import org.example.ais_sst.entity.enums.SectorIntroductionStatus;
import org.example.ais_sst.entity.enums.SectorParticipantStatuses;
import org.example.ais_sst.exception.SectorDoesNotExistException;
import org.example.ais_sst.exception.UserDoesNotExistException;
import org.example.ais_sst.mapper.SectorMapper;
import org.example.ais_sst.mapper.SectorParticipantMapper;
import org.example.ais_sst.mapper.converter.SectorWithUserStatusConverter;
import org.example.ais_sst.repository.*;
import org.example.ais_sst.service.userService.UserPhotoService;
import org.example.ais_sst.utils.ImageUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.management.relation.RoleNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectorService {

    private final SectorRepository sectorRepository;
    private final SectorMapper sectorMapper;
    private final SectorParticipantMapper sectorParticipantMapper;
    private final SectorWithUserStatusConverter sectorWithUserStatusConverter;

    private final UserRepository userRepository;
    private final SectorParticipantRepository sectorParticipantRepository;
    private final SectorIntroductionRequestRepository sectorIntroductionRequestRepository;
    private final RoleRepository roleRepository;
    private final UserPhotoService userPhotoService;  // Добавлен

    @Transactional
    public SectorDTO createSector(SectorDTO sectorDTO) throws RoleNotFoundException {
        log.info("Creating sector with title: {}", sectorDTO.getTitle());

        Sector sector = sectorRepository.save(sectorMapper.toEntity(sectorDTO));

        sectorDTO = sectorMapper.toSectorDTO(sector);
        log.info("Saved sector with id: {}", sectorDTO.getId());

        return sectorDTO;
    }

    @Transactional()
    public Page<SectorParticipantResponseDTO> getSectorParticipants(Long sectorId, Pageable pageable) {
        log.info("Getting participants for sector id: {}", sectorId);

        Sector sector = sectorRepository.findById(sectorId)
                .orElseThrow(() -> new SectorDoesNotExistException("Сектор с id " + sectorId + " не существует"));

        Page<SectorParticipant> participants = sectorParticipantRepository.findBySectorId(sectorId, pageable);

        // Используем маппер с UserPhotoService
        return participants.map(participant -> sectorParticipantMapper.toResponseDto(participant, userPhotoService));
    }

    @Transactional()
    public SectorParticipantResponseDTO getSectorCoordinator(Long sectorId) {
        log.info("Getting coordinator for sector id: {}", sectorId);

        SectorParticipant coordinator = sectorParticipantRepository.findBySectorIdAndIsCoordinatorTrue(sectorId)
                .orElse(null);

        if (coordinator == null) {
            return null;
        }

        return sectorParticipantMapper.toResponseDto(coordinator, userPhotoService);
    }

    @Transactional
    public SectorDTO getSectorById(Long id) {
        Sector sector = sectorRepository.findSectorById(id)
                .orElseThrow(() -> new SectorDoesNotExistException("Такой сектор не существует"));

        SectorDTO sectorDTO = sectorMapper.toSectorDTO(sector);

        SectorParticipant coordinator = sectorParticipantRepository
                .findBySectorIdAndIsCoordinatorTrue(id)
                .orElse(null);

        if (coordinator != null && coordinator.getStudent() != null) {
            User coordinatorUser = coordinator.getStudent();
            sectorDTO.setCoordinatorId(coordinatorUser.getId());
            sectorDTO.setCoordinatorName(coordinatorUser.getName());
            sectorDTO.setCoordinatorSurname(coordinatorUser.getSurname());
            sectorDTO.setCoordinatorPatronymic(coordinatorUser.getPatronymic());

            String fullName = coordinatorUser.getSurname() + " " + coordinatorUser.getName();
            if (coordinatorUser.getPatronymic() != null && !coordinatorUser.getPatronymic().isEmpty()) {
                fullName += " " + coordinatorUser.getPatronymic();
            }
            sectorDTO.setCoordinatorFullName(fullName);

            // Фото координатора - используем pathToPhoto
            if (coordinatorUser.getPathToPhoto() != null && !coordinatorUser.getPathToPhoto().isEmpty()) {
                String photoBase64 = userPhotoService.getPhotoAsBase64(coordinatorUser.getPathToPhoto());
                sectorDTO.setCoordinatorPhoto(photoBase64);
            }

            sectorDTO.setCoordinatorCourseNumber(coordinatorUser.getCourseNumber());

            if (coordinatorUser.getGroup() != null) {
                sectorDTO.setCoordinatorGroupTitle(coordinatorUser.getGroup().getTitle());
            }

            if (coordinatorUser.getSpeciality() != null) {
                sectorDTO.setCoordinatorSpecialityTitle(coordinatorUser.getSpeciality().getTitle());
            }
        }

        return sectorDTO;
    }

    @Transactional
    public List<SectorWithUserStatusDTO> getSectorsWithUserStatus(Long userId) {
        log.debug("Getting sectors with status for userId: {}", userId);

        if (!userRepository.existsById(userId)) {
            log.warn("User with id {} does not exist", userId);
            return new ArrayList<>();
        }

        List<Object[]> results = sectorRepository.findSectorsWithUserStatus(userId);
        log.debug("Query returned {} results", results.size());

        if (results == null || results.isEmpty()) {
            log.debug("No sectors found for userId: {}", userId);
            return new ArrayList<>();
        }

        // Передаем userPhotoService в конвертер
        return results.stream()
                .map(row -> sectorWithUserStatusConverter.fromNativeQuery(row, userPhotoService))
                .collect(Collectors.toList());
    }

    @Transactional
    public void addCoordinator(Long sectorId, Long userId) throws RoleNotFoundException {
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new UserDoesNotExistException(String.format("Пользователь с id: %d не найден", userId)));

        Role role = roleRepository.findByTitle("Sector_coordinator")
                .orElseThrow(() -> new RoleNotFoundException("Роль координатор не найдена"));

        user.setRole(role);
        userRepository.save(user);

        SectorParticipant participant = sectorParticipantRepository
                .findBySectorIdAndStudentId(sectorId, userId)
                .orElseThrow(() -> new UserDoesNotExistException(
                        String.format("Пользователь с id: %d не является участником сектора %d", userId, sectorId)));

        participant.setIsCoordinator(true);
        sectorParticipantRepository.save(participant);
    }

    @Transactional
    public void removeCoordinatorFromSector(Long sectorId, Long userId) throws RoleNotFoundException {
        log.info("Removing coordinator from sector: sectorId={}, userId={}", sectorId, userId);

        SectorParticipant participant = sectorParticipantRepository
                .findBySectorIdAndStudentId(sectorId, userId)
                .orElseThrow(() -> new UserDoesNotExistException(
                        String.format("Пользователь с id: %d не является участником сектора %d", userId, sectorId)));

        if (!participant.getIsCoordinator()) {
            throw new IllegalStateException(
                    String.format("Пользователь с id: %d не является координатором сектора %d", userId, sectorId));
        }

        participant.setIsCoordinator(false);
        sectorParticipantRepository.save(participant);

        log.info("Coordinator flag removed for user {} in sector {}", userId, sectorId);

        List<SectorParticipant> coordinatorEntries = sectorParticipantRepository
                .findAllByStudentIdAndIsCoordinatorTrue(userId);

        log.info("User {} is coordinator in {} other sectors", userId, coordinatorEntries.size());

        if (coordinatorEntries.isEmpty()) {
            User user = userRepository.findUserById(userId)
                    .orElseThrow(() -> new UserDoesNotExistException(
                            String.format("Пользователь с id: %d не найден", userId)));

            Role activistRole = roleRepository.findByTitle("Activist")
                    .orElseThrow(() -> new RoleNotFoundException("Роль 'Activist' не найдена"));

            user.setRole(activistRole);
            userRepository.save(user);

            log.info("User {} role changed to Activist (no longer coordinator in any sector)", userId);
        }
    }

    @Transactional
    public void kickParticipantFromSector(Long sectorId, Long coordinatorId, Long participantId) throws RoleNotFoundException {
        log.info("Kicking participant {} from sector {} by coordinator {}", participantId, sectorId, coordinatorId);

        User coordinator = userRepository.findUserById(coordinatorId)
                .orElseThrow(() -> new UserDoesNotExistException(
                        String.format("Координатор с id: %d не найден", coordinatorId)));

        SectorParticipant coordinatorParticipant = sectorParticipantRepository
                .findBySectorIdAndStudentId(sectorId, coordinatorId)
                .orElseThrow(() -> new UserDoesNotExistException(
                        String.format("Пользователь с id: %d не является участником сектора %d", coordinatorId, sectorId)));

        if (!coordinatorParticipant.getIsCoordinator()) {
            throw new SecurityException(
                    String.format("Пользователь с id: %d не является координатором сектора %d и не может выгонять участников",
                            coordinatorId, sectorId));
        }

        User participant = userRepository.findUserById(participantId)
                .orElseThrow(() -> new UserDoesNotExistException(
                        String.format("Участник с id: %d не найден", participantId)));

        SectorParticipant participantEntry = sectorParticipantRepository
                .findBySectorIdAndStudentId(sectorId, participantId)
                .orElseThrow(() -> new UserDoesNotExistException(
                        String.format("Пользователь с id: %d не является участником сектора %d", participantId, sectorId)));

        if (participantEntry.getIsCoordinator()) {
            throw new SecurityException("Нельзя выгнать координатора из сектора");
        }

        sectorParticipantRepository.delete(participantEntry);
        log.info("User {} removed from sector {}", participantId, sectorId);

        List<SectorParticipant> userSectors = sectorParticipantRepository.findByStudentId(participantId);
        if (userSectors.isEmpty()) {
            Role activistRole = roleRepository.findByTitle("Activist")
                    .orElseThrow(() -> new RoleNotFoundException("Роль 'Activist' не найдена"));
            participant.setRole(activistRole);
            userRepository.save(participant);
            log.info("User {} role changed to Activist (no longer in any sector)", participantId);
        }

        List<SectorIntroductionRequest> approvedRequests = sectorIntroductionRequestRepository
                .getSectorIntroductionRequestsBySector_IdAndStatus(sectorId, SectorIntroductionStatus.ОДОБРЕНА)
                .stream()
                .filter(req -> req.getUser().getId().equals(participantId))
                .toList();

        for (SectorIntroductionRequest request : approvedRequests) {
            request.setStatus(SectorIntroductionStatus.ВЫШЕДШИЙ);
            sectorIntroductionRequestRepository.save(request);
        }

        log.info("User {} kicked from sector {} successfully", participantId, sectorId);
    }

    @Transactional
    public void leaveSector(Long sectorId, Long userId) {
        log.info("User {} is leaving sector {}", userId, sectorId);

        SectorParticipant participant = sectorParticipantRepository
                .findByStudentIdAndSectorId(userId, sectorId)
                .orElseThrow(() -> new UserDoesNotExistException(
                        String.format("Пользователь с id: %d не является участником сектора %d", userId, sectorId)));

        if (participant.getIsCoordinator()) {
            throw new IllegalStateException(
                    String.format("Координатор сектора %d не может выйти. Сначала снимите с него полномочия координатора", sectorId));
        }

        if (participant.getStatus() == SectorParticipantStatuses.Вышедший) {
            throw new IllegalStateException(
                    String.format("Пользователь с id: %d уже покинул сектор %d", userId, sectorId));
        }

        participant.setStatus(SectorParticipantStatuses.Вышедший);
        sectorParticipantRepository.save(participant);
        log.info("User {} left sector {} with status 'Вышедший'", userId, sectorId);

        List<SectorIntroductionRequest> approvedRequests = sectorIntroductionRequestRepository
                .getSectorIntroductionRequestsBySector_IdAndStatus(sectorId, SectorIntroductionStatus.ОДОБРЕНА)
                .stream()
                .filter(req -> req.getUser().getId().equals(userId))
                .toList();

        for (SectorIntroductionRequest request : approvedRequests) {
            request.setStatus(SectorIntroductionStatus.ВЫШЕДШИЙ);
            sectorIntroductionRequestRepository.save(request);
        }

        log.info("User {} successfully left sector {}", userId, sectorId);
    }
}
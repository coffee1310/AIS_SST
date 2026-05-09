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

    @Transactional
    public SectorDTO createSector(SectorDTO sectorDTO) throws RoleNotFoundException {
        log.info("Creating sector with title: {}", sectorDTO.getTitle());

        // Создаем сектор
        Sector sector = sectorRepository.save(sectorMapper.toEntity(sectorDTO));

        log.info("Creating sector participant as coordinator for sector id: {}", sector.getId());

        // Создаем участника сектора с правами координатора
        // Нужно передать ID пользователя-координатора из другого источника
        // Например, из параметров или текущего авторизованного пользователя
        // createSectorParticipant(coordinatorId, sector.getId(), true);

        sectorDTO = sectorMapper.toSectorDTO(sector);
        log.info("Saved sector with id: {}", sectorDTO.getId());

        return sectorDTO;
    }

    @Transactional()
    public Page<SectorParticipantResponseDTO> getSectorParticipants(Long sectorId, Pageable pageable) {
        log.info("Getting participants for sector id: {}", sectorId);

        // Проверяем, существует ли сектор
        Sector sector = sectorRepository.findById(sectorId)
                .orElseThrow(() -> new SectorDoesNotExistException("Сектор с id " + sectorId + " не существует"));

        Page<SectorParticipant> participants = sectorParticipantRepository.findBySectorId(sectorId, pageable);

        return participants.map(sectorParticipantMapper::toResponseDto);
    }

    /**
     * Получить координатора сектора
     */
    @Transactional()
    public SectorParticipantResponseDTO getSectorCoordinator(Long sectorId) {
        log.info("Getting coordinator for sector id: {}", sectorId);

        SectorParticipant coordinator = sectorParticipantRepository.findBySectorIdAndIsCoordinatorTrue(sectorId)
                .orElse(null);

        if (coordinator == null) {
            return null;
        }

        return sectorParticipantMapper.toResponseDto(coordinator);
    }

    @Transactional
    public SectorDTO getSectorById(Long id) {
        Sector sector = sectorRepository.findSectorById(id)
                .orElseThrow(() -> new SectorDoesNotExistException("Такой сектор не существует"));

        SectorDTO sectorDTO = sectorMapper.toSectorDTO(sector);

        // Находим координатора сектора
        SectorParticipant coordinator = sectorParticipantRepository
                .findBySectorIdAndIsCoordinatorTrue(id)
                .orElse(null);

        if (coordinator != null && coordinator.getStudent() != null) {
            User coordinatorUser = coordinator.getStudent();
            sectorDTO.setCoordinatorId(coordinatorUser.getId());
            sectorDTO.setCoordinatorName(coordinatorUser.getName());
            sectorDTO.setCoordinatorSurname(coordinatorUser.getSurname());
            sectorDTO.setCoordinatorPatronymic(coordinatorUser.getPatronymic());

            // Формируем ФИО
            String fullName = coordinatorUser.getSurname() + " " + coordinatorUser.getName();
            if (coordinatorUser.getPatronymic() != null && !coordinatorUser.getPatronymic().isEmpty()) {
                fullName += " " + coordinatorUser.getPatronymic();
            }
            sectorDTO.setCoordinatorFullName(fullName);

            // Фото координатора
            if (coordinatorUser.getPhoto() != null && coordinatorUser.getPhoto().length > 0) {
                sectorDTO.setCoordinatorPhoto(ImageUtil.encodeToBase64(coordinatorUser.getPhoto()));
            }

            // Информация о курсе, группе и специальности
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

        return results.stream()
                .map(sectorWithUserStatusConverter::fromNativeQuery)
                .collect(Collectors.toList());
    }

    /**
     * Добавление координатора в сектор
     */
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

        // Находим запись участника в секторе
        SectorParticipant participant = sectorParticipantRepository
                .findBySectorIdAndStudentId(sectorId, userId)
                .orElseThrow(() -> new UserDoesNotExistException(
                        String.format("Пользователь с id: %d не является участником сектора %d", userId, sectorId)));

        // Проверяем, является ли он координатором
        if (!participant.getIsCoordinator()) {
            throw new IllegalStateException(
                    String.format("Пользователь с id: %d не является координатором сектора %d", userId, sectorId));
        }

        // Убираем галочку координатора
        participant.setIsCoordinator(false);
        sectorParticipantRepository.save(participant);

        log.info("Coordinator flag removed for user {} in sector {}", userId, sectorId);

        // Проверяем, не является ли пользователь координатором в других секторах
        List<SectorParticipant> coordinatorEntries = sectorParticipantRepository
                .findAllByStudentIdAndIsCoordinatorTrue(userId);

        log.info("User {} is coordinator in {} other sectors", userId, coordinatorEntries.size());

        // Если пользователь больше нигде не является координатором, меняем его роль на "Activist"
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

        // 1. Проверяем, что координатор существует
        User coordinator = userRepository.findUserById(coordinatorId)
                .orElseThrow(() -> new UserDoesNotExistException(
                        String.format("Координатор с id: %d не найден", coordinatorId)));

        // 2. Проверяем, что координатор является координатором этого сектора
        SectorParticipant coordinatorParticipant = sectorParticipantRepository
                .findBySectorIdAndStudentId(sectorId, coordinatorId)
                .orElseThrow(() -> new UserDoesNotExistException(
                        String.format("Пользователь с id: %d не является участником сектора %d", coordinatorId, sectorId)));

        if (!coordinatorParticipant.getIsCoordinator()) {
            throw new SecurityException(
                    String.format("Пользователь с id: %d не является координатором сектора %d и не может выгонять участников",
                            coordinatorId, sectorId));
        }

        // 3. Проверяем, что выгоняемый участник существует
        User participant = userRepository.findUserById(participantId)
                .orElseThrow(() -> new UserDoesNotExistException(
                        String.format("Участник с id: %d не найден", participantId)));

        // 4. Проверяем, что участник состоит в секторе
        SectorParticipant participantEntry = sectorParticipantRepository
                .findBySectorIdAndStudentId(sectorId, participantId)
                .orElseThrow(() -> new UserDoesNotExistException(
                        String.format("Пользователь с id: %d не является участником сектора %d", participantId, sectorId)));

        // 5. Нельзя выгнать координатора
        if (participantEntry.getIsCoordinator()) {
            throw new SecurityException("Нельзя выгнать координатора из сектора");
        }

        // 6. Удаляем участника из сектора
        sectorParticipantRepository.delete(participantEntry);
        log.info("User {} removed from sector {}", participantId, sectorId);

        // 7. Проверяем, не осталось ли у пользователя секторов (если нет, меняем роль на 'Activist')
        List<SectorParticipant> userSectors = sectorParticipantRepository.findByStudentId(participantId);
        if (userSectors.isEmpty()) {
            Role activistRole = roleRepository.findByTitle("Activist")
                    .orElseThrow(() -> new RoleNotFoundException("Роль 'Activist' не найдена"));
            participant.setRole(activistRole);
            userRepository.save(participant);
            log.info("User {} role changed to Activist (no longer in any sector)", participantId);
        }

        // 8. Проверяем, были ли у пользователя одобренные заявки в этот сектор и меняем их статус
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

        // Находим запись участника в секторе
        SectorParticipant participant = sectorParticipantRepository
                .findByStudentIdAndSectorId(userId, sectorId)
                .orElseThrow(() -> new UserDoesNotExistException(
                        String.format("Пользователь с id: %d не является участником сектора %d", userId, sectorId)));

        // Проверяем, не является ли пользователь координатором
        if (participant.getIsCoordinator()) {
            throw new IllegalStateException(
                    String.format("Координатор сектора %d не может выйти. Сначала снимите с него полномочия координатора", sectorId));
        }

        // Проверяем текущий статус - используем сравнение с enum
        if (participant.getStatus() == SectorParticipantStatuses.Вышедший) {
            throw new IllegalStateException(
                    String.format("Пользователь с id: %d уже покинул сектор %d", userId, sectorId));
        }

        // Обновляем статус участника на "Вышедший"
        participant.setStatus(SectorParticipantStatuses.Вышедший);
        sectorParticipantRepository.save(participant);
        log.info("User {} left sector {} with status 'Вышедший'", userId, sectorId);

        // Обновляем статус заявок
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

        // ToDo: сделать удаление чувака из сектора
    // ToDo: сделать множество координторо
    // ToDo: сделать удаление и добавление координаторов
    // ToDO: сделать кик чувака из сектора
    // ToDO: сделать фильтры по ролям пользователей

}

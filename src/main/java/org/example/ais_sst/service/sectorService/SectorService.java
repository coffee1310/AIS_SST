package org.example.ais_sst.service.sectorService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.sector.SectorDTO;
import org.example.ais_sst.dto.sector.SectorParticipantResponseDTO;
import org.example.ais_sst.dto.sector.SectorUpdateDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.management.relation.RoleNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final UserPhotoService userPhotoService;
    private final SectorPhotoService sectorPhotoService;

    @Transactional
    public SectorDTO createSector(SectorDTO sectorDTO) throws RoleNotFoundException {
        log.info("Creating sector with title: {}", sectorDTO.getTitle());

        // 1. Создаем и сохраняем сектор
        Sector savedSector = sectorRepository.save(sectorMapper.toEntity(sectorDTO));
        log.info("Sector saved with id: {}", savedSector.getId());

        // 2. Сохраняем фото (если есть)
        if (sectorDTO.getPhoto() != null && !sectorDTO.getPhoto().isEmpty()) {
            try {
                String photoPath = sectorPhotoService.savePhotoFromBase64(sectorDTO.getPhoto(), savedSector.getId());
                savedSector.setPathToPhoto(photoPath);
                savedSector = sectorRepository.save(savedSector);
                log.info("Photo saved for sector: {}", savedSector.getId());
            } catch (IOException e) {
                log.error("Failed to save photo for sector: {}", savedSector.getId(), e);
            }
        }

        // 3. Добавляем координаторов (при создании сектора - автоматически создаем участников)
        if (sectorDTO.getCoordinatorIds() != null && !sectorDTO.getCoordinatorIds().isEmpty()) {
            for (Long coordinatorId : sectorDTO.getCoordinatorIds()) {
                try {
                    addCoordinatorOnCreate(savedSector.getId(), coordinatorId);
                } catch (Exception e) {
                    log.error("Failed to add coordinator {}: {}", coordinatorId, e.getMessage());
                }
            }
        }

        // 4. Получаем обновленный сектор
        Sector refreshedSector = sectorRepository.findById(savedSector.getId()).orElse(savedSector);
        SectorDTO result = sectorMapper.toSectorDTO(refreshedSector, sectorPhotoService);

        // 5. Добавляем информацию о координаторах
        List<SectorParticipant> coordinators = sectorParticipantRepository
                .findBySectorIdAndIsCoordinatorTrue(refreshedSector.getId());

        List<Long> coordinatorIds = coordinators.stream()
                .map(participant -> participant.getStudent().getId())
                .collect(Collectors.toList());
        result.setCoordinatorIds(coordinatorIds);

        List<SectorParticipantResponseDTO> coordinatorDTOs = coordinators.stream()
                .map(coordinator -> sectorParticipantMapper.toResponseDto(coordinator, userPhotoService))
                .collect(Collectors.toList());
        result.setCoordinators(coordinatorDTOs);

        log.info("Sector {} created with {} coordinator(s)", refreshedSector.getId(), coordinatorIds.size());

        return result;
    }

    /**
     * Добавление координатора ПРИ СОЗДАНИИ сектора
     * Автоматически создает участника, если его нет
     */
    private void addCoordinatorOnCreate(Long sectorId, Long userId) throws RoleNotFoundException {
        log.debug("Adding coordinator {} to new sector {}", userId, sectorId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserDoesNotExistException("User not found: " + userId));

        Sector sector = sectorRepository.findById(sectorId)
                .orElseThrow(() -> new SectorDoesNotExistException("Sector not found: " + sectorId));

        // При создании сектора - всегда создаем нового участника-координатора
        SectorParticipant participant = SectorParticipant.builder()
                .sector(sector)
                .student(user)
                .isCoordinator(true)
                .status(SectorParticipantStatuses.Активный)
                .entryDate(LocalDate.now())
                .build();

        sectorParticipantRepository.save(participant);
        log.info("New participant-coordinator created with id: {}", participant.getId());

        // Меняем роль пользователя
        Role role = roleRepository.findByTitle("Sector_coordinator")
                .orElseThrow(() -> new RoleNotFoundException("Роль координатор не найдена"));
        user.setRole(role);
        userRepository.save(user);

        log.info("Coordinator {} successfully added to sector {}", userId, sectorId);
    }

    /**
     * Добавление координатора в СУЩЕСТВУЮЩИЙ сектор
     * Пользователь ДОЛЖЕН быть участником сектора
     */
    @Transactional
    public void addCoordinator(Long sectorId, Long userId) throws RoleNotFoundException {
        log.info("Adding coordinator to existing sector: sectorId={}, userId={}", sectorId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserDoesNotExistException("User not found: " + userId));

        // Проверяем, является ли пользователь участником сектора
        SectorParticipant participant = sectorParticipantRepository
                .findBySectorIdAndStudentId(sectorId, userId)
                .orElseThrow(() -> new UserDoesNotExistException(
                        String.format("Пользователь с id: %d не является участником сектора %d. Сначала вступите в сектор.",
                                userId, sectorId)));

        // Проверяем, не является ли пользователь уже координатором
        if (participant.getIsCoordinator()) {
            throw new IllegalStateException(
                    String.format("Пользователь с id: %d уже является координатором сектора %d", userId, sectorId));
        }

        // Проверяем статус участника
        if (participant.getStatus() != SectorParticipantStatuses.Активный) {
            throw new IllegalStateException(
                    String.format("Пользователь с id: %d не является активным участником сектора %d. Статус: %s",
                            userId, sectorId, participant.getStatus()));
        }

        // Назначаем координатором
        participant.setIsCoordinator(true);
        sectorParticipantRepository.save(participant);

        // Меняем роль пользователя
        Role role = roleRepository.findByTitle("Sector_coordinator")
                .orElseThrow(() -> new RoleNotFoundException("Роль координатор не найдена"));
        user.setRole(role);
        userRepository.save(user);

        log.info("User {} became coordinator of sector {}", userId, sectorId);
    }

    @Transactional()
    public Page<SectorParticipantResponseDTO> getSectorParticipants(Long sectorId, Pageable pageable) {
        log.info("Getting participants for sector id: {}", sectorId);

        Sector sector = sectorRepository.findById(sectorId)
                .orElseThrow(() -> new SectorDoesNotExistException("Сектор с id " + sectorId + " не существует"));

        Page<SectorParticipant> participants = sectorParticipantRepository.findBySectorId(sectorId, pageable);

        return participants.map(participant -> sectorParticipantMapper.toResponseDto(participant, userPhotoService));
    }

    @Transactional()
    public SectorParticipantResponseDTO getSectorCoordinator(Long sectorId) {
        log.info("Getting coordinator for sector id: {}", sectorId);

        return sectorParticipantRepository
                .findBySectorIdAndIsCoordinatorTrue(sectorId)
                .stream()
                .findFirst()
                .map(coordinator -> sectorParticipantMapper.toResponseDto(coordinator, userPhotoService))
                .orElse(null);
    }

    @Transactional
    public SectorDTO getSectorById(Long id) {
        Sector sector = sectorRepository.findSectorById(id)
                .orElseThrow(() -> new SectorDoesNotExistException("Такой сектор не существует"));

        SectorDTO sectorDTO = sectorMapper.toSectorDTO(sector, sectorPhotoService);

        // Получаем фото сектора
        if (sector.getPathToPhoto() != null && !sector.getPathToPhoto().isEmpty()) {
            String photoBase64 = sectorPhotoService.getPhotoAsBase64(sector.getPathToPhoto());
            sectorDTO.setPhoto(photoBase64);
        }

        // Получаем всех координаторов сектора
        List<SectorParticipant> coordinators = sectorParticipantRepository
                .findBySectorIdAndIsCoordinatorTrue(id);

        List<Long> coordinatorIds = coordinators.stream()
                .map(c -> c.getStudent().getId())
                .collect(Collectors.toList());
        sectorDTO.setCoordinatorIds(coordinatorIds);

        List<SectorParticipantResponseDTO> coordinatorDTOs = coordinators.stream()
                .map(coordinator -> sectorParticipantMapper.toResponseDto(coordinator, userPhotoService))
                .collect(Collectors.toList());
        sectorDTO.setCoordinators(coordinatorDTOs);

        log.info("Sector {} has {} coordinator(s)", id, coordinatorDTOs.size());

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

        // Убираем дубликаты по ID сектора
        Map<Long, SectorWithUserStatusDTO> sectorMap = new LinkedHashMap<>();

        for (Object[] row : results) {
            Long sectorId = ((Number) row[0]).longValue();

            if (!sectorMap.containsKey(sectorId)) {
                SectorWithUserStatusDTO sector = sectorWithUserStatusConverter.fromNativeQuery(row, userPhotoService);
                sectorMap.put(sectorId, sector);
            }
        }

        List<SectorWithUserStatusDTO> sectors = new ArrayList<>(sectorMap.values());

        // Для каждого сектора загружаем список всех координаторов
        for (SectorWithUserStatusDTO sector : sectors) {
            List<SectorParticipant> coordinators = sectorParticipantRepository
                    .findBySectorIdAndIsCoordinatorTrue(sector.getId());

            List<SectorParticipantResponseDTO> coordinatorDTOs = coordinators.stream()
                    .map(coordinator -> sectorParticipantMapper.toResponseDto(coordinator, userPhotoService))
                    .collect(Collectors.toList());

            sector.setCoordinators(coordinatorDTOs);
        }

        log.info("Found {} unique sectors for user {}", sectors.size(), userId);

        return sectors;

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
            User user = userRepository.findById(userId)
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
    public void kickParticipantFromSector(Long sectorId, Long coordinatorId, Long participantId) {
        log.info("Kicking participant {} from sector {} by coordinator {}", participantId, sectorId, coordinatorId);

        SectorParticipant coordinatorParticipant = sectorParticipantRepository
                .findBySectorIdAndStudentId(sectorId, coordinatorId)
                .orElseThrow(() -> new UserDoesNotExistException(
                        String.format("Пользователь с id: %d не является участником сектора %d", coordinatorId, sectorId)));

        if (!coordinatorParticipant.getIsCoordinator()) {
            throw new SecurityException(
                    String.format("Пользователь с id: %d не является координатором сектора %d и не может выгонять участников",
                            coordinatorId, sectorId));
        }

        SectorParticipant participantEntry = sectorParticipantRepository
                .findBySectorIdAndStudentId(sectorId, participantId)
                .orElseThrow(() -> new UserDoesNotExistException(
                        String.format("Пользователь с id: %d не является участником сектора %d", participantId, sectorId)));

        if (participantEntry.getIsCoordinator()) {
            throw new SecurityException("Нельзя выгнать координатора из сектора");
        }

        // Вместо удаления - меняем статус на "Вышедший"
        participantEntry.setStatus(SectorParticipantStatuses.Вышедший);
        participantEntry.setIsCoordinator(false); // Снимаем флаг координатора, если был
        sectorParticipantRepository.save(participantEntry);

        log.info("User {} status changed to 'Вышедший' in sector {}", participantId, sectorId);

        log.info("User {} kicked from sector {} successfully (status: Вышедший)", participantId, sectorId);
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

    @Transactional
    public SectorDTO updateSector(SectorUpdateDTO updateDTO) throws RoleNotFoundException {
        log.info("Updating sector with id: {}", updateDTO.getId());

        // 1. Находим существующий сектор
        Sector existingSector = sectorRepository.findById(updateDTO.getId())
                .orElseThrow(() -> new SectorDoesNotExistException("Сектор с id " + updateDTO.getId() + " не существует"));

        // 2. Обновляем основные поля
        if (updateDTO.getTitle() != null) {
            existingSector.setTitle(updateDTO.getTitle());
        }
        if (updateDTO.getDescription() != null) {
            existingSector.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getIsActive() != null) {
            existingSector.setIsActive(updateDTO.getIsActive());
        }

        // 3. Обновляем фото (если передано новое)
        if (updateDTO.getPhoto() != null && !updateDTO.getPhoto().isEmpty()) {
            try {
                if (existingSector.getPathToPhoto() != null && !existingSector.getPathToPhoto().isEmpty()) {
                    sectorPhotoService.deletePhoto(existingSector.getPathToPhoto());
                    log.info("Old photo deleted for sector: {}", updateDTO.getId());
                }
                String photoPath = sectorPhotoService.savePhotoFromBase64(updateDTO.getPhoto(), updateDTO.getId());
                existingSector.setPathToPhoto(photoPath);
                log.info("New photo saved for sector: {}", updateDTO.getId());
            } catch (IOException e) {
                log.error("Failed to save photo for sector: {}", updateDTO.getId(), e);
                throw new RuntimeException("Ошибка при сохранении фото сектора", e);
            }
        }

        // 4. Сохраняем обновленный сектор
        Sector updatedSector = sectorRepository.save(existingSector);
        log.info("Sector updated with id: {}", updatedSector.getId());

        // 5. Обновляем координаторов (если передан новый список)
        if (updateDTO.getCoordinatorIds() != null) {
            updateCoordinators(updateDTO.getId(), updateDTO.getCoordinatorIds());
        }

        // 6. Получаем обновленный DTO
        SectorDTO result = sectorMapper.toSectorDTO(updatedSector, sectorPhotoService);

        // 7. Добавляем фото в DTO
        if (updatedSector.getPathToPhoto() != null && !updatedSector.getPathToPhoto().isEmpty()) {
            String photoBase64 = sectorPhotoService.getPhotoAsBase64(updatedSector.getPathToPhoto());
            result.setPhoto(photoBase64);
        }

        // 8. Добавляем информацию о координаторах
        List<SectorParticipant> coordinators = sectorParticipantRepository
                .findBySectorIdAndIsCoordinatorTrue(updateDTO.getId());

        List<Long> coordinatorIds = coordinators.stream()
                .map(participant -> participant.getStudent().getId())
                .collect(Collectors.toList());
        result.setCoordinatorIds(coordinatorIds);

        List<SectorParticipantResponseDTO> coordinatorDTOs = coordinators.stream()
                .map(coordinator -> sectorParticipantMapper.toResponseDto(coordinator, userPhotoService))
                .collect(Collectors.toList());
        result.setCoordinators(coordinatorDTOs);

        log.info("Sector {} updated with {} coordinator(s)", updateDTO.getId(), coordinatorIds.size());

        return result;
    }

    @Transactional
    public void updateCoordinators(Long sectorId, List<Long> newCoordinatorIds) throws RoleNotFoundException {
        log.info("Updating coordinators for sector {}: new coordinators: {}", sectorId, newCoordinatorIds);

        // 1. Получаем текущих координаторов
        List<SectorParticipant> currentCoordinators = sectorParticipantRepository
                .findBySectorIdAndIsCoordinatorTrue(sectorId);

        List<Long> currentCoordinatorIds = currentCoordinators.stream()
                .map(c -> c.getStudent().getId())
                .collect(Collectors.toList());

        // 2. Определяем, кого нужно удалить из координаторов
        List<Long> toRemove = currentCoordinatorIds.stream()
                .filter(id -> !newCoordinatorIds.contains(id))
                .collect(Collectors.toList());

        // 3. Определяем, кого нужно добавить в координаторы
        List<Long> toAdd = newCoordinatorIds.stream()
                .filter(id -> !currentCoordinatorIds.contains(id))
                .collect(Collectors.toList());

        // 4. Удаляем координаторов
        for (Long userId : toRemove) {
            removeCoordinatorFromSector(sectorId, userId);
            log.info("Coordinator {} removed from sector {}", userId, sectorId);
        }

        // 5. Добавляем новых координаторов
        for (Long userId : toAdd) {
            addCoordinator(sectorId, userId);
            log.info("Coordinator {} added to sector {}", userId, sectorId);
        }

        log.info("Coordinators updated for sector {}: removed {}, added {}",
                sectorId, toRemove.size(), toAdd.size());
    }

    @Transactional
    public void deactivateSector(Long id) {
        sectorRepository.findById(id)
                        .orElseThrow(() -> new SectorDoesNotExistException(String.format("Сектор с id: %s не существует", id)));

        sectorRepository.deactivateSector(id);
    }

    @Transactional
    public void activateSector(Long id) {
        sectorRepository.findById(id)
                .orElseThrow(() -> new SectorDoesNotExistException(String.format("Сектор с id: %s не существует", id)));

        sectorRepository.activateSector(id);
    }
}
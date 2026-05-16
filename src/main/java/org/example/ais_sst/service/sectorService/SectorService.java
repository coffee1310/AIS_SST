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
import org.springframework.scheduling.annotation.Scheduled;
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
    private final SectorCacheService sectorCacheService;

    @Transactional
    public SectorDTO createSector(SectorDTO sectorDTO) throws RoleNotFoundException {
        log.info("Creating sector with title: {}", sectorDTO.getTitle());

        // Создаем и сохраняем сектор
        Sector savedSector = sectorRepository.save(sectorMapper.toEntity(sectorDTO));
        log.info("Sector saved with id: {}", savedSector.getId());

        // Сохраняем фото (если есть)
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

        // Добавляем координаторов
        if (sectorDTO.getCoordinatorIds() != null && !sectorDTO.getCoordinatorIds().isEmpty()) {
            for (Long coordinatorId : sectorDTO.getCoordinatorIds()) {
                try {
                    addCoordinatorOnCreate(savedSector.getId(), coordinatorId);
                } catch (Exception e) {
                    log.error("Failed to add coordinator {}: {}", coordinatorId, e.getMessage());
                }
            }
        }

        // Получаем обновленный сектор
        Sector refreshedSector = sectorRepository.findById(savedSector.getId()).orElse(savedSector);
        SectorDTO result = buildSectorDTO(refreshedSector);

        // Инвалидируем кэш секторов
        sectorCacheService.invalidateAllSectorCache();
        log.info("Sector cache invalidated after creation");

        log.info("Sector {} created with {} coordinator(s)", refreshedSector.getId(),
                result.getCoordinatorIds() != null ? result.getCoordinatorIds().size() : 0);

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

    @Transactional()
    public SectorDTO getSectorById(Long id) {
        // Пытаемся получить из кэша
        java.util.Optional<SectorDTO> cached = sectorCacheService.getSectorById(id);
        if (cached.isPresent()) {
            log.debug("Returning sector {} from cache", id);
            return cached.get();
        }

        log.debug("Sector {} not in cache, loading from database", id);

        Sector sector = sectorRepository.findSectorById(id)
                .orElseThrow(() -> new SectorDoesNotExistException("Такой сектор не существует"));

        SectorDTO result = buildSectorDTO(sector);

        // Сохраняем в кэш
        sectorCacheService.cacheSector(result);

        return result;
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

        Sector existingSector = sectorRepository.findById(updateDTO.getId())
                .orElseThrow(() -> new SectorDoesNotExistException("Сектор с id " + updateDTO.getId() + " не существует"));

        // Обновляем основные поля
        if (updateDTO.getTitle() != null) existingSector.setTitle(updateDTO.getTitle());
        if (updateDTO.getDescription() != null) existingSector.setDescription(updateDTO.getDescription());
        if (updateDTO.getIsActive() != null) existingSector.setIsActive(updateDTO.getIsActive());

        // Обновляем фото
        if (updateDTO.getPhoto() != null && !updateDTO.getPhoto().isEmpty()) {
            try {
                if (existingSector.getPathToPhoto() != null && !existingSector.getPathToPhoto().isEmpty()) {
                    sectorPhotoService.deletePhoto(existingSector.getPathToPhoto());
                }
                String photoPath = sectorPhotoService.savePhotoFromBase64(updateDTO.getPhoto(), updateDTO.getId());
                existingSector.setPathToPhoto(photoPath);
            } catch (IOException e) {
                log.error("Failed to save photo for sector: {}", updateDTO.getId(), e);
                throw new RuntimeException("Ошибка при сохранении фото сектора", e);
            }
        }

        Sector updatedSector = sectorRepository.save(existingSector);

        // Обновляем координаторов
        if (updateDTO.getCoordinatorIds() != null) {
            updateCoordinators(updateDTO.getId(), updateDTO.getCoordinatorIds());
        }

        SectorDTO result = buildSectorDTO(updatedSector);

        // Инвалидируем кэш сектора
        sectorCacheService.invalidateSector(updateDTO.getId());
        log.info("Sector {} cache invalidated after update", updateDTO.getId());

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


    @Transactional()
    public List<SectorDTO> getAllSectors() {
        // Пытаемся получить из кэша
        java.util.Optional<List<SectorDTO>> cached = sectorCacheService.getAllSectors();
        if (cached.isPresent()) {
            log.debug("Returning all sectors from cache");
            return cached.get();
        }

        log.debug("All sectors not in cache, loading from database");

        List<Sector> sectors = sectorRepository.findAll();
        List<SectorDTO> result = sectors.stream()
                .map(this::buildSectorDTO)
                .collect(Collectors.toList());

        // Сохраняем в кэш
        sectorCacheService.cacheAllSectors(result);

        return result;
    }

    @Transactional()
    public List<SectorDTO> getActiveSectors() {
        // Пытаемся получить из кэша
        java.util.Optional<List<SectorDTO>> cached = sectorCacheService.getActiveSectors();
        if (cached.isPresent()) {
            log.debug("Returning active sectors from cache");
            return cached.get();
        }

        log.debug("Active sectors not in cache, loading from database");

        List<Sector> sectors = sectorRepository.findByIsActiveTrue();
        List<SectorDTO> result = sectors.stream()
                .map(this::buildSectorDTO)
                .collect(Collectors.toList());

        // Сохраняем в кэш
        sectorCacheService.cacheActiveSectors(result);

        return result;
    }

    @Transactional()
    public List<SectorWithUserStatusDTO> getSectorsWithUserStatus(Long userId) {
        // Этот метод не кэшируем, так как зависит от пользователя
        log.debug("Getting sectors with status for userId: {}", userId);

        if (!userRepository.existsById(userId)) {
            log.warn("User with id {} does not exist", userId);
            return new ArrayList<>();
        }

        List<Object[]> results = sectorRepository.findSectorsWithUserStatus(userId);

        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, SectorWithUserStatusDTO> sectorMap = new LinkedHashMap<>();
        for (Object[] row : results) {
            Long sectorId = ((Number) row[0]).longValue();
            if (!sectorMap.containsKey(sectorId)) {
                SectorWithUserStatusDTO sector = sectorWithUserStatusConverter.fromNativeQuery(row, userPhotoService);
                sectorMap.put(sectorId, sector);
            }
        }

        List<SectorWithUserStatusDTO> sectors = new ArrayList<>(sectorMap.values());

        for (SectorWithUserStatusDTO sector : sectors) {
            List<SectorParticipant> coordinators = sectorParticipantRepository
                    .findBySectorIdAndIsCoordinatorTrue(sector.getId());
            List<SectorParticipantResponseDTO> coordinatorDTOs = coordinators.stream()
                    .map(coordinator -> sectorParticipantMapper.toResponseDto(coordinator, userPhotoService))
                    .collect(Collectors.toList());
            sector.setCoordinators(coordinatorDTOs);
        }

        return sectors;
    }


    @Transactional
    public void activateSector(Long id) {
        sectorRepository.findById(id)
                .orElseThrow(() -> new SectorDoesNotExistException(String.format("Сектор с id: %s не существует", id)));
        sectorRepository.activateSector(id);

        // Инвалидируем кэш
        sectorCacheService.invalidateSector(id);
        log.info("Sector {} activated and cache invalidated", id);
    }

    @Transactional
    public void deleteSector(Long id) {
        Sector sector = sectorRepository.findById(id)
                .orElseThrow(() -> new SectorDoesNotExistException("Сектор не найден"));
        sectorRepository.delete(sector);

        // Инвалидируем кэш
        sectorCacheService.invalidateSector(id);
        log.info("Sector {} deleted and cache invalidated", id);
    }

    // Вспомогательные методы
    private SectorDTO buildSectorDTO(Sector sector) {
        SectorDTO sectorDTO = sectorMapper.toSectorDTO(sector, sectorPhotoService);

        if (sector.getPathToPhoto() != null && !sector.getPathToPhoto().isEmpty()) {
            String photoBase64 = sectorPhotoService.getPhotoAsBase64(sector.getPathToPhoto());
            sectorDTO.setPhoto(photoBase64);
        }

        List<SectorParticipant> coordinators = sectorParticipantRepository
                .findBySectorIdAndIsCoordinatorTrue(sector.getId());

        List<Long> coordinatorIds = coordinators.stream()
                .map(c -> c.getStudent().getId())
                .collect(Collectors.toList());
        sectorDTO.setCoordinatorIds(coordinatorIds);

        List<SectorParticipantResponseDTO> coordinatorDTOs = coordinators.stream()
                .map(coordinator -> sectorParticipantMapper.toResponseDto(coordinator, userPhotoService))
                .collect(Collectors.toList());
        sectorDTO.setCoordinators(coordinatorDTOs);

        return sectorDTO;
    }

    // Периодическое обновление кэша (каждый час)
    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void refreshSectorCache() {
        log.info("Refreshing sector cache...");
        List<Sector> sectors = sectorRepository.findAll();
        List<SectorDTO> sectorDTOs = sectors.stream()
                .map(this::buildSectorDTO)
                .collect(Collectors.toList());

        sectorCacheService.cacheAllSectors(sectorDTOs);

        List<Sector> activeSectors = sectorRepository.findByIsActiveTrue();
        List<SectorDTO> activeSectorDTOs = activeSectors.stream()
                .map(this::buildSectorDTO)
                .collect(Collectors.toList());

        sectorCacheService.cacheActiveSectors(activeSectorDTOs);

        log.info("Sector cache refreshed: {} total, {} active", sectorDTOs.size(), activeSectorDTOs.size());
    }

    @Transactional()
    public List<SectorDTO> getAllSectorsWithoutCache() {
        log.debug("Loading all sectors from database (cache bypassed)");

        List<Sector> sectors = sectorRepository.findAll();
        return sectors.stream()
                .map(this::buildSectorDTO)
                .collect(Collectors.toList());
    }
}
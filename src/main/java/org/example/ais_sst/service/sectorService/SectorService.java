package org.example.ais_sst.service.sectorService;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.management.relation.RoleNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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

    @Value("${app.cache.sector.ttl:3600}")
    private long sectorCacheTtlSeconds;

    // Локальный кэш для фото (Caffeine)
    private final Cache<Long, String> photoCache = Caffeine.newBuilder()
            .maximumSize(200)
            .expireAfterWrite(24, TimeUnit.HOURS)
            .recordStats()
            .build();

    // Локальный кэш для списка секторов
    private final Cache<String, List<SectorDTO>> sectorListCache = Caffeine.newBuilder()
            .maximumSize(10)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats()
            .build();

    // ==================== CACHED METHODS ====================

    @Cacheable(value = "sector", key = "#id", unless = "#result == null")
    @Transactional(readOnly = true)
    public SectorDTO getSectorById(Long id) {
        log.info("🟡 Loading sector {} from DATABASE (cache miss)", id);

        long startTime = System.currentTimeMillis();

        Sector sector = sectorRepository.findSectorById(id)
                .orElseThrow(() -> new SectorDoesNotExistException("Сектор с id " + id + " не существует"));

        SectorDTO result = buildSectorDTO(sector);

        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ Sector {} loaded in {} ms", id, duration);

        return result;
    }

    @Transactional(readOnly = true)
    public List<SectorDTO> getAllSectors() {
        // Проверяем локальный кэш
        List<SectorDTO> cached = sectorListCache.getIfPresent("all");
        if (cached != null) {
            log.info("🟢 Returning ALL sectors from Caffeine cache (size: {})", cached.size());
            return cached;
        }

        log.info("🟡 Loading ALL sectors from DATABASE (cache miss)");
        long startTime = System.currentTimeMillis();

        List<Sector> sectors = sectorRepository.findAll();
        List<SectorDTO> result = sectors.stream()
                .parallel()  // Параллельная обработка для ускорения
                .map(this::buildSectorDTO)
                .collect(Collectors.toList());

        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ Loaded {} sectors from DB in {} ms", result.size(), duration);

        // Сохраняем в локальный кэш
        sectorListCache.put("all", result);
        sectorListCache.put("active", result.stream()
                .filter(SectorDTO::getIsActive)
                .collect(Collectors.toList()));

        return result;
    }

    @Transactional(readOnly = true)
    public List<SectorDTO> getActiveSectors() {
        List<SectorDTO> cached = sectorListCache.getIfPresent("active");
        if (cached != null) {
            log.info("🟢 Returning ACTIVE sectors from Caffeine cache (size: {})", cached.size());
            return cached;
        }

        log.info("🟡 Loading ACTIVE sectors from DATABASE (cache miss)");

        List<Sector> sectors = sectorRepository.findByIsActiveTrue();
        List<SectorDTO> result = sectors.stream()
                .map(this::buildSectorDTO)
                .collect(Collectors.toList());

        sectorListCache.put("active", result);

        return result;
    }

    // ==================== CACHE EVICTION ====================

    @CacheEvict(value = {"sector", "allSectors", "activeSectors"}, allEntries = true)
    @Transactional
    public SectorDTO createSector(SectorDTO sectorDTO) throws RoleNotFoundException {
        log.info("🟢 Creating sector '{}' - clearing all caches", sectorDTO.getTitle());

        long startTime = System.currentTimeMillis();

        Sector savedSector = sectorRepository.save(sectorMapper.toEntity(sectorDTO));
        log.info("Sector saved with id: {}", savedSector.getId());

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

        if (sectorDTO.getCoordinatorIds() != null && !sectorDTO.getCoordinatorIds().isEmpty()) {
            for (Long coordinatorId : sectorDTO.getCoordinatorIds()) {
                try {
                    addCoordinatorOnCreate(savedSector.getId(), coordinatorId);
                } catch (Exception e) {
                    log.error("Failed to add coordinator {}: {}", coordinatorId, e.getMessage());
                }
            }
        }

        // Инвалидируем локальные кэши
        sectorListCache.invalidateAll();
        photoCache.invalidateAll();

        SectorDTO result = buildSectorDTO(savedSector);

        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ Sector created in {} ms", duration);

        return result;
    }

    @CacheEvict(value = {"sector", "allSectors", "activeSectors"}, allEntries = true)
    @Transactional
    public SectorDTO updateSector(SectorUpdateDTO updateDTO) throws RoleNotFoundException {
        log.info("🟢 Updating sector {} - clearing all caches", updateDTO.getId());

        long startTime = System.currentTimeMillis();

        Sector existingSector = sectorRepository.findById(updateDTO.getId())
                .orElseThrow(() -> new SectorDoesNotExistException("Сектор с id " + updateDTO.getId() + " не существует"));

        if (updateDTO.getTitle() != null) existingSector.setTitle(updateDTO.getTitle());
        if (updateDTO.getDescription() != null) existingSector.setDescription(updateDTO.getDescription());
        if (updateDTO.getIsActive() != null) existingSector.setIsActive(updateDTO.getIsActive());

        if (updateDTO.getPhoto() != null && !updateDTO.getPhoto().isEmpty()) {
            try {
                if (existingSector.getPathToPhoto() != null && !existingSector.getPathToPhoto().isEmpty()) {
                    sectorPhotoService.deletePhoto(existingSector.getPathToPhoto());
                    // Удаляем фото из кэша
                    photoCache.invalidate(updateDTO.getId());
                }
                String photoPath = sectorPhotoService.savePhotoFromBase64(updateDTO.getPhoto(), updateDTO.getId());
                existingSector.setPathToPhoto(photoPath);
            } catch (IOException e) {
                log.error("Failed to save photo for sector: {}", updateDTO.getId(), e);
                throw new RuntimeException("Ошибка при сохранении фото сектора", e);
            }
        }

        Sector updatedSector = sectorRepository.save(existingSector);

        if (updateDTO.getCoordinatorIds() != null) {
            updateCoordinators(updateDTO.getId(), updateDTO.getCoordinatorIds());
        }

        // Инвалидируем кэши
        sectorListCache.invalidateAll();

        SectorDTO result = buildSectorDTO(updatedSector);

        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ Sector {} updated in {} ms", updateDTO.getId(), duration);

        return result;
    }

    @CacheEvict(value = {"sector", "allSectors", "activeSectors"}, allEntries = true)
    @Transactional
    public void deleteSector(Long id) {
        log.info("🟢 Deleting sector {} - clearing all caches", id);

        Sector sector = sectorRepository.findById(id)
                .orElseThrow(() -> new SectorDoesNotExistException("Сектор не найден"));
        sectorRepository.delete(sector);

        // Очищаем кэши
        sectorListCache.invalidateAll();
        photoCache.invalidate(id);

        log.info("✅ Sector {} deleted", id);
    }

    @CacheEvict(value = {"sector", "allSectors", "activeSectors"}, allEntries = true)
    @Transactional
    public void deactivateSector(Long id) throws RoleNotFoundException {
        log.info("Deactivating sector {}", id);

        // Проверяем существование сектора
        Sector sector = sectorRepository.findById(id)
                .orElseThrow(() -> new SectorDoesNotExistException("Сектор с id " + id + " не существует"));

        // Деактивируем сектор
        sectorRepository.deactivateSector(id);

        // Обновляем статус всех участников сектора
        List<SectorParticipant> sectorParticipants = sectorParticipantRepository.findBySectorId(id);
        if (!sectorParticipants.isEmpty()) {
            sectorParticipants.forEach(participant ->
                    participant.setStatus(SectorParticipantStatuses.Вышедший)
            );
            sectorParticipantRepository.saveAll(sectorParticipants);
        }

        // Снимаем роль координатора у всех координаторов сектора
        List<SectorParticipant> coordinators = sectorParticipantRepository.findBySectorIdAndIsCoordinator(id, true);
        if (!coordinators.isEmpty()) {
            Role activistRole = roleRepository.findByTitle("Activist")
                    .orElseThrow(() -> new RoleNotFoundException("Роль 'Активист' не найдена"));

            coordinators.forEach(coordinator -> {
                coordinator.setIsCoordinator(false);
                User user = coordinator.getStudent();
                user.setRole(activistRole);
                sectorParticipantRepository.save(coordinator);
                userRepository.save(user);
            });
        }

        log.info("Sector {} deactivated successfully", id);
    }

    @CacheEvict(value = {"sector", "allSectors", "activeSectors"}, allEntries = true)
    @Transactional
    public void activateSector(Long id) {
        log.info("🟢 Activating sector {} - clearing caches", id);

        sectorRepository.findById(id)
                .orElseThrow(() -> new SectorDoesNotExistException("Сектор с id " + id + " не существует"));
        sectorRepository.activateSector(id);

        sectorListCache.invalidateAll();

        log.info("✅ Sector {} activated", id);
    }

    // ==================== NON-CACHED METHODS ====================

    @Transactional(readOnly = true)
    public List<SectorWithUserStatusDTO> getSectorsWithUserStatus(Long userId) {
        log.debug("Getting sectors with status for userId: {} (not cached)", userId);

        if (!userRepository.existsById(userId)) {
            log.warn("User with id {} does not exist", userId);
            return new ArrayList<>();
        }

        long startTime = System.currentTimeMillis();

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

        long duration = System.currentTimeMillis() - startTime;
        log.debug("✅ Loaded {} sectors for user {} in {} ms", sectors.size(), userId, duration);

        return sectors;
    }

    @Transactional(readOnly = true)
    public Page<SectorParticipantResponseDTO> getSectorParticipants(Long sectorId, Pageable pageable) {
        log.info("Getting participants for sector id: {}", sectorId);

        Sector sector = sectorRepository.findById(sectorId)
                .orElseThrow(() -> new SectorDoesNotExistException("Сектор с id " + sectorId + " не существует"));

        Page<SectorParticipant> participants = sectorParticipantRepository.findBySectorId(sectorId, pageable);

        return participants.map(participant -> sectorParticipantMapper.toResponseDto(participant, userPhotoService));
    }

    @Transactional(readOnly = true)
    public SectorParticipantResponseDTO getSectorCoordinator(Long sectorId) {
        log.info("Getting coordinator for sector id: {}", sectorId);

        return sectorParticipantRepository
                .findBySectorIdAndIsCoordinatorTrue(sectorId)
                .stream()
                .findFirst()
                .map(coordinator -> sectorParticipantMapper.toResponseDto(coordinator, userPhotoService))
                .orElse(null);
    }

    // ==================== HELPER METHODS ====================

    private SectorDTO buildSectorDTO(Sector sector) {
        long startTime = System.currentTimeMillis();

        SectorDTO sectorDTO = sectorMapper.toSectorDTO(sector, null);

        // Загружаем фото из кэша или БД
        if (sector.getPathToPhoto() != null && !sector.getPathToPhoto().isEmpty()) {
            String photo = photoCache.get(sector.getId(), id -> {
                log.debug("Loading photo for sector {} from disk", id);
                return sectorPhotoService.getPhotoAsBase64(sector.getPathToPhoto());
            });
            sectorDTO.setPhoto(photo);
        }

        // Загружаем координаторов
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

        long duration = System.currentTimeMillis() - startTime;
        if (duration > 10) {
            log.debug("Building DTO for sector {} took {} ms", sector.getId(), duration);
        }

        return sectorDTO;
    }

    // ==================== COORDINATOR METHODS ====================

    private void addCoordinatorOnCreate(Long sectorId, Long userId) throws RoleNotFoundException {
        log.debug("Adding coordinator {} to new sector {}", userId, sectorId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserDoesNotExistException("User not found: " + userId));

        Sector sector = sectorRepository.findById(sectorId)
                .orElseThrow(() -> new SectorDoesNotExistException("Sector not found: " + sectorId));

        SectorParticipant participant = SectorParticipant.builder()
                .sector(sector)
                .student(user)
                .isCoordinator(true)
                .status(SectorParticipantStatuses.Активный)
                .entryDate(LocalDate.now())
                .build();

        sectorParticipantRepository.save(participant);
        log.info("New participant-coordinator created with id: {}", participant.getId());

        Role role = roleRepository.findByTitle("Sector_coordinator")
                .orElseThrow(() -> new RoleNotFoundException("Роль координатор не найдена"));
        user.setRole(role);
        userRepository.save(user);

        log.info("Coordinator {} successfully added to sector {}", userId, sectorId);
    }

    @Transactional
    public void addCoordinator(Long sectorId, Long userId) throws RoleNotFoundException {
        log.info("Adding coordinator to existing sector: sectorId={}, userId={}", sectorId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserDoesNotExistException("User not found: " + userId));

        SectorParticipant participant = sectorParticipantRepository
                .findBySectorIdAndStudentId(sectorId, userId)
                .orElseThrow(() -> new UserDoesNotExistException(
                        String.format("Пользователь с id: %d не является участником сектора %d", userId, sectorId)));

        if (participant.getIsCoordinator()) {
            throw new IllegalStateException("Пользователь уже является координатором сектора");
        }

        if (participant.getStatus() != SectorParticipantStatuses.Активный) {
            throw new IllegalStateException("Пользователь не является активным участником сектора");
        }

        participant.setIsCoordinator(true);
        sectorParticipantRepository.save(participant);

        Role role = roleRepository.findByTitle("Sector_coordinator")
                .orElseThrow(() -> new RoleNotFoundException("Роль координатор не найдена"));
        user.setRole(role);
        userRepository.save(user);

        // Инвалидируем кэш сектора
        sectorListCache.invalidateAll();

        log.info("User {} became coordinator of sector {}", userId, sectorId);
    }

    @Transactional
    public void updateCoordinators(Long sectorId, List<Long> newCoordinatorIds) throws RoleNotFoundException {
        log.info("Updating coordinators for sector {}", sectorId);

        List<SectorParticipant> currentCoordinators = sectorParticipantRepository
                .findBySectorIdAndIsCoordinatorTrue(sectorId);

        List<Long> currentCoordinatorIds = currentCoordinators.stream()
                .map(c -> c.getStudent().getId())
                .collect(Collectors.toList());

        List<Long> toRemove = currentCoordinatorIds.stream()
                .filter(id -> !newCoordinatorIds.contains(id))
                .collect(Collectors.toList());

        List<Long> toAdd = newCoordinatorIds.stream()
                .filter(id -> !currentCoordinatorIds.contains(id))
                .collect(Collectors.toList());

        for (Long userId : toRemove) {
            removeCoordinatorFromSector(sectorId, userId);
        }

        for (Long userId : toAdd) {
            addCoordinator(sectorId, userId);
        }

        // Инвалидируем кэш
        sectorListCache.invalidateAll();

        log.info("Coordinators updated: removed {}, added {}", toRemove.size(), toAdd.size());
    }

    @Transactional
    public void removeCoordinatorFromSector(Long sectorId, Long userId) throws RoleNotFoundException {
        log.info("Removing coordinator from sector: sectorId={}, userId={}", sectorId, userId);

        SectorParticipant participant = sectorParticipantRepository
                .findBySectorIdAndStudentId(sectorId, userId)
                .orElseThrow(() -> new UserDoesNotExistException("Пользователь не является участником сектора"));

        if (!participant.getIsCoordinator()) {
            throw new IllegalStateException("Пользователь не является координатором сектора");
        }

        participant.setIsCoordinator(false);
        sectorParticipantRepository.save(participant);

        // Инвалидируем кэш
        sectorListCache.invalidateAll();

        List<SectorParticipant> coordinatorEntries = sectorParticipantRepository
                .findAllByStudentIdAndIsCoordinatorTrue(userId);

        if (coordinatorEntries.isEmpty()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден"));
            Role activistRole = roleRepository.findByTitle("Activist")
                    .orElseThrow(() -> new RoleNotFoundException("Роль 'Activist' не найдена"));
            user.setRole(activistRole);
            userRepository.save(user);
        }

        log.info("Coordinator removed from sector {}", sectorId);
    }

    @Transactional
    public void kickParticipantFromSector(Long sectorId, Long coordinatorId, Long userId) {
        log.info("Kicking user {} from sector {} by coordinator {}", userId, sectorId, coordinatorId);

        SectorParticipant coordinatorParticipant = sectorParticipantRepository
                .findBySectorIdAndStudentId(sectorId, coordinatorId)
                .orElseThrow(() -> new UserDoesNotExistException("Координатор не является участником сектора"));

        if (!coordinatorParticipant.getIsCoordinator()) {
            throw new SecurityException("Пользователь не является координатором сектора");
        }

        SectorParticipant participantEntry = sectorParticipantRepository
                .findBySectorIdAndStudentId(sectorId, userId)
                .orElseThrow(() -> new UserDoesNotExistException("Участник не найден в секторе"));

        if (participantEntry.getIsCoordinator()) {
            throw new SecurityException("Нельзя выгнать координатора из сектора");
        }

        participantEntry.setStatus(SectorParticipantStatuses.Вышедший);
        participantEntry.setIsCoordinator(false);
        sectorParticipantRepository.save(participantEntry);

        // Инвалидируем кэш
        sectorListCache.invalidateAll();

        log.info("User {} kicked from sector {} (status: Вышедший)", userId, sectorId);
    }

    @Transactional
    public void leaveSector(Long sectorId, Long userId) {
        log.info("User {} is leaving sector {}", userId, sectorId);

        SectorParticipant participant = sectorParticipantRepository
                .findByStudentIdAndSectorId(userId, sectorId)
                .orElseThrow(() -> new UserDoesNotExistException("Пользователь не является участником сектора"));

        if (participant.getIsCoordinator()) {
            throw new IllegalStateException("Координатор не может выйти из сектора");
        }

        if (participant.getStatus() == SectorParticipantStatuses.Вышедший) {
            throw new IllegalStateException("Пользователь уже покинул сектор");
        }

        participant.setStatus(SectorParticipantStatuses.Вышедший);
        sectorParticipantRepository.save(participant);

        // Инвалидируем кэш
        sectorListCache.invalidateAll();

        log.info("User {} left sector {}", userId, sectorId);
    }

    // ==================== CACHE STATISTICS ====================

    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        var photoStats = photoCache.stats();
        stats.put("photo_cache_hit_rate", String.format("%.2f%%", photoStats.hitRate() * 100));
        stats.put("photo_cache_size", photoCache.estimatedSize());
        stats.put("photo_cache_hits", photoStats.hitCount());
        stats.put("photo_cache_misses", photoStats.missCount());

        stats.put("sector_list_cache_size", sectorListCache.estimatedSize());
        stats.put("sector_list_keys", sectorListCache.asMap().keySet());

        return stats;
    }

    // Периодическая очистка кэша (раз в час)
    @Scheduled(fixedDelay = 3600000)
    public void cleanCache() {
        log.info("🧹 Cleaning sector cache...");
        long beforeSize = sectorListCache.estimatedSize();
        sectorListCache.invalidateAll();
        log.info("✅ Cache cleaned: {} entries removed", beforeSize);
    }
}
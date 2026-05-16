package org.example.ais_sst.service.sectorService;

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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // ==================== CACHED METHODS ====================

    @Cacheable(value = "sector", key = "#id", unless = "#result == null")
    @Transactional
    public SectorDTO getSectorById(Long id) {
        log.info("🟡 Loading sector {} from DATABASE (cache miss)", id);

        Sector sector = sectorRepository.findSectorById(id)
                .orElseThrow(() -> new SectorDoesNotExistException("Сектор с id " + id + " не существует"));

        return buildSectorDTO(sector);
    }

    @Cacheable(value = "allSectors", key = "'all'")
    @Transactional(readOnly = true)
    public List<SectorDTO> getAllSectors() {
        log.info("🟡 Loading ALL sectors from DATABASE (cache miss)");

        List<Sector> sectors = sectorRepository.findAll();
        return sectors.stream()
                .map(this::buildSectorDTO)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "activeSectors", key = "'active'")
    @Transactional(readOnly = true)
    public List<SectorDTO> getActiveSectors() {
        log.info("🟡 Loading ACTIVE sectors from DATABASE (cache miss)");

        List<Sector> sectors = sectorRepository.findByIsActiveTrue();
        return sectors.stream()
                .map(this::buildSectorDTO)
                .collect(Collectors.toList());
    }

    // ==================== CACHE EVICTION ====================

    @CacheEvict(value = {"sector", "allSectors", "activeSectors"}, allEntries = true)
    @Transactional
    public SectorDTO createSector(SectorDTO sectorDTO) throws RoleNotFoundException {
        log.info("🟢 Creating sector '{}' - clearing all caches", sectorDTO.getTitle());

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

        SectorDTO result = buildSectorDTO(savedSector);
        log.info("✅ Sector created with id: {}", savedSector.getId());

        return result;
    }

    @CacheEvict(value = {"sector", "allSectors", "activeSectors"}, allEntries = true)
    @Transactional
    public SectorDTO updateSector(SectorUpdateDTO updateDTO) throws RoleNotFoundException {
        log.info("🟢 Updating sector {} - clearing all caches", updateDTO.getId());

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
        log.info("✅ Sector {} updated", updateDTO.getId());

        return result;
    }

    @CacheEvict(value = {"sector", "allSectors", "activeSectors"}, allEntries = true)
    @Transactional
    public void deleteSector(Long id) {
        log.info("🟢 Deleting sector {} - clearing all caches", id);

        Sector sector = sectorRepository.findById(id)
                .orElseThrow(() -> new SectorDoesNotExistException("Сектор не найден"));
        sectorRepository.delete(sector);

        log.info("✅ Sector {} deleted", id);
    }

    @CacheEvict(value = {"sector", "allSectors", "activeSectors"}, key = "#id")
    @Transactional
    public void deactivateSector(Long id) {
        log.info("🟢 Deactivating sector {} - clearing cache", id);

        sectorRepository.findById(id)
                .orElseThrow(() -> new SectorDoesNotExistException(String.format("Сектор с id: %s не существует", id)));
        sectorRepository.deactivateSector(id);

        log.info("✅ Sector {} deactivated", id);
    }

    @CacheEvict(value = {"sector", "allSectors", "activeSectors"}, key = "#id")
    @Transactional
    public void activateSector(Long id) {
        log.info("🟢 Activating sector {} - clearing cache", id);

        sectorRepository.findById(id)
                .orElseThrow(() -> new SectorDoesNotExistException(String.format("Сектор с id: %s не существует", id)));
        sectorRepository.activateSector(id);

        log.info("✅ Sector {} activated", id);
    }

    // ==================== NON-CACHED METHODS (user-specific) ====================

    @Transactional(readOnly = true)
    public List<SectorWithUserStatusDTO> getSectorsWithUserStatus(Long userId) {
        log.debug("Getting sectors with status for userId: {} (not cached)", userId);

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

    @Transactional(readOnly = true)
    public Page<SectorParticipantResponseDTO> getSectorParticipants(Long sectorId, Pageable pageable) {
        log.info("Getting participants for sector id: {}", sectorId);

        Sector sector = sectorRepository.findById(sectorId)
                .orElseThrow(() -> new SectorDoesNotExistException("Сектор с id " + sectorId + " не существует"));

        Page<SectorParticipant> participants = sectorParticipantRepository.findBySectorId(sectorId, pageable);
        return participants.map(participant -> sectorParticipantMapper.toResponseDto(participant, userPhotoService));
    }

    // ==================== HELPER METHODS ====================

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
    }

    @Transactional
    public void kickParticipantFromSector(Long sectorId, Long coordinatorId, Long participantId) {
        log.info("Kicking participant {} from sector {} by coordinator {}", participantId, sectorId, coordinatorId);

        SectorParticipant coordinatorParticipant = sectorParticipantRepository
                .findBySectorIdAndStudentId(sectorId, coordinatorId)
                .orElseThrow(() -> new UserDoesNotExistException("Координатор не является участником сектора"));

        if (!coordinatorParticipant.getIsCoordinator()) {
            throw new SecurityException("Пользователь не является координатором сектора");
        }

        SectorParticipant participantEntry = sectorParticipantRepository
                .findBySectorIdAndStudentId(sectorId, participantId)
                .orElseThrow(() -> new UserDoesNotExistException("Участник не найден в секторе"));

        if (participantEntry.getIsCoordinator()) {
            throw new SecurityException("Нельзя выгнать координатора из сектора");
        }

        participantEntry.setStatus(SectorParticipantStatuses.Вышедший);
        participantEntry.setIsCoordinator(false);
        sectorParticipantRepository.save(participantEntry);

        log.info("User {} kicked from sector {} (status: Вышедший)", participantId, sectorId);
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
        log.info("User {} left sector {}", userId, sectorId);
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
}
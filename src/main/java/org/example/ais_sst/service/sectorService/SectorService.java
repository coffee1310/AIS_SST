package org.example.ais_sst.service.sectorService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.sector.SectorDTO;
import org.example.ais_sst.dto.sector.SectorWithUserStatusDTO;
import org.example.ais_sst.entity.Role;
import org.example.ais_sst.entity.Sector;
import org.example.ais_sst.entity.SectorParticipant;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.exception.SectorDoesNotExistException;
import org.example.ais_sst.exception.UserDoesNotExistException;
import org.example.ais_sst.mapper.SectorMapper;
import org.example.ais_sst.mapper.converter.SectorWithUserStatusConverter;
import org.example.ais_sst.repository.*;
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

    private final SectorWithUserStatusConverter sectorWithUserStatusConverter;

    private final UserRepository userRepository;
    private final SectorParticipantRepository sectorParticipantRepository;
    private final SectorIntroductionRequestRepository sectorIntroductionRequestRepository;

    private final RoleRepository roleRepository;

    @Transactional
    public SectorDTO createSector(SectorDTO sectorDTO) throws RoleNotFoundException {
      log.info("Creating sector with id: {}", sectorDTO.getId());

      Sector sector = sectorRepository.save(sectorMapper.toEntity(sectorDTO));

      log.info("Creating sector participant");

      createSectorParticipant(sectorDTO.getCurrentCoordinator_id(),sector.getId());

      sectorDTO = sectorMapper.toSectorDTO(sector);
      log.info("Saved sector with id: {}", sectorDTO.getId());

      User user = userRepository.findUserById(sector.getCurrentCoordinator().getId())
              .orElseThrow(() -> new UserDoesNotExistException(String.format("Пользователь с id %d не найден", sector.getCurrentCoordinator().getId())));

      Role role = roleRepository.findByTitle("Coordinator")
              .orElseThrow(() -> new RoleNotFoundException("Роль куратор не была найдена"));

      user.setRole(role);
      userRepository.save(user);

      return sectorDTO;
    }

    @Transactional
    protected SectorParticipant createSectorParticipant(Long user_id, Long sector_id) {
        Sector sector = sectorRepository.findSectorById(sector_id)
                .orElseThrow(() -> new SectorDoesNotExistException(String.format("Сектор с id: %d не существует", sector_id)));

        User user = userRepository.findUserById(user_id)
                .orElseThrow(() -> new UserDoesNotExistException(String.format("Пользователь с id: %d не существует", user_id)));

        SectorParticipant sectorParticipant = SectorParticipant.builder()
                .sector(sector)
                .student(user)
                .entryDate(LocalDate.now())
                .build();

        return sectorParticipantRepository.save(sectorParticipant);
    }

    @Transactional
    public List<SectorWithUserStatusDTO> getSectorsWithUserStatus(Long userId) {
        log.debug("Getting sectors with status for userId: {}", userId);

        // Проверка существования пользователя
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

    @Transactional
    public SectorDTO appointACoordinator(Long sector_id, Long user_id) throws RoleNotFoundException {
        Sector sector = sectorRepository.findSectorById(sector_id)
                .orElseThrow(() -> new SectorDoesNotExistException(String.format("Сектор с id: %d не существует", sector_id)));

        User old_coordinator = sector.getCurrentCoordinator();
        Role old_user_role = roleRepository.findByTitle("Acitvist")
                        .orElseThrow(() -> new RoleNotFoundException("Роль куратор не была найдена"));

        old_coordinator.setRole(old_user_role);

        User user = userRepository.findUserById(user_id)
                .orElseThrow(() -> new UserDoesNotExistException(String.format("Пользователь с id: %d не существует", user_id)));

        Role role = roleRepository.findByTitle("Curator")
                .orElseThrow(() -> new RoleNotFoundException("Роль куратор не была найдена"));

        user.setRole(role);
        userRepository.save(old_coordinator);
        userRepository.save(user);

        sector.setCurrentCoordinator(user);
        sector = sectorRepository.save(sector);
        return sectorMapper.toSectorDTO(sector);
    }
}

package org.example.ais_sst.service.sectorService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.sector.SectorDTO;
import org.example.ais_sst.entity.Sector;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.exception.SectorDoesNotExistException;
import org.example.ais_sst.exception.UserDoesNotExistException;
import org.example.ais_sst.mapper.SectorMapper;
import org.example.ais_sst.repository.SectorRepository;
import org.example.ais_sst.repository.UserRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectorService {

    private final SectorRepository sectorRepository;
    private final SectorMapper sectorMapper;

    private final UserRepository userRepository;

    @Transactional
    public SectorDTO createSector(SectorDTO sectorDTO) {
      log.info("Creating sector with id: {}", sectorDTO.getId());

      Sector sector = sectorRepository.save(sectorMapper.toEntity(sectorDTO));
      sectorDTO = sectorMapper.toSectorDTO(sector);
      log.info("Saved sector with id: {}", sectorDTO.getId());
      return sectorDTO;
    }

    @Transactional
    public SectorDTO appointACoordinator(Long sector_id, Long user_id) {
        Sector sector = sectorRepository.findSectorById(sector_id)
                .orElseThrow(() -> new SectorDoesNotExistException(String.format("Сектор с id: %d не существует", sector_id)));

        User user = userRepository.findUserById(user_id)
                .orElseThrow(() -> new UserDoesNotExistException(String.format("Пользователь с id: %d не существует", user_id)));

        sector.setCurrentCoordinator(user);
        sector = sectorRepository.save(sector);
        return sectorMapper.toSectorDTO(sector);
    }
}

package org.example.ais_sst.service.sectorService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTO;
import org.example.ais_sst.entity.Sector;
import org.example.ais_sst.entity.SectorIntroductionRequest;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.exception.SectorDoesNotExistException;
import org.example.ais_sst.exception.UserDoesNotExistException;
import org.example.ais_sst.mapper.SectorIntroductionRequestMapper;
import org.example.ais_sst.repository.SectorIntroductionRequestRepository;
import org.example.ais_sst.repository.SectorRepository;
import org.example.ais_sst.repository.UserRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectorIntroductionRequestService {

    private final SectorIntroductionRequestMapper sectorIntroductionRequestMapper;
    private final SectorIntroductionRequestRepository sectorIntroductionRequestRepository;

    private final UserRepository userRepository;
    private final SectorRepository sectorRepository;

    @Transactional
    public SectorIntroductionRequestDTO createRequest(Long user_id, Long sector_id) {
        User user = userRepository.findUserById(user_id)
                .orElseThrow(() -> new UserDoesNotExistException(String.format("Пользователь с id: %d не найден", user_id)));

        Sector sector = sectorRepository.findSectorById(sector_id)
                .orElseThrow(() -> new SectorDoesNotExistException(String.format("Сектор с таким id: %d не найден", sector_id)));

        SectorIntroductionRequest request = SectorIntroductionRequest.builder()
                .user(user)
                .sector(sector)
                .build();

        request = sectorIntroductionRequestRepository.save(request);

        return sectorIntroductionRequestMapper.toSectorIntroductionRequestDTO(request);
    }
}

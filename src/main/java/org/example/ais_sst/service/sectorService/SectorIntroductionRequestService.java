package org.example.ais_sst.service.sectorService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTO;
import org.example.ais_sst.dto.request.SectorIntroductionRequestDTOSummary;
import org.example.ais_sst.dto.sector.SectorParticipantDTO;
import org.example.ais_sst.entity.Sector;
import org.example.ais_sst.entity.SectorIntroductionRequest;
import org.example.ais_sst.entity.SectorParticipant;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.entity.enums.SectorIntroductionStatus;
import org.example.ais_sst.exception.*;
import org.example.ais_sst.mapper.SectorIntroductionRequestMapper;
import org.example.ais_sst.mapper.SectorParticipantMapper;
import org.example.ais_sst.repository.SectorIntroductionRequestRepository;
import org.example.ais_sst.repository.SectorParticipantRepository;
import org.example.ais_sst.repository.SectorRepository;
import org.example.ais_sst.repository.UserRepository;
import org.springframework.stereotype.Service;

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

    @Transactional
    public SectorIntroductionRequestDTO createRequest(Long user_id, Long sector_id) {
        User user = userRepository.findUserById(user_id)
                .orElseThrow(() -> new UserDoesNotExistException(String.format("Пользователь с id: %d не найден", user_id)));

        if (sectorParticipantRepository.existsByStudent_IdAndSector_Id(user_id, sector_id))
            throw new UserIsAlreadyInThisSectorException(String.format("Пользователь с id: %d уже вступил в этот сектор", user_id));

        if (sectorRepository.existsByCurrentCoordinator_IdAndId(user_id, sector_id))
            throw new UserIsAlreadyInThisSectorException(String.format("Пользователь с id: %d уже вступил в этот сектор", user_id));

        Sector sector = sectorRepository.findSectorById(sector_id)
                .orElseThrow(() -> new SectorDoesNotExistException(String.format("Сектор с таким id: %d не найден", sector_id)));

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

        SectorParticipant sectorParticipant = sectorParticipantService.createParticipant(request);
        SectorParticipantDTO sectorParticipantDTO = sectorParticipantMapper.toSectorParticipantDTO(sectorParticipant);

        request.setStatus(SectorIntroductionStatus.ОДОБРЕНА);
        sectorIntroductionRequestRepository.save(request);

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
    public List<SectorIntroductionRequestDTO> getRequestsListByCoordinator(Long coordinator_id) {

        Sector sector = sectorRepository.findSectorsByCurrentCoordinator_Id(coordinator_id)
                .orElseThrow(() -> new NoSectorWithSuchCooridnatorFoundException("Пользователь не является координатором"));

        List<SectorIntroductionRequest> requests = sectorIntroductionRequestRepository.
                getSectorIntroductionRequestsBySector_Id(sector.getId());

        return requests.stream()
                .map(sectorIntroductionRequestMapper::toSectorIntroductionRequestDTO)
                .toList();
    }
}

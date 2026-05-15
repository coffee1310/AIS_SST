package org.example.ais_sst.service.sectorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.entity.SectorIntroductionRequest;
import org.example.ais_sst.entity.SectorParticipant;
import org.example.ais_sst.entity.enums.SectorParticipantStatuses;
import org.example.ais_sst.mapper.SectorParticipantMapper;
import org.example.ais_sst.repository.SectorParticipantRepository;
import org.example.ais_sst.service.base.BaseEntityService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectorParticipantService extends BaseEntityService {

    private final SectorParticipantRepository repository;

    public SectorParticipant createParticipant(SectorIntroductionRequest request) {
        return executeWithLogging(() -> {
            SectorParticipant participant = SectorParticipant.builder()
                    .sector(request.getSector())
                    .student(request.getUser())
                    .status(SectorParticipantStatuses.Активный)
                    .entryDate(java.time.LocalDate.now())
                    .build();

            log.info("Creating participant for user {} in sector {}",
                    request.getUser().getId(), request.getSector().getId());

            return repository.save(participant);

        }, "createParticipant", request.getUser().getId(), request.getSector().getId());
    }
}

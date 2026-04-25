package org.example.ais_sst.service.sectorService;

import lombok.RequiredArgsConstructor;
import org.example.ais_sst.entity.SectorIntroductionRequest;
import org.example.ais_sst.entity.SectorParticipant;
import org.example.ais_sst.mapper.SectorParticipantMapper;
import org.example.ais_sst.repository.SectorParticipantRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SectorParticipantService {

    private final SectorParticipantRepository sectorParticipantRepository;

    public SectorParticipant createParticipant(SectorIntroductionRequest request) {
        SectorParticipant sectorParticipant = SectorParticipant.builder()
                .sector(request.getSector())
                .student(request.getUser())
                .build();

        return sectorParticipantRepository.save(sectorParticipant);
    }

}

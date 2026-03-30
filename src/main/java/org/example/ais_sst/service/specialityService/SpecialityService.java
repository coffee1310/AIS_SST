package org.example.ais_sst.service.specialityService;

import lombok.RequiredArgsConstructor;
import org.example.ais_sst.entity.Speciality;
import org.example.ais_sst.repository.SpecialityRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecialityService {

    private final SpecialityRepository specialityRepository;

    public List<Speciality> getSpecialities() {
        List<Speciality> specialities = specialityRepository.findAll();
        return specialities;
    }
}

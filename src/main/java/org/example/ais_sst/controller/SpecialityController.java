package org.example.ais_sst.controller;

import lombok.RequiredArgsConstructor;
import org.example.ais_sst.entity.Speciality;
import org.example.ais_sst.service.specialityService.SpecialityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/specialities")
@RequiredArgsConstructor
public class SpecialityController {

    private final SpecialityService specialityService;

    @GetMapping
    public ResponseEntity<?> getSpecialities() {
        List<Speciality> specialityList = specialityService.getSpecialities();
        return new ResponseEntity<>(specialityList, HttpStatus.OK);
    }
}

package org.example.ais_sst.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.controller.base.BaseController;
import org.example.ais_sst.entity.Speciality;
import org.example.ais_sst.service.specialityService.SpecialityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/specialities")
@RequiredArgsConstructor
public class SpecialityController extends BaseController {

    private final SpecialityService specialityService;

    @GetMapping
    public ResponseEntity<?> getSpecialities() {
        logInfo("/api/specialities", "Getting all specialities");

        List<Speciality> specialityList = specialityService.getSpecialities();
        return createSuccessResponse(specialityList);
    }
}
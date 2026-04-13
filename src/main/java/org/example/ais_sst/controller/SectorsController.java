package org.example.ais_sst.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sectors")
public class SectorsController {

    @PostMapping
    public ResponseEntity<?> createSector() {

        return new ResponseEntity<>("", HttpStatus.CREATED);
    }
}

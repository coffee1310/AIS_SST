package org.example.ais_sst.controller;

import lombok.RequiredArgsConstructor;
import org.example.ais_sst.entity.Group;
import org.example.ais_sst.repository.GroupRepository;
import org.example.ais_sst.service.groupService.GroupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    public ResponseEntity<?> getGroups() {
        List<Group> groupList = groupService.getGroups();
        return new ResponseEntity<>(groupList, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody Group group) {
        group = groupService.createGroup(group);
        return new ResponseEntity<>(group, HttpStatus.CREATED);
    }
}

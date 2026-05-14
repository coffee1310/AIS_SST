package org.example.ais_sst.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.controller.base.BaseController;
import org.example.ais_sst.entity.Group;
import org.example.ais_sst.repository.GroupRepository;
import org.example.ais_sst.service.groupService.GroupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
public class GroupController extends BaseController {

    private final GroupService groupService;

    @GetMapping
    public ResponseEntity<?> getGroups() {
        logInfo("/api/group", "Getting all groups");

        List<Group> groupList = groupService.getGroups();
        return createSuccessResponse(groupList);
    }

    @PostMapping
    public ResponseEntity<?> createGroup(@Valid @RequestBody Group group) {
        logInfo("/api/group", "Creating new group: {}", group.getTitle());

        Group createdGroup = groupService.createGroup(group);
        return createSuccessResponse("Группа успешно создана", createdGroup);
    }
}
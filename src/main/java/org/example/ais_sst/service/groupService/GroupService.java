package org.example.ais_sst.service.groupService;

import lombok.RequiredArgsConstructor;
import org.example.ais_sst.entity.Group;
import org.example.ais_sst.repository.GroupRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;

    public List<Group> getGroups() {
        return groupRepository.findAll();
    }

    public Group createGroup(Group group) {
        return groupRepository.save(group);
    }
}

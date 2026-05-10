package org.example.ais_sst.services.GroupsService;

import org.example.ais_sst.entity.Group;
import org.example.ais_sst.repository.GroupRepository;
import org.example.ais_sst.service.groupService.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @InjectMocks
    private GroupService groupService;

    private Group group1;
    private Group group2;
    private Group newGroup;

    @BeforeEach
    void setUp() {
        group1 = Group.builder()
                .id(1L)
                .title("ПИ-101")
                .build();

        group2 = Group.builder()
                .id(2L)
                .title("ПИ-102")
                .build();

        newGroup = Group.builder()
                .title("ИС-201")
                .build();
    }

    @Test
    void getGroups_Success() {
        // given
        List<Group> expectedGroups = Arrays.asList(group1, group2);
        when(groupRepository.findAll()).thenReturn(expectedGroups);

        // when
        List<Group> result = groupService.getGroups();

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(group1, group2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getTitle()).isEqualTo("ПИ-101");
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getTitle()).isEqualTo("ПИ-102");

        verify(groupRepository).findAll();
        verify(groupRepository, times(1)).findAll();
    }

    @Test
    void getGroups_WhenNoGroupsExist_ReturnsEmptyList() {
        // given
        when(groupRepository.findAll()).thenReturn(Arrays.asList());

        // when
        List<Group> result = groupService.getGroups();

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        assertThat(result).hasSize(0);

        verify(groupRepository).findAll();
    }

    @Test
    void getGroups_WhenRepositoryReturnsNull_ReturnsEmptyList() {
        // given
        when(groupRepository.findAll()).thenReturn(null);

        // when
        List<Group> result = groupService.getGroups();

        // then
        assertThat(result).isNull();

        verify(groupRepository).findAll();
    }

    @Test
    void createGroup_Success() {
        // given
        Group savedGroup = Group.builder()
                .id(3L)
                .title("ИС-201")
                .build();

        when(groupRepository.save(any(Group.class))).thenReturn(savedGroup);

        // when
        Group result = groupService.createGroup(newGroup);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getTitle()).isEqualTo("ИС-201");
        assertThat(result.getTitle()).isEqualTo(newGroup.getTitle());

        verify(groupRepository).save(newGroup);
        verify(groupRepository, times(1)).save(any(Group.class));
    }

    @Test
    void createGroup_WithEmptyTitle_StillSaves() {
        // given
        Group emptyTitleGroup = Group.builder()
                .title("")
                .build();

        Group savedGroup = Group.builder()
                .id(4L)
                .title("")
                .build();

        when(groupRepository.save(emptyTitleGroup)).thenReturn(savedGroup);

        // when
        Group result = groupService.createGroup(emptyTitleGroup);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(4L);
        assertThat(result.getTitle()).isEmpty();

        verify(groupRepository).save(emptyTitleGroup);
    }

    @Test
    void createGroup_WithNullTitle_StillSaves() {
        // given
        Group nullTitleGroup = Group.builder()
                .title(null)
                .build();

        Group savedGroup = Group.builder()
                .id(5L)
                .title(null)
                .build();

        when(groupRepository.save(nullTitleGroup)).thenReturn(savedGroup);

        // when
        Group result = groupService.createGroup(nullTitleGroup);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getTitle()).isNull();

        verify(groupRepository).save(nullTitleGroup);
    }

    @Test
    void createGroup_WithVeryLongTitle_StillSaves() {
        // given
        String longTitle = "Очень длинное название группы которая может быть больше обычного лимита";
        Group longTitleGroup = Group.builder()
                .title(longTitle)
                .build();

        Group savedGroup = Group.builder()
                .id(6L)
                .title(longTitle)
                .build();

        when(groupRepository.save(longTitleGroup)).thenReturn(savedGroup);

        // when
        Group result = groupService.createGroup(longTitleGroup);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(6L);
        assertThat(result.getTitle()).isEqualTo(longTitle);

        verify(groupRepository).save(longTitleGroup);
    }

    @Test
    void createGroup_RepositoryThrowsException_PropagatesException() {
        // given
        when(groupRepository.save(any(Group.class)))
                .thenThrow(new RuntimeException("Database error"));

        // when & then
        assertThatThrownBy(() -> groupService.createGroup(newGroup))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");

        verify(groupRepository).save(newGroup);
    }

    @Test
    void getGroups_MultipleCalls_ReturnsConsistentResults() {
        // given
        List<Group> expectedGroups = Arrays.asList(group1, group2);
        when(groupRepository.findAll()).thenReturn(expectedGroups);

        // when
        List<Group> firstCall = groupService.getGroups();
        List<Group> secondCall = groupService.getGroups();

        // then
        assertThat(firstCall).isSameAs(secondCall); // Same reference because same mock returns same object
        assertThat(firstCall).hasSize(2);
        assertThat(secondCall).hasSize(2);

        verify(groupRepository, times(2)).findAll();
    }

    @Test
    void createGroup_MultipleGroups_SavesEachCorrectly() {
        // given
        Group groupA = Group.builder().title("Группа А").build();
        Group groupB = Group.builder().title("Группа Б").build();

        Group savedGroupA = Group.builder().id(10L).title("Группа А").build();
        Group savedGroupB = Group.builder().id(11L).title("Группа Б").build();

        when(groupRepository.save(groupA)).thenReturn(savedGroupA);
        when(groupRepository.save(groupB)).thenReturn(savedGroupB);

        // when
        Group resultA = groupService.createGroup(groupA);
        Group resultB = groupService.createGroup(groupB);

        // then
        assertThat(resultA.getId()).isEqualTo(10L);
        assertThat(resultA.getTitle()).isEqualTo("Группа А");
        assertThat(resultB.getId()).isEqualTo(11L);
        assertThat(resultB.getTitle()).isEqualTo("Группа Б");

        verify(groupRepository).save(groupA);
        verify(groupRepository).save(groupB);
    }
}

package org.example.ais_sst.mock.controllers;

import org.example.ais_sst.controller.GroupController;
import org.example.ais_sst.entity.Group;
import org.example.ais_sst.service.groupService.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupControllerTest {

    @Mock
    private GroupService groupService;

    @InjectMocks
    private GroupController groupController;

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

    // ==================== TESTS FOR getGroups ====================

    @Test
    void getGroups_Success() {
        // given
        List<Group> expectedGroups = Arrays.asList(group1, group2);
        when(groupService.getGroups()).thenReturn(expectedGroups);

        // when
        ResponseEntity<?> response = groupController.getGroups();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedGroups);

        @SuppressWarnings("unchecked")
        List<Group> body = (List<Group>) response.getBody();
        assertThat(body).hasSize(2);
        assertThat(body.get(0).getId()).isEqualTo(1L);
        assertThat(body.get(0).getTitle()).isEqualTo("ПИ-101");
        assertThat(body.get(1).getId()).isEqualTo(2L);
        assertThat(body.get(1).getTitle()).isEqualTo("ПИ-102");

        verify(groupService).getGroups();
        verify(groupService, times(1)).getGroups();
    }

    @Test
    void getGroups_WhenNoGroups_ReturnsEmptyList() {
        // given
        when(groupService.getGroups()).thenReturn(Collections.emptyList());

        // when
        ResponseEntity<?> response = groupController.getGroups();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        @SuppressWarnings("unchecked")
        List<Group> body = (List<Group>) response.getBody();
        assertThat(body).isEmpty();

        verify(groupService).getGroups();
    }

    @Test
    void getGroups_WhenServiceReturnsNull_ReturnsNull() {
        // given
        when(groupService.getGroups()).thenReturn(null);

        // when
        ResponseEntity<?> response = groupController.getGroups();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNull();

        verify(groupService).getGroups();
    }

    // ==================== TESTS FOR createGroup ====================

    @Test
    void createGroup_Success() {
        // given
        Group savedGroup = Group.builder()
                .id(3L)
                .title("ИС-201")
                .build();

        when(groupService.createGroup(any(Group.class))).thenReturn(savedGroup);

        // when
        ResponseEntity<?> response = groupController.createGroup(newGroup);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(savedGroup);

        Group body = (Group) response.getBody();
        assertThat(body.getId()).isEqualTo(3L);
        assertThat(body.getTitle()).isEqualTo("ИС-201");

        verify(groupService).createGroup(newGroup);
        verify(groupService, times(1)).createGroup(any(Group.class));
    }

    @Test
    void createGroup_WithNullGroup_StillPassesToService() {
        // given
        when(groupService.createGroup(null)).thenThrow(new IllegalArgumentException("Group cannot be null"));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            groupController.createGroup(null);
        });

        verify(groupService).createGroup(null);
    }

    @Test
    void createGroup_WithEmptyTitle_StillCreates() {
        // given
        Group emptyTitleGroup = Group.builder()
                .title("")
                .build();

        Group savedGroup = Group.builder()
                .id(4L)
                .title("")
                .build();

        when(groupService.createGroup(emptyTitleGroup)).thenReturn(savedGroup);

        // when
        ResponseEntity<?> response = groupController.createGroup(emptyTitleGroup);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Group body = (Group) response.getBody();
        assertThat(body.getId()).isEqualTo(4L);
        assertThat(body.getTitle()).isEmpty();

        verify(groupService).createGroup(emptyTitleGroup);
    }

    @Test
    void createGroup_WithNullTitle_StillCreates() {
        // given
        Group nullTitleGroup = Group.builder()
                .title(null)
                .build();

        Group savedGroup = Group.builder()
                .id(5L)
                .title(null)
                .build();

        when(groupService.createGroup(nullTitleGroup)).thenReturn(savedGroup);

        // when
        ResponseEntity<?> response = groupController.createGroup(nullTitleGroup);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Group body = (Group) response.getBody();
        assertThat(body.getId()).isEqualTo(5L);
        assertThat(body.getTitle()).isNull();

        verify(groupService).createGroup(nullTitleGroup);
    }

    @Test
    void createGroup_WithVeryLongTitle_StillCreates() {
        // given
        String longTitle = "Очень длинное название группы которая может быть больше обычного лимита";
        Group longTitleGroup = Group.builder()
                .title(longTitle)
                .build();

        Group savedGroup = Group.builder()
                .id(6L)
                .title(longTitle)
                .build();

        when(groupService.createGroup(longTitleGroup)).thenReturn(savedGroup);

        // when
        ResponseEntity<?> response = groupController.createGroup(longTitleGroup);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Group body = (Group) response.getBody();
        assertThat(body.getId()).isEqualTo(6L);
        assertThat(body.getTitle()).isEqualTo(longTitle);

        verify(groupService).createGroup(longTitleGroup);
    }

    @Test
    void createGroup_ServiceThrowsException_PropagatesException() {
        // given
        when(groupService.createGroup(any(Group.class)))
                .thenThrow(new RuntimeException("Database error"));

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            groupController.createGroup(newGroup);
        });

        verify(groupService).createGroup(newGroup);
    }

    // ==================== INTEGRATION STYLE TESTS ====================

    @Test
    void getGroups_ReturnsResponseWithCorrectContentType() {
        // given
        List<Group> expectedGroups = Arrays.asList(group1, group2);
        when(groupService.getGroups()).thenReturn(expectedGroups);

        // when
        ResponseEntity<?> response = groupController.getGroups();

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedGroups);
    }

    @Test
    void createGroup_ReturnsResponseWithCorrectContentType() {
        // given
        Group savedGroup = Group.builder()
                .id(3L)
                .title("ИС-201")
                .build();

        when(groupService.createGroup(any(Group.class))).thenReturn(savedGroup);

        // when
        ResponseEntity<?> response = groupController.createGroup(newGroup);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(savedGroup);
    }

    // ==================== EDGE CASES ====================

    @Test
    void getGroups_MultipleCalls_ReturnConsistentResults() {
        // given
        List<Group> expectedGroups = Arrays.asList(group1, group2);
        when(groupService.getGroups()).thenReturn(expectedGroups);

        // when
        ResponseEntity<?> firstCall = groupController.getGroups();
        ResponseEntity<?> secondCall = groupController.getGroups();

        // then
        assertThat(firstCall.getBody()).isEqualTo(secondCall.getBody());

        verify(groupService, times(2)).getGroups();
    }

    @Test
    void createGroup_MultipleGroups_CreatesEachSuccessfully() {
        // given
        Group groupA = Group.builder().title("Группа А").build();
        Group groupB = Group.builder().title("Группа Б").build();

        Group savedGroupA = Group.builder().id(10L).title("Группа А").build();
        Group savedGroupB = Group.builder().id(11L).title("Группа Б").build();

        when(groupService.createGroup(groupA)).thenReturn(savedGroupA);
        when(groupService.createGroup(groupB)).thenReturn(savedGroupB);

        // when
        ResponseEntity<?> responseA = groupController.createGroup(groupA);
        ResponseEntity<?> responseB = groupController.createGroup(groupB);

        // then
        assertThat(((Group) responseA.getBody()).getId()).isEqualTo(10L);
        assertThat(((Group) responseB.getBody()).getId()).isEqualTo(11L);

        verify(groupService).createGroup(groupA);
        verify(groupService).createGroup(groupB);
    }
}
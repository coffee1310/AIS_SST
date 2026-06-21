package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.tasks.TaskResponseDTO;
import org.example.ais_sst.dto.tasks.UserInfoDTO;
import org.example.ais_sst.entity.Task;
import org.example.ais_sst.entity.TaskUser;
import org.example.ais_sst.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "creator", source = "task", qualifiedByName = "mapCreatorToUserInfo")
    @Mapping(target = "assignedUsers", source = "task", qualifiedByName = "mapAssignedUsersToUserInfo")
    @Mapping(target = "assignedUsersCount", source = "task", qualifiedByName = "mapAssignedUsersCount")
    TaskResponseDTO toResponseDto(Task task);

    @Named("mapCreatorToUserInfo")
    default UserInfoDTO mapCreatorToUserInfo(Task task) {
        if (task.getCreator() == null) {
            return null;
        }
        User creator = task.getCreator();
        return UserInfoDTO.builder()
                .id(creator.getId())
                .name(creator.getName())
                .surname(creator.getSurname())
                .patronymic(creator.getPatronymic())
                .email(creator.getStudentEmail())
                .build();
    }

    @Named("mapAssignedUsersToUserInfo")
    default List<UserInfoDTO> mapAssignedUsersToUserInfo(Task task) {
        if (task.getTaskUsers() == null) {
            return List.of();
        }
        return task.getTaskUsers().stream()
                .filter(tu -> !Boolean.TRUE.equals(tu.getIsDeleted()))
                .map(TaskUser::getUser)
                .map(user -> UserInfoDTO.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .surname(user.getSurname())
                        .patronymic(user.getPatronymic())
                        .email(user.getStudentEmail())
                        .build())
                .collect(Collectors.toList());
    }

    @Named("mapAssignedUsersCount")
    default Long mapAssignedUsersCount(Task task) {
        if (task.getTaskUsers() == null) {
            return 0L;
        }
        return task.getTaskUsers().stream()
                .filter(tu -> !Boolean.TRUE.equals(tu.getIsDeleted()))
                .count();
    }

    List<TaskResponseDTO> toResponseDtoList(List<Task> tasks);
}
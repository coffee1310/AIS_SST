package org.example.ais_sst.mapper;

import org.example.ais_sst.dto.task_request.TaskRequestResponseDTO;
import org.example.ais_sst.entity.TaskRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TaskRequestMapper {

    @Mapping(target = "taskId", source = "task.id")
    @Mapping(target = "taskTitle", source = "task.title")
    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentName", source = "student.name")
    @Mapping(target = "studentSurname", source = "student.surname")
    @Mapping(target = "status", source = "status", qualifiedByName = "mapStatusToString")
    TaskRequestResponseDTO toResponseDto(TaskRequest taskRequest);

    @Named("mapStatusToString")
    default String mapStatusToString(org.example.ais_sst.entity.enums.TaskRequestStatus status) {
        if (status == null) {
            return null;
        }
        return status.getDisplayName();
    }

    List<TaskRequestResponseDTO> toResponseDtoList(List<TaskRequest> taskRequests);
}
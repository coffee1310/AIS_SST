package org.example.ais_sst.dto.tasks;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponseDTO {
    private Integer id;
    private String title;
    private String description;
    private Instant deadline;
    private Integer maxPeopleCount;
    private Integer countOfPoints;
    private Boolean isCompleted;
    private Boolean isDeleted;
    private Boolean isPreassigned;
    private Instant createdAt;
    private Instant updatedAt;
    private UserInfoDTO creator;  // Информация о создателе
    private List<UserInfoDTO> assignedUsers;
    private Long assignedUsersCount;
}
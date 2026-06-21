package org.example.ais_sst.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 128)
    @NotNull
    @Column(name = "title", nullable = false, length = 128)
    private String title;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @Column(name = "deadline")
    private Instant deadline;

    @NotNull
    @Min(0)
    @Column(name = "max_people_count", nullable = false)
    @Builder.Default
    private Integer maxPeopleCount = 0;

    @NotNull
    @Min(1)
    @Column(name = "count_of_points", nullable = false)
    @Builder.Default
    private Integer countOfPoints = 1;

    @ColumnDefault("false")
    @Column(name = "is_completed")
    @Builder.Default
    private Boolean isCompleted = false;

    @ColumnDefault("false")
    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @ColumnDefault("false")
    @Column(name = "is_preassigned")
    @Builder.Default
    private Boolean isPreassigned = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TaskUser> taskUsers = new ArrayList<>();

    public void addUser(User user) {
        TaskUser taskUser = TaskUser.builder()
                .task(this)
                .user(user)
                .isAssigned(false)
                .isCompleted(false)
                .build();
        taskUsers.add(taskUser);
    }

    public void removeUser(User user) {
        taskUsers.removeIf(tu -> tu.getUser().equals(user));
    }

    public List<User> getAssignedUsers() {
        return taskUsers.stream()
                .filter(tu -> !Boolean.TRUE.equals(tu.getIsDeleted()))
                .map(TaskUser::getUser)
                .collect(java.util.stream.Collectors.toList());
    }

    public long getAssignedUsersCount() {
        return taskUsers.stream()
                .filter(tu -> !Boolean.TRUE.equals(tu.getIsDeleted()))
                .count();
    }
}
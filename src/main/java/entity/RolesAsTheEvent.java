package entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "roles_as_the_event")
public class RolesAsTheEvent {
    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 64)
    @NotNull
    @Column(name = "title", nullable = false, length = 64)
    private String title;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @NotNull
    @Column(name = "deadline_by_date", nullable = false)
    private LocalDate deadlineByDate;

    @NotNull
    @Column(name = "deadline_by_time", nullable = false)
    private LocalTime deadlineByTime;

    @NotNull
    @Column(name = "people_count", nullable = false)
    private Integer peopleCount;

    @NotNull
    @Column(name = "reserve_count", nullable = false)
    private Integer reserveCount;

    @ColumnDefault("0")
    @Column(name = "count_of_points")
    private Integer countOfPoints;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsible_sector_id", nullable = false)
    private Sector responsibleSector;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @NotNull
    @Column(name = "is_accepting_applications_open", nullable = false)
    private Boolean isAcceptingApplicationsOpen = false;

}
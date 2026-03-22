package org.example.ais_sst.dto.user;

import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserScoreDTO {

    private Long userId;

    private Integer lessons_points;
    private Integer tasks_points;
    private Integer events_points;

    public Integer get_total_points() {
        return this.events_points + this.lessons_points + this.tasks_points;
    }

    public ScoreBreakDown get_break_down() {
        return new ScoreBreakDown(lessons_points, tasks_points, events_points);
    }

    public record ScoreBreakDown(
            Integer lessons,
            Integer tasks,
            Integer events
    ){}
}

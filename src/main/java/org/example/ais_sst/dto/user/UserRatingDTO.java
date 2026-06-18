package org.example.ais_sst.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRatingDTO {
    private Long userId;
    private String userName;
    private String userSurname;
    private Long totalPoints;
    private Long eventsCount;
    private Integer position; // Место в рейтинге
}
package org.example.ais_sst.dto.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventOrganizerResponseDTO {

    private Long id;
    private Long eventId;
    private String eventTitle;
    private Long userId;
    private String userName;
    private String userSurname;
    private String userPatronymic;
    private String userEmail;
    private String userPhoto;
    private LocalDateTime addedAt;
}
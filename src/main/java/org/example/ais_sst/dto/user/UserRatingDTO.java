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
    private String patronymic;   // Новое поле для полного ФИО
    private String role;         // Новое поле (название роли)
    private String fio;          // Новое поле: готовое ФИО "Фамилия Имя Отчество"
    private Long totalPoints;
    private Long eventsCount;
    private Integer position; // Место в рейтинге (1, 2, 3...)
}
package org.example.ais_sst.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFilterDTO {
    private String role;        // Фильтр по роли
    private String search;      // Поиск по имени, фамилии, email
    private Boolean isActive;   // Фильтр по активности
    private Boolean isBanned;   // Фильтр по бану
    private Long groupId;       // Фильтр по группе
    private Long specialityId;  // Фильтр по специальности
    private Long sectorId;  // Добавьте это поле
}
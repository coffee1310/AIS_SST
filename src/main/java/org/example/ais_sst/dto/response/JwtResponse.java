package org.example.ais_sst.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String refreshToken;
    private String type = "Bearer";
    private Long id;
    private String email;
    private String name;
    private String surname;
    private List<String> roles;

    // Конструктор без type (type будет "Bearer" по умолчанию)
    public JwtResponse(String token, String refreshToken, Long id, String email, String name, String surname, List<String> roles) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.id = id;
        this.email = email;
        this.name = name;
        this.surname = surname;
        this.roles = roles;
        this.type = "Bearer";
    }
}
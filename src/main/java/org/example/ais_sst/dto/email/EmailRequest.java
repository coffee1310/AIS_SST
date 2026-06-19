package org.example.ais_sst.dto.email;

import lombok.Data;

@Data
public class EmailRequest {
    private String to;
    private String subject;
    private String body;
    // getters + setters
}
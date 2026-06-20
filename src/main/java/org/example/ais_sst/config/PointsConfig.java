package org.example.ais_sst.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class PointsConfig {

    @Value("${app.points.organizer.default:5}")
    private int defaultOrganizerPoints;

    @Value("${app.points.participant.default:2}")
    private int defaultParticipantPoints;
}
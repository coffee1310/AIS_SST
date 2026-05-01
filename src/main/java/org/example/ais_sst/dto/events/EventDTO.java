package org.example.ais_sst.dto.events;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ais_sst.annotation.ValidUserId;
import org.example.ais_sst.entity.User;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventDTO {

    private Long id;

    private String title;

    @NotNull
    private LocalDate dateOfEvent;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    @Size(max = 256)
    private String venue;

    @Column(name = "photo")
    private byte[] photo;

    @NotNull
    private String referenceToPosition;

    @ColumnDefault("true")
    @Column(name = "is_public")
    private Boolean isPublic;

    @ColumnDefault("true")
    private Boolean isDraft;

    @NotNull
    @ValidUserId
    private Long organizer_id;

    @ColumnDefault("false")
    @Builder.Default
    private Boolean isCompleted = false;

    @ColumnDefault("false")
    @Column(name = "is_deleted")
    private Boolean isDeleted;
}

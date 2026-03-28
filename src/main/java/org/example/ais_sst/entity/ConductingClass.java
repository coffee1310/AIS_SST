package org.example.ais_sst.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "conducting_classes")
public class ConductingClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ColumnDefault("'Очно'")
    @Column(name = "format", columnDefinition = "formats_for_conducting_classes not null")
    private Object format;

    @Size(max = 128)
    @Column(name = "venue", length = 128)
    private String venue;


}
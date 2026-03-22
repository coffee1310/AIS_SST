package org.example.ais_sst.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "conducting_classes")
public class ConductingClass {
    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 128)
    @Column(name = "venue", length = 128)
    private String venue;

/*
 TODO [Reverse Engineering] create field to map the 'format' column
 Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @ColumnDefault("'Очно'")
    @Column(name = "format", columnDefinition = "formats_for_conducting_classes not null")
    private Object format;
*/
}
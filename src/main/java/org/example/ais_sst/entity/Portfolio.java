package org.example.ais_sst.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "portfolios")
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "path_to_file", nullable = false, length = Integer.MAX_VALUE)
    private String pathToFile;

    @Size(max = 128)
    @NotNull
    @Column(name = "file_name", nullable = false, length = 128)
    private String fileName;


}
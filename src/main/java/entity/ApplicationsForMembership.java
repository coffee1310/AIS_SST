package entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "applications_for_membership")
public class ApplicationsForMembership {
    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_owner_id", nullable = false)
    private User documentOwner;

    @NotNull
    @Column(name = "path_to_file", nullable = false, length = Integer.MAX_VALUE)
    private String pathToFile;

    @Size(max = 128)
    @NotNull
    @Column(name = "file_name", nullable = false, length = 128)
    private String fileName;

/*
 TODO [Reverse Engineering] create field to map the 'status' column
 Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @ColumnDefault("'На рассмотрении'")
    @Column(name = "status", columnDefinition = "application_statuses_for_membership not null")
    private Object status;
*/
}
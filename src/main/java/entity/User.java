package entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 64)
    @NotNull
    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Size(max = 64)
    @NotNull
    @Column(name = "surname", nullable = false, length = 64)
    private String surname;

    @Size(max = 64)
    @Column(name = "patronymic", length = 64)
    private String patronymic;

    @NotNull
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "course_number")
    private Short courseNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "speciality_id")
    private Speciality speciality;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(name = "student_id_number")
    private Integer studentIdNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "social_status_id")
    private SocialStatus socialStatus;

    @Size(max = 32)
    @NotNull
    @Column(name = "student_email", nullable = false, length = 32)
    private String studentEmail;

    @Size(max = 256)
    @Column(name = "additional_email", length = 256)
    private String additionalEmail;

    @Size(max = 16)
    @NotNull
    @Column(name = "phone_number", nullable = false, length = 16)
    private String phoneNumber;

    @Size(max = 256)
    @Column(name = "vk_link", length = 256)
    private String vkLink;

    @Column(name = "photo")
    private byte[] photo;

    @Size(max = 256)
    @NotNull
    @Column(name = "password", nullable = false, length = 256)
    private String password;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ColumnDefault("true")
    @Column(name = "is_active")
    private Boolean isActive;
    @ColumnDefault("false")
    @Column(name = "is_banned")
    private Boolean isBanned;

/*
 TODO [Reverse Engineering] create field to map the 'gender' column
 Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "gender", columnDefinition = "genders not null")
    private Object gender;
*/
}
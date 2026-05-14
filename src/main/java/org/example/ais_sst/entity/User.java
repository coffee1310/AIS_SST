package org.example.ais_sst.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.example.ais_sst.entity.enums.Gender;
import org.hibernate.annotations.*;

import java.sql.Types;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Builder
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@SQLDelete(sql = "UPDATE users SET is_deleted = true WHERE id = ?")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

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

    // Только @JdbcTypeCode, без @Enumerated!
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", columnDefinition = "genders", nullable = false)
    private Gender gender;

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

    @Column(name = "path_to_photo", length = 512)  // Новое поле
    private String pathToPhoto;

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

    @OneToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "account_creating_request_id")
    private AccountCreatingRequest accountCreatingRequest;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;
}
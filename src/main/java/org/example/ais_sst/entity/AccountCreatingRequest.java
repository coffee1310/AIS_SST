package org.example.ais_sst.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.example.ais_sst.entity.enums.AccountCreatingRequestStatus;
import org.example.ais_sst.entity.enums.Gender;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@AllArgsConstructor
@Table(name = "account_creating_requests")
@RequiredArgsConstructor
@Builder
public class AccountCreatingRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(name = "gender", columnDefinition = "genders not null")
    private Gender gender;

    @NotNull
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @NotNull
    @Column(name = "course_number", nullable = false)
    private Short courseNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "speciality_id", nullable = false)
    private Speciality speciality;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @NotNull
    @Column(name = "student_id_number", nullable = false)
    private Integer studentIdNumber;

    @Size(max = 32)
    @NotNull
    @Column(name = "student_email", nullable = false, length = 32)
    private String studentEmail;

    @Size(max = 16)
    @NotNull
    @Column(name = "phone_number", nullable = false, length = 16)
    private String phoneNumber;

    @Column(name = "reason_for_refusal", length = Integer.MAX_VALUE)
    private String reasonForRefusal;

    @Size(max = 256)
    @NotNull
    @Column(name = "password", nullable = false, length = 256)
    private String password;

    @NotNull
    @Column(name = "status", nullable = false)
    private AccountCreatingRequestStatus status;
}
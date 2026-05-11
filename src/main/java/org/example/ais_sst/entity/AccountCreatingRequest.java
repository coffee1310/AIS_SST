package org.example.ais_sst.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.example.ais_sst.entity.enums.AccountCreatingRequestStatus;
import org.example.ais_sst.entity.enums.Gender;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestRejectDTO;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestsSummaryDTO;
import org.example.ais_sst.entity.AccountCreatingRequest;
import org.example.ais_sst.entity.Group;
import org.example.ais_sst.entity.Speciality;
import org.example.ais_sst.entity.enums.AccountCreatingRequestStatus;
import org.example.ais_sst.entity.enums.Gender;
import org.example.ais_sst.exception.*;
import org.example.ais_sst.repository.AccountCreatingRequestsRepository;
import org.example.ais_sst.repository.GroupRepository;
import org.example.ais_sst.repository.SpecialityRepository;
import org.example.ais_sst.repository.UserRepository;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

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

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @NotNull
    @Column(name = "status", nullable = false)
    private AccountCreatingRequestStatus status;

    @Size(max = 256)
    @Column(name = "additional_email", length = 256)
    private String additionalEmail;


    @Column(name = "path_to_photo", length = 512)
    private String pathToPhoto;  // Заменяет поле photo

    @Column(name = "vk_link")
    private String vkLink;
}
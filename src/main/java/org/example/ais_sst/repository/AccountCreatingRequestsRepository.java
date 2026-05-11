package org.example.ais_sst.repository;

import org.example.ais_sst.entity.AccountCreatingRequest;
import org.example.ais_sst.entity.enums.AccountCreatingRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountCreatingRequestsRepository extends JpaRepository<AccountCreatingRequest, Long> {
    Optional<AccountCreatingRequest> findAccountCreatingRequestById(Long id);

    Page<AccountCreatingRequest> findByStatus(AccountCreatingRequestStatus status, Pageable pageable);

    @Query(value = """
    SELECT 
        acr.id,
        acr.name,
        acr.surname,
        acr.patronymic,
        acr.gender,
        acr.date_of_birth,
        acr.course_number,
        acr.student_id_number,
        acr.student_email,
        acr.phone_number,
        acr.reason_for_refusal,
        acr.status,
        acr.created_at,
        acr.updated_at,
        g.id as group_id,
        g.title as group_title,
        s.id as speciality_id,
        s.title as speciality_title,
        acr.path_to_photo   -- Добавлено поле photo
    FROM account_creating_requests acr
    LEFT JOIN groups g ON g.id = acr.group_id
    LEFT JOIN specialities s ON s.id = acr.speciality_id
    WHERE 
    (CAST(:id AS bigint) IS NULL OR acr.id = CAST(:id AS bigint))
    AND (CAST(:name AS text) IS NULL OR acr.name ILIKE CONCAT('%', CAST(:name AS text), '%'))
    AND (CAST(:surname AS text) IS NULL OR acr.surname ILIKE CONCAT('%', CAST(:surname AS text), '%'))
    AND (CAST(:patronymic AS text) IS NULL OR acr.patronymic ILIKE CONCAT('%', CAST(:patronymic AS text), '%'))
    AND (CAST(:gender AS text) IS NULL OR CAST(acr.gender AS text) = CAST(:gender AS text))
    AND (CAST(:dateFrom AS date) IS NULL OR acr.date_of_birth >= CAST(:dateFrom AS date))
    AND (CAST(:dateTo AS date) IS NULL OR acr.date_of_birth <= CAST(:dateTo AS date))
    AND (CAST(:studentEmail AS text) IS NULL OR acr.student_email ILIKE CONCAT('%', CAST(:studentEmail AS text), '%'))
    AND (CAST(:phoneNumber AS text) IS NULL OR acr.phone_number ILIKE CONCAT('%', CAST(:phoneNumber AS text), '%'))
    AND (CAST(:studentIdNumber AS integer) IS NULL OR acr.student_id_number = CAST(:studentIdNumber AS integer))
    AND (CAST(:courseNumber AS smallint) IS NULL OR acr.course_number = CAST(:courseNumber AS smallint))
    AND (CAST(:status AS text) IS NULL OR CAST(acr.status AS text) = CAST(:status AS text))
    AND (CAST(:groupId AS bigint) IS NULL OR acr.group_id = CAST(:groupId AS bigint))
    AND (CAST(:specialityId AS bigint) IS NULL OR acr.speciality_id = CAST(:specialityId AS bigint))
    ORDER BY acr.created_at DESC
    OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
    """, nativeQuery = true)
    List<Object[]> findAllWithFiltersNative(
            @Param("id") Long id,
            @Param("name") String name,
            @Param("surname") String surname,
            @Param("patronymic") String patronymic,
            @Param("gender") String gender,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("studentEmail") String studentEmail,
            @Param("phoneNumber") String phoneNumber,
            @Param("studentIdNumber") Integer studentIdNumber,
            @Param("courseNumber") Short courseNumber,
            @Param("status") String status,
            @Param("groupId") Long groupId,
            @Param("specialityId") Long specialityId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Query(value = """
    SELECT COUNT(*)
    FROM account_creating_requests acr
    LEFT JOIN groups g ON g.id = acr.group_id
    LEFT JOIN specialities s ON s.id = acr.speciality_id
    WHERE 
    (CAST(:id AS bigint) IS NULL OR acr.id = CAST(:id AS bigint))
    AND (CAST(:name AS text) IS NULL OR acr.name ILIKE CONCAT('%', CAST(:name AS text), '%'))
    AND (CAST(:surname AS text) IS NULL OR acr.surname ILIKE CONCAT('%', CAST(:surname AS text), '%'))
    AND (CAST(:patronymic AS text) IS NULL OR acr.patronymic ILIKE CONCAT('%', CAST(:patronymic AS text), '%'))
    AND (CAST(:gender AS text) IS NULL OR CAST(acr.gender AS text) = CAST(:gender AS text))
    AND (CAST(:dateFrom AS date) IS NULL OR acr.date_of_birth >= CAST(:dateFrom AS date))
    AND (CAST(:dateTo AS date) IS NULL OR acr.date_of_birth <= CAST(:dateTo AS date))
    AND (CAST(:studentEmail AS text) IS NULL OR acr.student_email ILIKE CONCAT('%', CAST(:studentEmail AS text), '%'))
    AND (CAST(:phoneNumber AS text) IS NULL OR acr.phone_number ILIKE CONCAT('%', CAST(:phoneNumber AS text), '%'))
    AND (CAST(:studentIdNumber AS integer) IS NULL OR acr.student_id_number = CAST(:studentIdNumber AS integer))
    AND (CAST(:courseNumber AS smallint) IS NULL OR acr.course_number = CAST(:courseNumber AS smallint))
    AND (CAST(:status AS text) IS NULL OR CAST(acr.status AS text) = CAST(:status AS text))
    AND (CAST(:groupId AS bigint) IS NULL OR acr.group_id = CAST(:groupId AS bigint))
    AND (CAST(:specialityId AS bigint) IS NULL OR acr.speciality_id = CAST(:specialityId AS bigint))
    """, nativeQuery = true)
    long countAllWithFiltersNative(
            @Param("id") Long id,
            @Param("name") String name,
            @Param("surname") String surname,
            @Param("patronymic") String patronymic,
            @Param("gender") String gender,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("studentEmail") String studentEmail,
            @Param("phoneNumber") String phoneNumber,
            @Param("studentIdNumber") Integer studentIdNumber,
            @Param("courseNumber") Short courseNumber,
            @Param("status") String status,
            @Param("groupId") Long groupId,
            @Param("specialityId") Long specialityId);


}

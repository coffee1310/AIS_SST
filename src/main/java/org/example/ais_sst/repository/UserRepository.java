package org.example.ais_sst.repository;

import org.example.ais_sst.dto.user.UserProjection;
import org.example.ais_sst.dto.user.UserProjectionDTO;
import org.example.ais_sst.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByStudentEmail(String studentEmail);
    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findUserById(Long id);

    boolean existsByStudentEmail(String studentEmail);
    boolean existsByPhoneNumber(String phoneNumber);

    @Query("SELECT u FROM User u WHERE u.role.title = :role")
    Page<User> findByRole(@Param("role") String role, Pageable pageable);

    Page<User> findByRole_Title(String role, Pageable pageable);

    // ==================== NATIVE QUERY ====================
    @Query(value = """
        SELECT 
            u.id,
            u.name,
            u.surname,
            u.patronymic,
            CAST(u.gender AS text) as gender,
            u.date_of_birth,
            u.course_number,
            u.student_id_number,
            u.student_email,
            u.additional_email,
            u.phone_number,
            u.vk_link,
            u.is_active,
            u.is_banned,
            r.title as role_title,
            g.id as group_id,
            g.title as group_title,
            s.id as speciality_id,
            s.title as speciality_title,
            u.path_to_photo,
            s.short_title
        FROM users u
        LEFT JOIN roles r ON r.id = u.role_id
        LEFT JOIN groups g ON g.id = u.group_id
        LEFT JOIN specialities s ON s.id = u.speciality_id
        LEFT JOIN sector_participants sp ON sp.student_id = u.id
        WHERE (CAST(:id AS bigint) IS NULL OR u.id = CAST(:id AS bigint))
        AND (CAST(:role AS text) IS NULL OR r.title = CAST(:role AS text))
        AND (CAST(:search AS text) IS NULL OR 
             u.name ILIKE CONCAT('%', CAST(:search AS text), '%') OR
             u.surname ILIKE CONCAT('%', CAST(:search AS text), '%') OR
             u.student_email ILIKE CONCAT('%', CAST(:search AS text), '%'))
        AND (CAST(:isActive AS boolean) IS NULL OR u.is_active = CAST(:isActive AS boolean))
        AND (CAST(:isBanned AS boolean) IS NULL OR u.is_banned = CAST(:isBanned AS boolean))
        AND (CAST(:groupId AS bigint) IS NULL OR u.group_id = CAST(:groupId AS bigint))
        AND (CAST(:specialityId AS bigint) IS NULL OR u.speciality_id = CAST(:specialityId AS bigint))
        AND (CAST(:sectorId AS bigint) IS NULL OR sp.sector_id = CAST(:sectorId AS bigint))
        ORDER BY u.id
        OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
        """, nativeQuery = true)
    List<Object[]> findAllWithFiltersNative(
            @Param("id") Long id,
            @Param("role") String role,
            @Param("search") String search,
            @Param("isActive") Boolean isActive,
            @Param("isBanned") Boolean isBanned,
            @Param("groupId") Long groupId,
            @Param("specialityId") Long specialityId,
            @Param("sectorId") Long sectorId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Query(value = """
        SELECT COUNT(DISTINCT u.id)
        FROM users u
        LEFT JOIN roles r ON r.id = u.role_id
        LEFT JOIN groups g ON g.id = u.group_id
        LEFT JOIN specialities s ON s.id = u.speciality_id
        LEFT JOIN sector_participants sp ON sp.student_id = u.id
        WHERE (CAST(:id AS bigint) IS NULL OR u.id = CAST(:id AS bigint))
        AND (CAST(:role AS text) IS NULL OR r.title = CAST(:role AS text))
        AND (CAST(:search AS text) IS NULL OR 
             u.name ILIKE CONCAT('%', CAST(:search AS text), '%') OR
             u.surname ILIKE CONCAT('%', CAST(:search AS text), '%') OR
             u.student_email ILIKE CONCAT('%', CAST(:search AS text), '%'))
        AND (CAST(:isActive AS boolean) IS NULL OR u.is_active = CAST(:isActive AS boolean))
        AND (CAST(:isBanned AS boolean) IS NULL OR u.is_banned = CAST(:isBanned AS boolean))
        AND (CAST(:groupId AS bigint) IS NULL OR u.group_id = CAST(:groupId AS bigint))
        AND (CAST(:specialityId AS bigint) IS NULL OR u.speciality_id = CAST(:specialityId AS bigint))
        AND (CAST(:sectorId AS bigint) IS NULL OR sp.sector_id = CAST(:sectorId AS bigint))
        """, nativeQuery = true)
    long countAllWithFiltersNative(
            @Param("id") Long id,
            @Param("role") String role,
            @Param("search") String search,
            @Param("isActive") Boolean isActive,
            @Param("isBanned") Boolean isBanned,
            @Param("groupId") Long groupId,
            @Param("specialityId") Long specialityId,
            @Param("sectorId") Long sectorId);
}
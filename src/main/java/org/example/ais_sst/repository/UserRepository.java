package org.example.ais_sst.repository;

import org.example.ais_sst.dto.user.UserProjection;
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

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.role r " +
            "LEFT JOIN FETCH u.group g " +
            "LEFT JOIN FETCH u.speciality s " +
            "WHERE (:role IS NULL OR r.title = :role) " +
            "AND (:isActive IS NULL OR u.isActive = :isActive) " +
            "AND (:isBanned IS NULL OR u.isBanned = :isBanned) " +
            "AND (:groupId IS NULL OR g.id = :groupId) " +
            "AND (:specialityId IS NULL OR s.id = :specialityId)")
    Page<User> findAllWithFilters(@Param("role") String role,
                                  @Param("isActive") Boolean isActive,
                                  @Param("isBanned") Boolean isBanned,
                                  @Param("groupId") Long groupId,
                                  @Param("specialityId") Long specialityId,
                                  Pageable pageable);


    @Query("SELECT u FROM User u WHERE u.role.title = :role")
    Page<User> findByRole(@Param("role") String role, Pageable pageable);

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.role r " +
            "LEFT JOIN FETCH u.group g " +
            "LEFT JOIN FETCH u.speciality s")
    Page<User> findAllWithFilters(Pageable pageable);

    Page<User> findByRole_Title(String role, Pageable pageable);
}

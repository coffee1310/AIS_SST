package org.example.ais_sst.repository;

import org.example.ais_sst.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByStudentEmail(String studentEmail);
    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findUserById(Long id);

    boolean existsByStudentEmail(String studentEmail);
    boolean existsByPhoneNumber(String phoneNumber);
}

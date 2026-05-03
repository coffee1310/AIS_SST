package org.example.ais_sst.repository;

import org.example.ais_sst.entity.SocialStatusStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SocialStatusStudentsRepository extends JpaRepository<SocialStatusStudent, Long> {
    List<SocialStatusStudent> findByStudentId(Long studentId);

    @Query("SELECT sss.socialStatus.title FROM SocialStatusStudent sss WHERE sss.student.id = :studentId")
    List<String> findSocialStatusTitlesByStudentId(@Param("studentId") Long studentId);
}

package org.example.ais_sst.repository;

import org.example.ais_sst.entity.SocialStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocialStatusRepository extends JpaRepository<SocialStatus, Long> {
}

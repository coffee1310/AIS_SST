package org.example.ais_sst.repository;

import org.example.ais_sst.entity.RolesAsTheEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolesAsTheEventRepository extends JpaRepository<RolesAsTheEvent, Long> {
    boolean existsByTitle(String title);

    Optional<RolesAsTheEvent> findByTitle(String title);

    List<RolesAsTheEvent> findByIsDefaultRoleTrue();

//    @Query("SELECT SUM(*) FROM roles_as_the_event")
//    public Integer sumPointsByUserId(Long userId);
}

package org.example.ais_sst.repository;

import org.example.ais_sst.entity.GlobalEventRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GlobalEventRolesRepository extends JpaRepository<GlobalEventRole, Long> {
    boolean existsByTitle(String title);

    Optional<GlobalEventRole> findByTitle(String title);

//    @Query("SELECT SUM(*) FROM roles_as_the_event")
//    public Integer sumPointsByUserId(Long userId);
}

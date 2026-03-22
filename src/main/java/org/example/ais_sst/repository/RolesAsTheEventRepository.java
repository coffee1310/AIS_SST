package org.example.ais_sst.repository;

import org.example.ais_sst.entity.RolesAsTheEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RolesAsTheEventRepository extends JpaRepository<RolesAsTheEvent, Long> {

//    @Query("SELECT SUM(*) FROM roles_as_the_event")
//    public Integer sumPointsByUserId(Long userId);
}

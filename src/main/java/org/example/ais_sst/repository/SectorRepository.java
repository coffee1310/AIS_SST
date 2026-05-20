package org.example.ais_sst.repository;

import org.example.ais_sst.entity.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Optional;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Long> {
    boolean existsByTitle(String title);

    Optional<Sector> findSectorById(Long id);

    @Query(value = """
    SELECT DISTINCT
        s.id,
        s.title,
        s.description,
        CASE 
            WHEN sp.id IS NOT NULL AND sp.status = 'Активный' 
                 AND (sir.id IS NULL OR sir.status NOT IN ('На рассмотрении', 'Ожидание'))
            THEN true 
            ELSE false 
        END as is_participant,
        CASE WHEN sir.id IS NOT NULL AND sir.status IN ('На рассмотрении', 'Ожидание') 
             THEN true ELSE false END as has_active_request,
        COALESCE(sp.is_coordinator, false) as is_coordinator,
        s.path_to_photo,
        sir.status as request_status,
        (SELECT COUNT(*) FROM sector_participants sp2 WHERE sp2.sector_id = s.id AND sp2.status = 'Активный') as participant_count
    FROM sectors s
    LEFT JOIN sector_participants sp ON sp.sector_id = s.id 
        AND sp.student_id = :userId 
    LEFT JOIN sector_introduction_request sir ON sir.sector_id = s.id 
        AND sir.user_id = :userId 
        AND sir.status IN ('На рассмотрении', 'Ожидание')
    WHERE s.is_active = true
    ORDER BY is_participant DESC, has_active_request DESC, s.title ASC
    """, nativeQuery = true)
    List<Object[]> findSectorsWithUserStatus(@Param("userId") Long userId);

    Sector getSectorById(Long id);

    Optional<Sector> findByTitle(String title);

    @Modifying
    @Query("update Sector s set s.isActive = false where s.id = :id")
    Integer deactivateSector(@Param("id") Long id);

    @Modifying
    @Query("update Sector s set s.isActive = true where s.id = :id")
    Integer activateSector(@Param("id") Long id);

    List<Sector> findByIsActiveTrue();

    @Query("SELECT DISTINCT s FROM Sector s LEFT JOIN FETCH s.sectorParticipants p LEFT JOIN FETCH p.student WHERE s.isActive = true")
    List<Sector> findActiveSectorsWithParticipants();

    // Новый метод для загрузки всех секторов с инициализацией
    @Query("SELECT DISTINCT s FROM Sector s LEFT JOIN FETCH s.sectorParticipants p LEFT JOIN FETCH p.student")
    List<Sector> findAllWithParticipants();
}
package org.example.ais_sst.repository;

import org.example.ais_sst.entity.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Long> {
    boolean existsByTitle(String title);

    Optional<Sector> findSectorById(Long id);

    @Query(value = """
    SELECT 
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
        s.photo,  -- путь к фото сектора
        sir.status as request_status,
        (SELECT COUNT(*) FROM sector_participants sp2 WHERE sp2.sector_id = s.id AND sp2.status = 'Активный') as participant_count,
        coord.name as coordinator_name,
        coord.surname as coordinator_surname,
        coord.patronymic as coordinator_patronymic,
        coord.path_to_photo as coordinator_photo  -- изменено с coord.photo на coord.path_to_photo
    FROM sectors s
    LEFT JOIN sector_participants sp ON sp.sector_id = s.id 
        AND sp.student_id = :userId 
    LEFT JOIN sector_introduction_request sir ON sir.sector_id = s.id 
        AND sir.user_id = :userId 
        AND sir.status IN ('На рассмотрении', 'Ожидание')
    LEFT JOIN sector_participants coord_participant ON coord_participant.sector_id = s.id 
        AND coord_participant.is_coordinator = true
        AND coord_participant.status = 'Активный'
    LEFT JOIN users coord ON coord.id = coord_participant.student_id
    WHERE s.is_active = true
    ORDER BY is_participant DESC, has_active_request DESC, s.title ASC
    """, nativeQuery = true)
    List<Object[]> findSectorsWithUserStatus(@Param("userId") Long userId);

    Sector getSectorById(Long id);
}
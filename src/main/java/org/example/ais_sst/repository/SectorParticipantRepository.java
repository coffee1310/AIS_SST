package org.example.ais_sst.repository;

import org.example.ais_sst.entity.SectorParticipant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectorParticipantRepository extends JpaRepository<SectorParticipant, Long> {


    List<SectorParticipant> findByStudentId(Long studentId);

    List<SectorParticipant> findBySectorId(Long sectorId);

    Optional<SectorParticipant> findBySectorIdAndStudentId(Long sectorId, Long studentId);

    Optional<SectorParticipant> findBySectorIdAndIsCoordinatorTrue(Long sectorId);

    List<SectorParticipant> findAllBySectorIdAndIsCoordinatorTrue(Long sectorId);

    List<SectorParticipant> findAllByStudentIdAndIsCoordinatorTrue(Long userId);

    @Query("SELECT sp.sector.title FROM SectorParticipant sp WHERE sp.student.id = :userId AND sp.isCoordinator = true")
    Optional<String> findCoordinatorSectorTitleByUserId(@Param("userId") Long userId);

    @Query("SELECT sp FROM SectorParticipant sp WHERE sp.student.id = :userId AND sp.isCoordinator = true")
    Optional<SectorParticipant> findCoordinatorByUserId(@Param("userId") Long userId);


    @Query("SELECT sp FROM SectorParticipant sp WHERE sp.student.id = :userId AND sp.isCoordinator = true")
    List<SectorParticipant> findSectorsWhereUserIsCoordinator(@Param("userId") Long userId);

    boolean existsByStudentIdAndSectorId(Long studentId, Long sectorId);

    boolean existsByStudentId(Long studentId);

    boolean existsBySectorId(Long sectorId);

    // Получить всех участников сектора с пагинацией
    Page<SectorParticipant> findBySectorId(Long sectorId, Pageable pageable);


    // Получить всех активных участников сектора
    @Query("SELECT sp FROM SectorParticipant sp WHERE sp.sector.id = :sectorId AND sp.status = 'Активный'")
    Page<SectorParticipant> findActiveBySectorId(@Param("sectorId") Long sectorId, Pageable pageable);

    long countBySectorId(Long sectorId);
}
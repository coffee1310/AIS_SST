package org.example.ais_sst.repository;

import org.example.ais_sst.entity.SectorParticipant;
import org.example.ais_sst.entity.enums.SectorParticipantStatuses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectorParticipantRepository extends JpaRepository<SectorParticipant, Long> {

    List<SectorParticipant> findByStudentId(Long studentId);

    Optional<SectorParticipant> findBySectorIdAndStudentId(Long sectorId, Long studentId);

    Optional<SectorParticipant> findByStudentIdAndSectorId(Long studentId, Long sectorId);

    List<SectorParticipant> findBySectorIdAndIsCoordinatorTrue(Long sectorId);

    List<SectorParticipant> findAllByStudentIdAndIsCoordinatorTrue(Long userId);

    @Query("SELECT sp.sector.title FROM SectorParticipant sp WHERE sp.student.id = :userId AND sp.isCoordinator = true")
    Optional<String> findCoordinatorSectorTitleByUserId(@Param("userId") Long userId);

    @Query("SELECT sp FROM SectorParticipant sp WHERE sp.student.id = :userId AND sp.isCoordinator = true")
    Optional<SectorParticipant> findCoordinatorByUserId(@Param("userId") Long userId);

    @Query("SELECT sp FROM SectorParticipant sp WHERE sp.student.id = :userId AND sp.isCoordinator = true")
    List<SectorParticipant> findSectorsWhereUserIsCoordinator(@Param("userId") Long userId);

    Page<SectorParticipant> findBySectorId(Long sectorId, Pageable pageable);

    @Query("SELECT sp FROM SectorParticipant sp WHERE sp.sector.id = :sectorId AND sp.status = 'Активный'")
    Page<SectorParticipant> findActiveBySectorId(@Param("sectorId") Long sectorId, Pageable pageable);

    @Query("SELECT sp.sector.id, sp.sector.title FROM SectorParticipant sp " +
            "WHERE sp.student.id = :userId AND sp.isCoordinator = true")
    List<Object[]> findCoordinatorSectorInfoByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE SectorParticipant sp SET sp.isCoordinator = true WHERE sp.sector.id = :sectorId AND sp.student.id = :userId")
    void setCoordinatorFlag(@Param("sectorId") Long sectorId, @Param("userId") Long userId);

    boolean existsBySectorIdAndStudentId(Long sectorId, Long studentId);

    @Query("SELECT sp.sector.title FROM SectorParticipant sp WHERE sp.student.id = :userId AND sp.status = :status")
    List<String> findSectorTitlesByUserIdAndStatus(@Param("userId") Long userId, @Param("status") SectorParticipantStatuses status);

    // НОВЫЙ МЕТОД: получить все секторы с деталями
    @Query("SELECT sp.sector.id, sp.sector.title, sp.status, sp.isCoordinator FROM SectorParticipant sp WHERE sp.student.id = :userId AND sp.status = :status")
    List<Object[]> findSectorDetailsByUserIdAndStatus(@Param("userId") Long userId, @Param("status") SectorParticipantStatuses status);

    List<SectorParticipant> findByStudentIdAndStatus(Long studentId, SectorParticipantStatuses status);
}
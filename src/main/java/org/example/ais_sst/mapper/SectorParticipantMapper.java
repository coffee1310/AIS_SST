package org.example.ais_sst.mapper;


import org.example.ais_sst.dto.sector.SectorParticipantDTO;
import org.example.ais_sst.entity.Sector;
import org.example.ais_sst.entity.SectorParticipant;
import org.example.ais_sst.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface SectorParticipantMapper {

    // Из Entity в DTO
    @Mapping(source = "student.id", target = "student_id")
    @Mapping(source = "sector.id", target = "sector_id")
    SectorParticipantDTO toSectorParticipantDTO(SectorParticipant sectorParticipant);

    // Из DTO в Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "student", source = "student_id", qualifiedByName = "mapStudent")
    @Mapping(target = "sector", source = "sector_id", qualifiedByName = "mapSector")
    SectorParticipant toEntity(SectorParticipantDTO sectorParticipantDTO);

    @Named("mapStudent")
    default User mapStudent(Long studentId) {
        if (studentId == null) return null;
        User user = new User();
        user.setId(studentId);
        return user;
    }

    @Named("mapSector")
    default Sector mapSector(Long sectorId) {
        if (sectorId == null) return null;
        Sector sector = new Sector();
        sector.setId(sectorId);
        return sector;
    }
}
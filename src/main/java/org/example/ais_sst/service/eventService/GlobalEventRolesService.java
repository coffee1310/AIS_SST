package org.example.ais_sst.service.eventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.events.GlobalEventRoleCreateDTO;
import org.example.ais_sst.dto.events.GlobalEventRoleDTO;
import org.example.ais_sst.dto.events.GlobalEventRoleUpdateDTO;
import org.example.ais_sst.entity.GlobalEventRole;
import org.example.ais_sst.entity.Sector;
import org.example.ais_sst.mapper.GlobalEventRolesMapper;
import org.example.ais_sst.repository.GlobalEventRolesRepository;
import org.example.ais_sst.repository.SectorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GlobalEventRolesService {

    private final GlobalEventRolesRepository globalEventRolesRepository;
    private final SectorRepository sectorRepository;
    private final GlobalEventRolesMapper globalEventRolesMapper;

    @Transactional
    public GlobalEventRoleDTO createRole(GlobalEventRoleCreateDTO dto) {
        log.info("Creating new role: {}", dto.getTitle());

        // Проверяем существование сектора
        Sector sector = sectorRepository.findById(dto.getSector_id())
                .orElseThrow(() -> new RuntimeException("Сектор с id " + dto.getSector_id() + " не найден"));

        // Проверяем, существует ли роль с таким названием
        Optional<GlobalEventRole> globalEventRole = globalEventRolesRepository.findByTitle(dto.getTitle());
        if (!globalEventRole.isEmpty() && globalEventRole.get().getIsDeleted() == false) {
            throw new RuntimeException("Роль с названием '" + dto.getTitle() + "' уже существует");
        }

        GlobalEventRole role = globalEventRolesMapper.toEntity(dto);
        role.setSector(sector);  // Устанавливаем сектор

        GlobalEventRole savedRole = globalEventRolesRepository.save(role);
        log.info("Role created successfully with id: {}", savedRole.getId());

        return globalEventRolesMapper.toDto(savedRole);
    }

    @Transactional
    public GlobalEventRoleDTO updateRole(Long id, GlobalEventRoleUpdateDTO dto) {
        log.info("Updating role with id: {}", id);

        GlobalEventRole role = globalEventRolesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Роль с id " + id + " не найдена"));

        // Проверяем существование сектора, если передан
        if (dto.getSector_id() != null) {
            Sector sector = sectorRepository.findById(dto.getSector_id())
                    .orElseThrow(() -> new RuntimeException("Сектор с id " + dto.getSector_id() + " не найден"));
            role.setSector(sector);
        }

        if (dto.getTitle() != null && !dto.getTitle().equals(role.getTitle())) {
            if (globalEventRolesRepository.existsByTitle(dto.getTitle())) {
                throw new RuntimeException("Роль с названием '" + dto.getTitle() + "' уже существует");
            }
            role.setTitle(dto.getTitle());
        }

        if (dto.getDescription() != null) {
            role.setDescription(dto.getDescription());
        }

        if (dto.getDefault_points() != null) {
            role.setDefaultPoints(dto.getDefault_points());
        }

        GlobalEventRole updatedRole = globalEventRolesRepository.save(role);
        log.info("Role updated successfully: {}", updatedRole.getId());

        return globalEventRolesMapper.toDto(updatedRole);
    }
    @Transactional(readOnly = true)
    public List<GlobalEventRoleDTO> getAllRoles() {
        log.info("Getting all roles");

        return globalEventRolesRepository.findAll().stream()
                .map(globalEventRolesMapper::toDto)
                .collect(Collectors.toList());
    }

    // ДОБАВЬТЕ МЕТОД ДЛЯ ПОЛУЧЕНИЯ РОЛЕЙ ПО СЕКТОРУ
    @Transactional(readOnly = true)
    public GlobalEventRoleDTO getRoleByTitle(String title) {
        log.info("Getting role by title: {}", title);

        GlobalEventRole role = globalEventRolesRepository.findByTitle(title)
                .orElseThrow(() -> new RuntimeException("Роль с названием '" + title + "' не найдена"));

        return globalEventRolesMapper.toDto(role);
    }
    @Transactional(readOnly = true)
    public GlobalEventRoleDTO getRoleById(Long id) {
        log.info("Getting role by id: {}", id);

        GlobalEventRole role = globalEventRolesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Роль с id " + id + " не найдена"));

        return globalEventRolesMapper.toDto(role);
    }

    // ДОБАВЬТЕ ЭТОТ МЕТОД
    @Transactional
    public void deleteRole(Long id) {
        log.info("Deleting role with id: {}", id);

        GlobalEventRole role = globalEventRolesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Роль с id " + id + " не найдена"));

        globalEventRolesRepository.delete(role);
        log.info("Role deleted successfully: {}", id);
    }
}
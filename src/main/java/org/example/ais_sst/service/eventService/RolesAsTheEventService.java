package org.example.ais_sst.service.eventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.events.RolesAsTheEventCreateDTO;
import org.example.ais_sst.dto.events.RolesAsTheEventDTO;
import org.example.ais_sst.dto.events.RolesAsTheEventUpdateDTO;
import org.example.ais_sst.entity.RolesAsTheEvent;
import org.example.ais_sst.exception.RoleAlreadyExistsException;
import org.example.ais_sst.exception.RoleNotFoundException;
import org.example.ais_sst.mapper.RolesAsTheEventMapper;
import org.example.ais_sst.repository.RolesAsTheEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RolesAsTheEventService {

    private final RolesAsTheEventRepository rolesAsTheEventRepository;
    private final RolesAsTheEventMapper mapper;

    /**
     * Создание новой роли
     */
    @Transactional
    public RolesAsTheEventDTO createRole(RolesAsTheEventCreateDTO request) {
        log.info("Creating new role: {}", request.getTitle());

        if (rolesAsTheEventRepository.existsByTitle((request.getTitle()))) {
            throw new RoleAlreadyExistsException("Роль с названием '" + request.getTitle() + "' уже существует");
        }

        if (request.getIsDefaultRole()) {
            removeDefaultRoleFlag();
        }

        RolesAsTheEvent role = mapper.toEntity(request);
        RolesAsTheEvent savedRole = rolesAsTheEventRepository.save(role);

        log.info("Role created successfully with id: {}", savedRole.getId());
        return mapper.toDto(savedRole);
    }

    /**
     * Получение роли по ID
     */
    @Transactional(readOnly = true)
    public RolesAsTheEventDTO getRoleById(Long id) {
        RolesAsTheEvent role = rolesAsTheEventRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException("Роль с id " + id + " не найдена"));
        return mapper.toDto(role);
    }

    /**
     * Получение роли по названию
     */
    @Transactional(readOnly = true)
    public RolesAsTheEventDTO getRoleByTitle(String title) {
        RolesAsTheEvent role = rolesAsTheEventRepository.findByTitle(title)
                .orElseThrow(() -> new RoleNotFoundException("Роль с названием '" + title + "' не найдена"));
        return mapper.toDto(role);
    }

    /**
     * Получение всех ролей
     */
    @Transactional(readOnly = true)
    public List<RolesAsTheEventDTO> getAllRoles() {
        return rolesAsTheEventRepository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Получение роли по умолчанию
     */
    @Transactional(readOnly = true)
    public RolesAsTheEventDTO getDefaultRole() {
        List<RolesAsTheEvent> defaultRoles = rolesAsTheEventRepository.findByIsDefaultRoleTrue();
        if (defaultRoles.isEmpty()) {
            throw new RoleNotFoundException("Роль по умолчанию не найдена");
        }
        return mapper.toDto(defaultRoles.get(0));
    }

    /**
     * Обновление роли
     */
    @Transactional
    public RolesAsTheEventDTO updateRole(Long id, RolesAsTheEventUpdateDTO request) {
        log.info("Updating role with id: {}", id);

        RolesAsTheEvent role = rolesAsTheEventRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException("Роль с id " + id + " не найдена"));

        if (!role.getTitle().equals(request.getTitle()) &&
                rolesAsTheEventRepository.existsByTitle(request.getTitle())) {
            throw new RoleAlreadyExistsException("Роль с названием '" + request.getTitle() + "' уже существует");
        }

        if (request.getIsDefaultRole() != null && request.getIsDefaultRole() && !role.getIsDefaultRole()) {
            removeDefaultRoleFlag();
        }

        role.setTitle(request.getTitle());
        role.setDescription(request.getDescription());
        if (request.getIsDefaultRole() != null) {
            role.setIsDefaultRole(request.getIsDefaultRole());
        }

        RolesAsTheEvent updatedRole = rolesAsTheEventRepository.save(role);
        log.info("Role updated successfully: {}", updatedRole.getId());

        return mapper.toDto(updatedRole);
    }

    /**
     * Удаление роли
     */
    @Transactional
    public void deleteRole(Long id) {
        log.info("Deleting role with id: {}", id);

        RolesAsTheEvent role = rolesAsTheEventRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException("Роль с id " + id + " не найдена"));

        if (role.getIsDefaultRole()) {
            throw new IllegalStateException("Нельзя удалить роль по умолчанию");
        }

        rolesAsTheEventRepository.delete(role);
        log.info("Role deleted successfully: {}", id);
    }

    /**
     * Снятие флага "по умолчанию" со всех ролей
     */
    private void removeDefaultRoleFlag() {
        List<RolesAsTheEvent> defaultRoles = rolesAsTheEventRepository.findByIsDefaultRoleTrue();
        for (RolesAsTheEvent role : defaultRoles) {
            role.setIsDefaultRole(false);
            rolesAsTheEventRepository.save(role);
        }
    }

}

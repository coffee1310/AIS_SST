package org.example.ais_sst.mock.services.EventsService;

import org.example.ais_sst.dto.events.GlobalEventRoleCreateDTO;
import org.example.ais_sst.dto.events.GlobalEventRoleDTO;
import org.example.ais_sst.dto.events.GlobalEventRoleUpdateDTO;
import org.example.ais_sst.entity.GlobalEventRole;
import org.example.ais_sst.entity.Sector;
import org.example.ais_sst.mapper.GlobalEventRolesMapper;
import org.example.ais_sst.repository.GlobalEventRolesRepository;
import org.example.ais_sst.repository.SectorRepository;
import org.example.ais_sst.service.eventService.GlobalEventRolesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalEventRolesServiceTest {

    @Mock
    private GlobalEventRolesRepository globalEventRolesRepository;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private GlobalEventRolesMapper globalEventRolesMapper;

    @InjectMocks
    private GlobalEventRolesService globalEventRolesService;

    private Sector sector;
    private GlobalEventRole role;
    private GlobalEventRoleDTO roleDTO;
    private GlobalEventRoleCreateDTO createDTO;
    private GlobalEventRoleUpdateDTO updateDTO;

    @BeforeEach
    void setUp() {
        sector = Sector.builder()
                .id(1L)
                .title("Спортивный сектор")
                .description("Описание спортивного сектора")
                .isActive(true)
                .build();

        role = GlobalEventRole.builder()
                .id(1L)
                .title("Организатор")
                .description("Организует мероприятия")
                .sector(sector)
                .build();

        roleDTO = GlobalEventRoleDTO.builder()
                .id(1L)
                .title("Организатор")
                .description("Организует мероприятия")
                .sectorId(1L)
                .sectorTitle("Спортивный сектор")
                .build();

        createDTO = GlobalEventRoleCreateDTO.builder()
                .title("Организатор")
                .description("Организует мероприятия")
                .sector_id(1L)
                .build();

        updateDTO = GlobalEventRoleUpdateDTO.builder()
                .title("Главный организатор")
                .description("Главный организатор мероприятий")
                .sector_id(1L)
                .build();
    }

    // ==================== TESTS FOR createRole ====================

    @Test
    void createRole_Success() {
        // given
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(globalEventRolesRepository.existsByTitle("Организатор")).thenReturn(false);
        when(globalEventRolesMapper.toEntity(createDTO)).thenReturn(role);
        when(globalEventRolesRepository.save(any(GlobalEventRole.class))).thenReturn(role);
        when(globalEventRolesMapper.toDto(role)).thenReturn(roleDTO);

        // when
        GlobalEventRoleDTO result = globalEventRolesService.createRole(createDTO);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Организатор");
        assertThat(result.getSectorId()).isEqualTo(1L);

        verify(sectorRepository).findById(1L);
        verify(globalEventRolesRepository).existsByTitle("Организатор");
        verify(globalEventRolesMapper).toEntity(createDTO);
        verify(globalEventRolesRepository).save(any(GlobalEventRole.class));
        verify(globalEventRolesMapper).toDto(role);
    }

    @Test
    void createRole_SectorNotFound_ThrowsException() {
        // given
        when(sectorRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> globalEventRolesService.createRole(createDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Сектор с id 1 не найден");

        verify(sectorRepository).findById(1L);
        verify(globalEventRolesRepository, never()).existsByTitle(any());
        verify(globalEventRolesRepository, never()).save(any());
    }

    @Test
    void createRole_TitleAlreadyExists_ThrowsException() {
        // given
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(globalEventRolesRepository.existsByTitle("Организатор")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> globalEventRolesService.createRole(createDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Роль с названием 'Организатор' уже существует");

        verify(sectorRepository).findById(1L);
        verify(globalEventRolesRepository).existsByTitle("Организатор");
        verify(globalEventRolesRepository, never()).save(any());
    }

    // ==================== TESTS FOR updateRole ====================

    @Test
    void updateRole_Success() {
        // given
        when(globalEventRolesRepository.findById(1L)).thenReturn(Optional.of(role));
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(globalEventRolesRepository.existsByTitle("Главный организатор")).thenReturn(false);
        when(globalEventRolesRepository.save(any(GlobalEventRole.class))).thenReturn(role);
        when(globalEventRolesMapper.toDto(role)).thenReturn(roleDTO);

        // when
        GlobalEventRoleDTO result = globalEventRolesService.updateRole(1L, updateDTO);

        // then
        assertThat(result).isNotNull();

        verify(globalEventRolesRepository).findById(1L);
        verify(sectorRepository).findById(1L);
        verify(globalEventRolesRepository).existsByTitle("Главный организатор");
        verify(globalEventRolesRepository).save(role);
    }

    @Test
    void updateRole_RoleNotFound_ThrowsException() {
        // given
        when(globalEventRolesRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> globalEventRolesService.updateRole(1L, updateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Роль с id 1 не найдена");

        verify(globalEventRolesRepository).findById(1L);
        verify(sectorRepository, never()).findById(any());
        verify(globalEventRolesRepository, never()).save(any());
    }

    @Test
    void updateRole_UpdateSectorOnly_Success() {
        // given
        GlobalEventRoleUpdateDTO updateSectorOnly = GlobalEventRoleUpdateDTO.builder()
                .sector_id(2L)
                .build();

        Sector newSector = Sector.builder().id(2L).title("Новый сектор").build();

        when(globalEventRolesRepository.findById(1L)).thenReturn(Optional.of(role));
        when(sectorRepository.findById(2L)).thenReturn(Optional.of(newSector));
        when(globalEventRolesRepository.save(any(GlobalEventRole.class))).thenReturn(role);
        when(globalEventRolesMapper.toDto(role)).thenReturn(roleDTO);

        // when
        GlobalEventRoleDTO result = globalEventRolesService.updateRole(1L, updateSectorOnly);

        // then
        assertThat(result).isNotNull();
        verify(globalEventRolesRepository).findById(1L);
        verify(sectorRepository).findById(2L);
        verify(globalEventRolesRepository, never()).existsByTitle(any());
    }

    @Test
    void updateRole_UpdateTitleOnly_Success() {
        // given
        GlobalEventRoleUpdateDTO updateTitleOnly = GlobalEventRoleUpdateDTO.builder()
                .title("Новое название")
                .build();

        when(globalEventRolesRepository.findById(1L)).thenReturn(Optional.of(role));
        when(globalEventRolesRepository.existsByTitle("Новое название")).thenReturn(false);
        when(globalEventRolesRepository.save(any(GlobalEventRole.class))).thenReturn(role);
        when(globalEventRolesMapper.toDto(role)).thenReturn(roleDTO);

        // when
        GlobalEventRoleDTO result = globalEventRolesService.updateRole(1L, updateTitleOnly);

        // then
        assertThat(result).isNotNull();
        verify(globalEventRolesRepository).findById(1L);
        verify(globalEventRolesRepository).existsByTitle("Новое название");
        verify(sectorRepository, never()).findById(any());
    }

    @Test
    void updateRole_UpdateDescriptionOnly_Success() {
        // given
        GlobalEventRoleUpdateDTO updateDescriptionOnly = GlobalEventRoleUpdateDTO.builder()
                .description("Новое описание")
                .build();

        when(globalEventRolesRepository.findById(1L)).thenReturn(Optional.of(role));
        when(globalEventRolesRepository.save(any(GlobalEventRole.class))).thenReturn(role);
        when(globalEventRolesMapper.toDto(role)).thenReturn(roleDTO);

        // when
        GlobalEventRoleDTO result = globalEventRolesService.updateRole(1L, updateDescriptionOnly);

        // then
        assertThat(result).isNotNull();
        assertThat(role.getDescription()).isEqualTo("Новое описание");

        verify(globalEventRolesRepository).findById(1L);
        verify(sectorRepository, never()).findById(any());
        verify(globalEventRolesRepository, never()).existsByTitle(any());
    }

    @Test
    void updateRole_NewTitleAlreadyExists_ThrowsException() {
        // given
        when(globalEventRolesRepository.findById(1L)).thenReturn(Optional.of(role));
        // ДОБАВЬТЕ ЭТУ СТРОКУ - мокаем успешный поиск сектора
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(globalEventRolesRepository.existsByTitle("Главный организатор")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> globalEventRolesService.updateRole(1L, updateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Роль с названием 'Главный организатор' уже существует");

        verify(globalEventRolesRepository).findById(1L);
        verify(sectorRepository).findById(1L);
        verify(globalEventRolesRepository).existsByTitle("Главный организатор");
        verify(globalEventRolesRepository, never()).save(any());
    }

    @Test
    void updateRole_SectorNotFound_ThrowsException() {
        // given
        when(globalEventRolesRepository.findById(1L)).thenReturn(Optional.of(role));
        when(sectorRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> globalEventRolesService.updateRole(1L, updateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Сектор с id 1 не найден");

        verify(globalEventRolesRepository).findById(1L);
        verify(sectorRepository).findById(1L);
        verify(globalEventRolesRepository, never()).save(any());
    }

    // ==================== TESTS FOR getAllRoles ====================

    @Test
    void getAllRoles_Success() {
        // given
        List<GlobalEventRole> roles = Arrays.asList(role);
        List<GlobalEventRoleDTO> roleDTOs = Arrays.asList(roleDTO);

        when(globalEventRolesRepository.findAll()).thenReturn(roles);
        when(globalEventRolesMapper.toDto(role)).thenReturn(roleDTO);

        // when
        List<GlobalEventRoleDTO> result = globalEventRolesService.getAllRoles();

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);

        verify(globalEventRolesRepository).findAll();
        verify(globalEventRolesMapper).toDto(role);
    }

    @Test
    void getAllRoles_EmptyList_ReturnsEmptyList() {
        // given
        when(globalEventRolesRepository.findAll()).thenReturn(List.of());

        // when
        List<GlobalEventRoleDTO> result = globalEventRolesService.getAllRoles();

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(globalEventRolesRepository).findAll();
        verify(globalEventRolesMapper, never()).toDto(any());
    }

    // ==================== TESTS FOR getRoleByTitle ====================

    @Test
    void getRoleByTitle_Success() {
        // given
        when(globalEventRolesRepository.findByTitle("Организатор")).thenReturn(Optional.of(role));
        when(globalEventRolesMapper.toDto(role)).thenReturn(roleDTO);

        // when
        GlobalEventRoleDTO result = globalEventRolesService.getRoleByTitle("Организатор");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Организатор");

        verify(globalEventRolesRepository).findByTitle("Организатор");
        verify(globalEventRolesMapper).toDto(role);
    }

    @Test
    void getRoleByTitle_NotFound_ThrowsException() {
        // given
        when(globalEventRolesRepository.findByTitle("Организатор")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> globalEventRolesService.getRoleByTitle("Организатор"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Роль с названием 'Организатор' не найдена");

        verify(globalEventRolesRepository).findByTitle("Организатор");
        verify(globalEventRolesMapper, never()).toDto(any());
    }

    // ==================== TESTS FOR getRoleById ====================

    @Test
    void getRoleById_Success() {
        // given
        when(globalEventRolesRepository.findById(1L)).thenReturn(Optional.of(role));
        when(globalEventRolesMapper.toDto(role)).thenReturn(roleDTO);

        // when
        GlobalEventRoleDTO result = globalEventRolesService.getRoleById(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        verify(globalEventRolesRepository).findById(1L);
        verify(globalEventRolesMapper).toDto(role);
    }

    @Test
    void getRoleById_NotFound_ThrowsException() {
        // given
        when(globalEventRolesRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> globalEventRolesService.getRoleById(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Роль с id 1 не найдена");

        verify(globalEventRolesRepository).findById(1L);
        verify(globalEventRolesMapper, never()).toDto(any());
    }

    // ==================== TESTS FOR deleteRole ====================

    @Test
    void deleteRole_Success() {
        // given
        when(globalEventRolesRepository.findById(1L)).thenReturn(Optional.of(role));
        doNothing().when(globalEventRolesRepository).delete(role);

        // when
        globalEventRolesService.deleteRole(1L);

        // then
        verify(globalEventRolesRepository).findById(1L);
        verify(globalEventRolesRepository).delete(role);
    }

    @Test
    void deleteRole_RoleNotFound_ThrowsException() {
        // given
        when(globalEventRolesRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> globalEventRolesService.deleteRole(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Роль с id 1 не найдена");

        verify(globalEventRolesRepository).findById(1L);
        verify(globalEventRolesRepository, never()).delete(any());
    }
}
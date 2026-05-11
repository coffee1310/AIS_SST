package org.example.ais_sst.services.EventsService;

import org.example.ais_sst.dto.event_roles.EventRoleCreateDTO;
import org.example.ais_sst.dto.event_roles.EventRoleFilterDTO;
import org.example.ais_sst.dto.event_roles.EventRoleResponseDTO;
import org.example.ais_sst.dto.event_roles.EventRoleUpdateDTO;
import org.example.ais_sst.entity.Event;
import org.example.ais_sst.entity.EventRole;
import org.example.ais_sst.entity.GlobalEventRole;
import org.example.ais_sst.exception.*;
import org.example.ais_sst.mapper.EventRoleMapper;
import org.example.ais_sst.repository.EventRepository;
import org.example.ais_sst.repository.EventRoleRepository;
import org.example.ais_sst.repository.GlobalEventRolesRepository;
import org.example.ais_sst.service.eventService.EventRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventRoleServiceTest {

    @Mock
    private EventRoleRepository eventRoleRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private GlobalEventRolesRepository globalEventRoleRepository;

    @Mock
    private EventRoleMapper eventRoleMapper;

    @InjectMocks
    private EventRoleService eventRoleService;

    private Event event;
    private GlobalEventRole globalEventRole;
    private EventRole eventRole;
    private EventRoleCreateDTO createDTO;
    private EventRoleUpdateDTO updateDTO;
    private EventRoleResponseDTO responseDTO;
    private EventRoleFilterDTO filterDTO;

    @BeforeEach
    void setUp() {
        event = Event.builder()
                .id(1L)
                .title("Конференция")
                .build();

        globalEventRole = GlobalEventRole.builder()
                .id(1L)
                .title("Волонтер")
                .description("Помощь в организации")
                .build();

        eventRole = EventRole.builder()
                .id(1L)
                .event(event)
                .globalEventRole(globalEventRole)
                .capacity(10)
                .reserveCapacity(2)
                .deleted(false)
                .build();

        createDTO = EventRoleCreateDTO.builder()
                .eventId(1L)
                .globalEventRoleId(1L)
                .capacity(10)
                .reserveCapacity(2)
                .build();

        updateDTO = EventRoleUpdateDTO.builder()
                .capacity(15)
                .reserveCapacity(3)
                .deleted(false)
                .build();

        responseDTO = EventRoleResponseDTO.builder()
                .id(1L)
                .eventId(1L)
                .globalEventRoleId(1L)
                .capacity(10)
                .reserveCapacity(2)
                .deleted(false)
                .build();

        filterDTO = EventRoleFilterDTO.builder()
                .eventId(1L)
                .globalEventRoleId(1L)
                .deleted(false)
                .build();
    }

    // ==================== TESTS FOR createEventRole ====================

    @Test
    void createEventRole_Success() {
        // given
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(globalEventRoleRepository.findById(1L)).thenReturn(Optional.of(globalEventRole));
        when(eventRoleRepository.existsByEventIdAndGlobalEventRoleIdAndDeletedFalse(1L, 1L))
                .thenReturn(false);
        when(eventRoleMapper.toEntity(createDTO)).thenReturn(eventRole);  // ✅ Добавлен мок
        when(eventRoleRepository.save(any(EventRole.class))).thenReturn(eventRole);
        when(eventRoleMapper.toResponseDto(eventRole)).thenReturn(responseDTO);

        // when
        EventRoleResponseDTO result = eventRoleService.createEventRole(createDTO);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        verify(eventRepository).findById(1L);
        verify(globalEventRoleRepository).findById(1L);
        verify(eventRoleRepository).existsByEventIdAndGlobalEventRoleIdAndDeletedFalse(1L, 1L);
        verify(eventRoleMapper).toEntity(createDTO);
        verify(eventRoleRepository).save(any(EventRole.class));
    }

    @Test
    void createEventRole_EventNotFound_ThrowsException() {
        // given
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eventRoleService.createEventRole(createDTO))
                .isInstanceOf(EventDoesNotExistException.class)
                .hasMessageContaining("Мероприятие не найдено");
    }

    @Test
    void createEventRole_GlobalRoleNotFound_ThrowsException() {
        // given
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(globalEventRoleRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eventRoleService.createEventRole(createDTO))
                .isInstanceOf(GlobalRoleDoesNotExistException.class)
                .hasMessageContaining("Глобальная роль не найдена");
    }

    @Test
    void createEventRole_RoleAlreadyExists_ThrowsException() {
        // given
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(globalEventRoleRepository.findById(1L)).thenReturn(Optional.of(globalEventRole));
        when(eventRoleRepository.existsByEventIdAndGlobalEventRoleIdAndDeletedFalse(1L, 1L))
                .thenReturn(true);

        // when & then
        assertThatThrownBy(() -> eventRoleService.createEventRole(createDTO))
                .isInstanceOf(EventRoleAlreadyExistsException.class)
                .hasMessageContaining("Роль уже существует для этого мероприятия");
    }

    // ==================== TESTS FOR getEventRoleById ====================

    @Test
    void getEventRoleById_Success() {
        // given
        when(eventRoleRepository.findById(1L)).thenReturn(Optional.of(eventRole));
        when(eventRoleMapper.toResponseDto(eventRole)).thenReturn(responseDTO);

        // when
        EventRoleResponseDTO result = eventRoleService.getEventRoleById(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        verify(eventRoleRepository).findById(1L);
    }

    @Test
    void getEventRoleById_NotFound_ThrowsException() {
        // given
        when(eventRoleRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eventRoleService.getEventRoleById(1L))
                .isInstanceOf(EventRoleDoesNotFoundException.class)
                .hasMessageContaining("Роль мероприятия не найдена");
    }

    // ==================== TESTS FOR updateEventRole ====================

    @Test
    void updateEventRole_Success() {
        // given
        when(eventRoleRepository.findById(1L)).thenReturn(Optional.of(eventRole));
        when(eventRoleRepository.save(any(EventRole.class))).thenReturn(eventRole);
        when(eventRoleMapper.toResponseDto(eventRole)).thenReturn(responseDTO);

        // when
        EventRoleResponseDTO result = eventRoleService.updateEventRole(1L, updateDTO);

        // then
        assertThat(result).isNotNull();
        assertThat(eventRole.getCapacity()).isEqualTo(15);
        assertThat(eventRole.getReserveCapacity()).isEqualTo(3);

        verify(eventRoleRepository).findById(1L);
        verify(eventRoleRepository).save(eventRole);
    }

    @Test
    void updateEventRole_UpdateEvent_Success() {
        // given
        Event newEvent = Event.builder().id(2L).title("Новое мероприятие").build();
        updateDTO.setEventId(2L);

        when(eventRoleRepository.findById(1L)).thenReturn(Optional.of(eventRole));
        when(eventRepository.findById(2L)).thenReturn(Optional.of(newEvent));
        when(eventRoleRepository.save(any(EventRole.class))).thenReturn(eventRole);
        when(eventRoleMapper.toResponseDto(eventRole)).thenReturn(responseDTO);

        // when
        EventRoleResponseDTO result = eventRoleService.updateEventRole(1L, updateDTO);

        // then
        assertThat(result).isNotNull();
        assertThat(eventRole.getEvent()).isEqualTo(newEvent);

        verify(eventRepository).findById(2L);
    }

    @Test
    void updateEventRole_UpdateGlobalRole_Success() {
        // given
        GlobalEventRole newGlobalRole = GlobalEventRole.builder().id(2L).title("Новая роль").build();
        updateDTO.setGlobalEventRoleId(2L);

        when(eventRoleRepository.findById(1L)).thenReturn(Optional.of(eventRole));
        when(globalEventRoleRepository.findById(2L)).thenReturn(Optional.of(newGlobalRole));
        when(eventRoleRepository.save(any(EventRole.class))).thenReturn(eventRole);
        when(eventRoleMapper.toResponseDto(eventRole)).thenReturn(responseDTO);

        // when
        EventRoleResponseDTO result = eventRoleService.updateEventRole(1L, updateDTO);

        // then
        assertThat(result).isNotNull();
        assertThat(eventRole.getGlobalEventRole()).isEqualTo(newGlobalRole);
    }

    @Test
    void updateEventRole_NotFound_ThrowsException() {
        // given
        when(eventRoleRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eventRoleService.updateEventRole(1L, updateDTO))
                .isInstanceOf(EventRoleDoesNotFoundException.class)
                .hasMessageContaining("Роль мероприятия не найдена");
    }

    // ==================== TESTS FOR deleteEventRole ====================

    @Test
    void deleteEventRole_Success() {
        // given
        when(eventRoleRepository.findById(1L)).thenReturn(Optional.of(eventRole));
        when(eventRoleRepository.save(any(EventRole.class))).thenReturn(eventRole);

        // when
        eventRoleService.deleteEventRole(1L);

        // then
        assertThat(eventRole.getDeleted()).isTrue();
        verify(eventRoleRepository).save(eventRole);
    }

    @Test
    void deleteEventRole_NotFound_ThrowsException() {
        // given
        when(eventRoleRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eventRoleService.deleteEventRole(1L))
                .isInstanceOf(EventRoleDoesNotFoundException.class)
                .hasMessageContaining("Роль мероприятия не найдена");
    }

    // ==================== TESTS FOR hardDeleteEventRole ====================

    @Test
    void hardDeleteEventRole_Success() {
        // given
        when(eventRoleRepository.findById(1L)).thenReturn(Optional.of(eventRole));
        doNothing().when(eventRoleRepository).delete(eventRole);

        // when
        eventRoleService.hardDeleteEventRole(1L);

        // then
        verify(eventRoleRepository).delete(eventRole);
    }

    @Test
    void hardDeleteEventRole_NotFound_ThrowsException() {
        // given
        when(eventRoleRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eventRoleService.hardDeleteEventRole(1L))
                .isInstanceOf(EventRoleDoesNotFoundException.class)
                .hasMessageContaining("Роль мероприятия не найдена");
    }

    // ==================== TESTS FOR getAllEventRoles ====================

    @Test
    void getAllEventRoles_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<EventRole> eventRolePage = new PageImpl<>(List.of(eventRole));

        when(eventRoleRepository.findAllWithFilters(
                any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(eventRolePage);
        when(eventRoleMapper.toResponseDto(eventRole)).thenReturn(responseDTO);

        // when
        Page<EventRoleResponseDTO> result = eventRoleService.getAllEventRoles(filterDTO, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);

        verify(eventRoleRepository).findAllWithFilters(
                eq(filterDTO.getId()),
                eq(filterDTO.getEventId()),
                eq(filterDTO.getGlobalEventRoleId()),
                eq(filterDTO.getDeleted()),
                eq(pageable));
    }

    @Test
    void getAllEventRoles_EmptyFilter_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<EventRole> eventRolePage = new PageImpl<>(List.of(eventRole));
        EventRoleFilterDTO emptyFilter = EventRoleFilterDTO.builder().build();

        when(eventRoleRepository.findAllWithFilters(
                any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(eventRolePage);
        when(eventRoleMapper.toResponseDto(eventRole)).thenReturn(responseDTO);

        // when
        Page<EventRoleResponseDTO> result = eventRoleService.getAllEventRoles(emptyFilter, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getAllEventRoles_EmptyResult_ReturnsEmptyPage() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<EventRole> emptyPage = new PageImpl<>(List.of());

        when(eventRoleRepository.findAllWithFilters(
                any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

        // when
        Page<EventRoleResponseDTO> result = eventRoleService.getAllEventRoles(filterDTO, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }
}
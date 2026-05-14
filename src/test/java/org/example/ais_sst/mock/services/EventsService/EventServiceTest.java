package org.example.ais_sst.mock.services.EventsService;

import org.example.ais_sst.dto.events.EventCreateDTO;
import org.example.ais_sst.dto.events.EventResponseDTO;
import org.example.ais_sst.dto.events.EventUpdateDTO;
import org.example.ais_sst.entity.Event;
import org.example.ais_sst.entity.EventOrganizer;
import org.example.ais_sst.entity.Role;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.exception.EventDoesNotExistException;
import org.example.ais_sst.exception.UnauthorizedException;
import org.example.ais_sst.exception.UserDoesNotExistException;
import org.example.ais_sst.mapper.EventMapper;
import org.example.ais_sst.repository.EventOrganizerRepository;
import org.example.ais_sst.repository.EventRepository;
import org.example.ais_sst.repository.UserRepository;
import org.example.ais_sst.service.eventService.EventPhotoService;
import org.example.ais_sst.service.eventService.EventService;
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

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventOrganizerRepository eventOrganizerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private EventPhotoService eventPhotoService;

    @InjectMocks
    private EventService eventService;

    private User creator;
    private User organizer;
    private User unauthorizedUser;
    private Event event;
    private EventCreateDTO createDTO;
    private EventUpdateDTO updateDTO;
    private EventResponseDTO responseDTO;
    private Role allowedRole;
    private Role disallowedRole;

    @BeforeEach
    void setUp() {
        allowedRole = Role.builder()
                .id(1L)
                .title("Administrator")
                .build();

        disallowedRole = Role.builder()
                .id(2L)
                .title("Activist")
                .build();

        creator = User.builder()
                .id(1L)
                .name("Иван")
                .surname("Иванов")
                .role(allowedRole)  // ✅ Роль установлена
                .build();

        organizer = User.builder()
                .id(2L)
                .name("Петр")
                .surname("Петров")
                .role(allowedRole)  // ✅ Роль установлена
                .build();

        unauthorizedUser = User.builder()
                .id(3L)
                .name("Сидор")
                .surname("Сидоров")
                .role(disallowedRole)  // ✅ Роль установлена
                .build();

        event = Event.builder()
                .id(1L)
                .title("Конференция")
                .description("Описание конференции")
                .dateOfEvent(LocalDate.of(2024, 12, 15))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(18, 0))
                .venue("Конференц-зал")
                .referenceToPosition("https://example.com")
                .isPublic(true)
                .isDraft(false)
                .isActive(true)
                .isCompleted(false)
                .photo("events/photo123.jpg")
                .eventCreator(creator)
                .build();

        createDTO = EventCreateDTO.builder()
                .title("Новая конференция")
                .description("Описание новой конференции")
                .dateOfEvent(LocalDate.of(2024, 12, 20))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(18, 0))
                .venue("Новый зал")
                .referenceToPosition("https://newevent.com")
                .isPublic(true)
                .photo("base64photo...")
                .organizerIds(List.of(2L))
                .build();

        updateDTO = EventUpdateDTO.builder()
                .title("Обновленная конференция")
                .description("Новое описание")
                .isDraft(false)
                .photo("new-base64-photo...")
                .organizerIds(List.of(2L, 3L))
                .build();

        responseDTO = EventResponseDTO.builder()
                .id(1L)
                .title("Конференция")
                .description("Описание конференции")
                .build();
    }

    // ==================== TESTS FOR getEventById ====================

    @Test
    void getEventById_Success() throws IOException {
        // given
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventMapper.toResponseDto(event)).thenReturn(responseDTO);
        when(eventPhotoService.getPhotoAsBase64("events/photo123.jpg")).thenReturn("base64-photo-data");

        // when
        EventResponseDTO result = eventService.getEventById(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPhoto()).isEqualTo("base64-photo-data");

        verify(eventRepository).findById(1L);
        verify(eventPhotoService).getPhotoAsBase64("events/photo123.jpg");
    }

    @Test
    void getEventById_EventNotFound_ThrowsException() {
        // given
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eventService.getEventById(1L))
                .isInstanceOf(EventDoesNotExistException.class)
                .hasMessageContaining("Мероприятие с id 1 не найдено");
    }

    @Test
    void getEventById_WithoutPhoto_ReturnsNullPhoto() throws IOException {
        // given
        event.setPhoto(null);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventMapper.toResponseDto(event)).thenReturn(responseDTO);

        // when
        EventResponseDTO result = eventService.getEventById(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPhoto()).isNull();
        verify(eventPhotoService, never()).getPhotoAsBase64(any());
    }

    // ==================== TESTS FOR createEvent ====================

    @Test
    void createEvent_Success() throws IOException {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        // ДОБАВЬТЕ МОК ДЛЯ ОРГАНИЗАТОРА
        when(userRepository.findById(2L)).thenReturn(Optional.of(organizer));
        when(eventMapper.toEntity(createDTO)).thenReturn(event);
        when(eventPhotoService.savePhotoFromBase64("base64photo...")).thenReturn("events/new-photo.jpg");
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toResponseDto(event)).thenReturn(responseDTO);

        // when
        EventResponseDTO result = eventService.createEvent(createDTO, 1L);

        // then
        assertThat(result).isNotNull();

        verify(userRepository).findById(1L);
        verify(userRepository).findById(2L);
        verify(eventPhotoService).savePhotoFromBase64("base64photo...");
        verify(eventRepository).save(any(Event.class));
        verify(eventOrganizerRepository).save(any(EventOrganizer.class));
    }

    @Test
    void createEvent_UserNotFound_ThrowsException() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eventService.createEvent(createDTO, 1L))
                .isInstanceOf(UserDoesNotExistException.class)
                .hasMessageContaining("Пользователь не найден");
    }

    @Test
    void createEvent_UnauthorizedRole_ThrowsException() {
        // given
        when(userRepository.findById(3L)).thenReturn(Optional.of(unauthorizedUser));

        // when & then
        assertThatThrownBy(() -> eventService.createEvent(createDTO, 3L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("У вас нет прав для создания мероприятий");
    }

    @Test
    void createEvent_WithNullPhoto_Success() throws IOException {
        // given
        createDTO.setPhoto(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        // ДОБАВЬТЕ МОК ДЛЯ ОРГАНИЗАТОРА
        when(userRepository.findById(2L)).thenReturn(Optional.of(organizer));
        when(eventMapper.toEntity(createDTO)).thenReturn(event);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toResponseDto(event)).thenReturn(responseDTO);

        // when
        EventResponseDTO result = eventService.createEvent(createDTO, 1L);

        // then
        assertThat(result).isNotNull();
        verify(eventPhotoService, never()).savePhotoFromBase64(any());
        verify(userRepository).findById(2L);
    }

    // ==================== TESTS FOR updateEvent ====================

    @Test
    void updateEvent_Success() throws IOException {
        // given
        when(eventRepository.existsByIdAndEventCreatorId(1L, 1L)).thenReturn(true);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(2L)).thenReturn(Optional.of(organizer));

        // ✅ Исправлено: добавляем роль пользователю
        User newOrganizer = User.builder()
                .id(3L)
                .name("Новый")
                .surname("Организатор")
                .role(allowedRole)  // 👈 ДОБАВИТЬ РОЛЬ
                .build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(newOrganizer));

        when(eventPhotoService.savePhotoFromBase64("new-base64-photo...")).thenReturn("events/updated-photo.jpg");
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toResponseDto(event)).thenReturn(responseDTO);

        // when
        EventResponseDTO result = eventService.updateEvent(1L, updateDTO, 1L);

        // then
        assertThat(result).isNotNull();
        verify(eventRepository).findById(1L);
        verify(eventPhotoService).deletePhoto("events/photo123.jpg");
        verify(eventPhotoService).savePhotoFromBase64("new-base64-photo...");
        verify(eventOrganizerRepository).deleteByEventId(1L);
        verify(eventOrganizerRepository, atLeastOnce()).save(any(EventOrganizer.class));
    }

    @Test
    void updateEvent_UserNotCreator_ThrowsException() {
        // given
        when(eventRepository.existsByIdAndEventCreatorId(1L, 2L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> eventService.updateEvent(1L, updateDTO, 2L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Вы не являетесь создателем этого мероприятия");
    }

    @Test
    void updateEvent_EventNotFound_ThrowsException() {
        // given
        when(eventRepository.existsByIdAndEventCreatorId(1L, 1L)).thenReturn(true);
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eventService.updateEvent(1L, updateDTO, 1L))
                .isInstanceOf(EventDoesNotExistException.class);
    }

    @Test
    void updateEvent_WithoutPhoto_DoesNotDeleteExistingPhoto() throws IOException {
        // given
        updateDTO.setPhoto(null);
        when(eventRepository.existsByIdAndEventCreatorId(1L, 1L)).thenReturn(true);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(2L)).thenReturn(Optional.of(organizer));

        // ✅ Исправлено: добавляем роль пользователю
        User newOrganizer = User.builder()
                .id(3L)
                .name("Новый")
                .surname("Организатор")
                .role(allowedRole)  // 👈 ДОБАВИТЬ РОЛЬ
                .build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(newOrganizer));

        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toResponseDto(event)).thenReturn(responseDTO);

        // when
        eventService.updateEvent(1L, updateDTO, 1L);

        // then
        verify(eventPhotoService, never()).deletePhoto(any());
        verify(eventPhotoService, never()).savePhotoFromBase64(any());
    }

    // ==================== TESTS FOR completeEvent ====================

    @Test
    void completeEvent_Success() {
        // given
        when(eventRepository.existsByIdAndEventCreatorId(1L, 1L)).thenReturn(true);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toResponseDto(event)).thenReturn(responseDTO);

        // when
        EventResponseDTO result = eventService.completeEvent(1L, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(event.getIsCompleted()).isTrue();
        assertThat(event.getIsActive()).isFalse();
        verify(eventRepository).save(event);
    }

    @Test
    void completeEvent_AlreadyCompleted_ThrowsException() {
        // given
        event.setIsCompleted(true);
        when(eventRepository.existsByIdAndEventCreatorId(1L, 1L)).thenReturn(true);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        // when & then
        assertThatThrownBy(() -> eventService.completeEvent(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Мероприятие уже завершено");
    }

    @Test
    void completeEvent_UserNotCreator_ThrowsException() {
        // given
        when(eventRepository.existsByIdAndEventCreatorId(1L, 2L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> eventService.completeEvent(1L, 2L))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ==================== TESTS FOR addOrganizer ====================

    @Test
    void addOrganizer_Success() {
        // given
        when(eventRepository.existsByIdAndEventCreatorId(1L, 1L)).thenReturn(true);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(2L)).thenReturn(Optional.of(organizer));
        when(eventOrganizerRepository.existsByEventIdAndUserId(1L, 2L)).thenReturn(false);
        when(eventMapper.toResponseDto(event)).thenReturn(responseDTO);

        // when
        EventResponseDTO result = eventService.addOrganizer(1L, 2L, 1L);

        // then
        assertThat(result).isNotNull();
        verify(eventOrganizerRepository).save(any(EventOrganizer.class));
    }

    @Test
    void addOrganizer_OrganizerAlreadyExists_ThrowsException() {
        // given
        when(eventRepository.existsByIdAndEventCreatorId(1L, 1L)).thenReturn(true);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(2L)).thenReturn(Optional.of(organizer));
        when(eventOrganizerRepository.existsByEventIdAndUserId(1L, 2L)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> eventService.addOrganizer(1L, 2L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Пользователь уже является организатором");
    }

    // ==================== TESTS FOR removeOrganizer ====================

    @Test
    void removeOrganizer_Success() {
        // given
        when(eventRepository.existsByIdAndEventCreatorId(1L, 1L)).thenReturn(true);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventOrganizerRepository.countByEventId(1L)).thenReturn(2L);
        when(eventMapper.toResponseDto(event)).thenReturn(responseDTO);

        // when
        EventResponseDTO result = eventService.removeOrganizer(1L, 2L, 1L);

        // then
        assertThat(result).isNotNull();
        verify(eventOrganizerRepository).deleteByEventIdAndUserId(1L, 2L);
    }

    @Test
    void removeOrganizer_LastOrganizer_ThrowsException() {
        // given
        when(eventRepository.existsByIdAndEventCreatorId(1L, 1L)).thenReturn(true);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventOrganizerRepository.countByEventId(1L)).thenReturn(1L);

        // when & then
        assertThatThrownBy(() -> eventService.removeOrganizer(1L, 2L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Нельзя удалить единственного организатора мероприятия");
    }

    // ==================== TESTS FOR deleteEvent ====================

    @Test
    void deleteEvent_Success() {
        // given
        when(eventRepository.existsByIdAndEventCreatorId(1L, 1L)).thenReturn(true);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        // when
        eventService.deleteEvent(1L, 1L);

        // then
        assertThat(event.getIsActive()).isFalse();
        verify(eventRepository).save(event);
    }

    // ==================== TESTS FOR getAllEvents ====================

    @Test
    void getAllEvents_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> eventPage = new PageImpl<>(List.of(event));
        when(eventRepository.findByIsActiveTrue(pageable)).thenReturn(eventPage);
        when(eventMapper.toResponseDto(any(Event.class))).thenReturn(responseDTO);

        // when
        Page<EventResponseDTO> result = eventService.getAllEvents(pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(eventRepository).findByIsActiveTrue(pageable);
    }

    // ==================== TESTS FOR getEventsByCreator ====================

    @Test
    void getEventsByCreator_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> eventPage = new PageImpl<>(List.of(event));
        when(eventRepository.findByEventCreatorId(1L, pageable)).thenReturn(eventPage);
        when(eventMapper.toResponseDto(any(Event.class))).thenReturn(responseDTO);

        // when
        Page<EventResponseDTO> result = eventService.getEventsByCreator(1L, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(eventRepository).findByEventCreatorId(1L, pageable);
    }

    // ==================== TESTS FOR getEventsWithFilters ====================

    @Test
    void getEventsWithFilters_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> eventPage = new PageImpl<>(List.of(event));
        when(eventRepository.findAllWithFilters(any(), any(), any(), any(), any(), any(), any(), any(), any(), eq(pageable)))
                .thenReturn(eventPage);
        when(eventMapper.toResponseDto(any(Event.class))).thenReturn(responseDTO);

        // when
        Page<EventResponseDTO> result = eventService.getEventsWithFilters(
                "Конференция", "Зал", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31),
                true, false, false, true, 1L, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(eventRepository).findAllWithFilters(any(), any(), any(), any(), any(), any(), any(), any(), any(), eq(pageable));
    }

    // ==================== TEST FOR PHOTO SERVICE ERROR HANDLING ====================

    @Test
    void createEvent_PhotoSaveFails_ThrowsException() throws IOException {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(eventMapper.toEntity(createDTO)).thenReturn(event);
        when(eventPhotoService.savePhotoFromBase64("base64photo...")).thenThrow(new IOException("IO Error"));

        // when & then
        assertThatThrownBy(() -> eventService.createEvent(createDTO, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ошибка при сохранении фото");

        verify(eventRepository, never()).save(any());
    }
}
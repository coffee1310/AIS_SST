//package org.example.ais_sst.mock.controllers;
//
//import org.example.ais_sst.controller.EventController;
//import org.example.ais_sst.dto.events.EventCreateDTO;
//import org.example.ais_sst.dto.events.EventFilterDTO;
//import org.example.ais_sst.dto.events.EventResponseDTO;
//import org.example.ais_sst.dto.events.EventUpdateDTO;
//import org.example.ais_sst.entity.CustomUserDetails;
//import org.example.ais_sst.service.eventService.EventService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.*;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//
//import java.time.LocalDate;
//import java.time.LocalTime;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class EventControllerTest {
//
//    @Mock
//    private EventService eventService;
//
//    @Mock
//    private CustomUserDetails userDetails;
//
//    @InjectMocks
//    private EventController eventController;
//
//    private EventCreateDTO createDTO;
//    private EventUpdateDTO updateDTO;
//    private EventResponseDTO responseDTO;
//    private Page<EventResponseDTO> responsePage;
//
//    @BeforeEach
//    void setUp() {
//        createDTO = EventCreateDTO.builder()
//                .title("Конференция")
//                .description("Описание конференции")
//                .dateOfEvent(LocalDate.of(2024, 12, 15))
//                .startTime(LocalTime.of(10, 0))
//                .endTime(LocalTime.of(18, 0))
//                .venue("Конференц-зал")
//                .referenceToPosition("https://example.com")
//                .isPublic(true)
//                .photo("base64photo...")
//                .organizerIds(List.of(2L, 3L))
//                .build();
//
//        updateDTO = EventUpdateDTO.builder()
//                .title("Обновленная конференция")
//                .description("Новое описание")
//                .build();
//
//        responseDTO = EventResponseDTO.builder()
//                .id(1L)
//                .title("Конференция")
//                .description("Описание конференции")
//                .build();
//
//        responsePage = new PageImpl<>(List.of(responseDTO));
//    }
//
//    // ==================== TESTS FOR createEvent ====================
//
//    @Test
//    void createEvent_Success() {
//        // given
//        when(userDetails.getId()).thenReturn(1L);
//        when(eventService.createEvent(any(EventCreateDTO.class), eq(1L))).thenReturn(responseDTO);
//
//        // when
//        ResponseEntity<EventResponseDTO> response = eventController.createEvent(createDTO, userDetails);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
//        assertThat(response.getBody()).isEqualTo(responseDTO);
//
//        verify(eventService).createEvent(createDTO, 1L);
//    }
//
//    @Test
//    void createEvent_WithNullUserDetails_ThrowsException() {
//        // when & then
//        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> {
//            eventController.createEvent(createDTO, null);
//        });
//    }
//
//    // ==================== TESTS FOR getEventById ====================
//
//    @Test
//    void getEventById_Success() {
//        // given
//        when(eventService.getEventById(1L)).thenReturn(responseDTO);
//
//        // when
//        ResponseEntity<EventResponseDTO> response = eventController.getEventById(1L);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(response.getBody()).isEqualTo(responseDTO);
//
//        verify(eventService).getEventById(1L);
//    }
//
//    @Test
//    void getEventById_WithNonExistentId_ThrowsException() {
//        // given
//        when(eventService.getEventById(999L)).thenThrow(new RuntimeException("Event not found"));
//
//        // when & then
//        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
//            eventController.getEventById(999L);
//        });
//    }
//
//    // ==================== TESTS FOR getEvents ====================
//
//    @Test
//    void getEvents_Success() {
//        // given
//        when(userDetails.getId()).thenReturn(1L);
//        when(eventService.getEventsWithFilters(any(EventFilterDTO.class), any(Pageable.class)))
//                .thenReturn(responsePage);
//
//        // when - Добавлен параметр id (null) в начало
//        ResponseEntity<Page<EventResponseDTO>> response = eventController.getEvents(
//                null,                    // id
//                "Конференция",           // title
//                "Зал",                   // venue
//                "Описание",              // description
//                "https://example.com",   // referenceToPosition
//                LocalDate.of(2024, 1, 1), // dateFrom
//                LocalDate.of(2024, 12, 31), // dateTo
//                LocalTime.of(9, 0),      // startTimeFrom
//                LocalTime.of(20, 0),     // startTimeTo
//                LocalTime.of(9, 0),      // endTimeFrom
//                LocalTime.of(20, 0),     // endTimeTo
//                true,                    // isPublic
//                false,                   // isDraft
//                false,                   // isCompleted
//                true,                    // isActive
//                null,                    // creatorId
//                0,                       // page
//                10,                      // size
//                "id",                    // sortBy
//                "DESC",                  // sortDirection
//                userDetails              // userDetails
//        );
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(response.getBody()).isNotNull();
//        assertThat(response.getBody().getContent()).hasSize(1);
//
//        verify(eventService).getEventsWithFilters(any(EventFilterDTO.class), any(Pageable.class));
//    }
//
//    @Test
//    void getEvents_WithCreatorId_OverridesUserDetails() {
//        // given
//        when(eventService.getEventsWithFilters(any(EventFilterDTO.class), any(Pageable.class)))
//                .thenReturn(responsePage);
//
//        // when - Добавлен параметр id (null) в начало
//        ResponseEntity<Page<EventResponseDTO>> response = eventController.getEvents(
//                null, null, null, null, null, null, null, null, null, null, null,
//                null, null, null, null, 5L, 0, 10, "id", "DESC", userDetails);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//
//        verify(eventService).getEventsWithFilters(any(EventFilterDTO.class), any(Pageable.class));
//        verify(userDetails, never()).getId();
//    }
//
//    @Test
//    void getEvents_WithIdFilter() {
//        // given
//        when(eventService.getEventsWithFilters(any(EventFilterDTO.class), any(Pageable.class)))
//                .thenReturn(responsePage);
//
//        // when - Передаем конкретный id
//        ResponseEntity<Page<EventResponseDTO>> response = eventController.getEvents(
//                5L,                      // id
//                null, null, null, null, null, null, null, null, null, null,
//                null, null, null, null, null, 0, 10, "id", "DESC", userDetails);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//
//        verify(eventService).getEventsWithFilters(
//                argThat(filter -> filter.getId() != null && filter.getId() == 5L),
//                any(Pageable.class));
//    }
//
//    @Test
//    void getEvents_WithoutUserDetails_SetsCreatorIdToNull() {
//        // given
//        when(eventService.getEventsWithFilters(any(EventFilterDTO.class), any(Pageable.class)))
//                .thenReturn(responsePage);
//
//        // when - Добавлен параметр id (null) в начало
//        ResponseEntity<Page<EventResponseDTO>> response = eventController.getEvents(
//                null, null, null, null, null, null, null, null, null, null, null,
//                null, null, null, null, null, 0, 10, "id", "DESC", null);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//
//        verify(eventService).getEventsWithFilters(any(EventFilterDTO.class), any(Pageable.class));
//    }
//
//    @Test
//    void getEvents_WithDefaultPagination() {
//        // given
//        when(userDetails.getId()).thenReturn(1L);
//        when(eventService.getEventsWithFilters(any(EventFilterDTO.class), any(Pageable.class)))
//                .thenReturn(responsePage);
//
//        // when - Добавлен параметр id (null) в начало
//        eventController.getEvents(
//                null, null, null, null, null, null, null, null, null, null, null,
//                null, null, null, null, null, 0, 10, "id", "DESC", userDetails);
//
//        // then
//        verify(eventService).getEventsWithFilters(
//                argThat(filter -> filter != null),
//                argThat(pageable -> pageable.getPageNumber() == 0 &&
//                        pageable.getPageSize() == 10 &&
//                        pageable.getSort().getOrderFor("id") != null));
//    }
//
//    @Test
//    void getEvents_WithCustomSorting() {
//        // given
//        when(userDetails.getId()).thenReturn(1L);
//        when(eventService.getEventsWithFilters(any(EventFilterDTO.class), any(Pageable.class)))
//                .thenReturn(responsePage);
//
//        // when - Добавлен параметр id (null) в начало
//        eventController.getEvents(
//                null, null, null, null, null, null, null, null, null, null, null,
//                null, null, null, null, null, 2, 25, "title", "ASC", userDetails);
//
//        // then
//        verify(eventService).getEventsWithFilters(
//                any(EventFilterDTO.class),
//                argThat(pageable -> pageable.getPageNumber() == 2 &&
//                        pageable.getPageSize() == 25 &&
//                        pageable.getSort().getOrderFor("title") != null &&
//                        pageable.getSort().getOrderFor("title").isAscending()));
//    }
//
//    @Test
//    void getEvents_WithAllFilters() {
//        // given
//        when(userDetails.getId()).thenReturn(1L);
//        when(eventService.getEventsWithFilters(any(EventFilterDTO.class), any(Pageable.class)))
//                .thenReturn(responsePage);
//
//        // when - Добавлен параметр id (null) в начало
//        eventController.getEvents(
//                null,                    // id
//                "Test Title",           // title
//                "Test Venue",           // venue
//                "Test Description",     // description
//                "https://test.com",     // referenceToPosition
//                LocalDate.of(2024, 1, 1),  // dateFrom
//                LocalDate.of(2024, 12, 31), // dateTo
//                LocalTime.of(10, 0),    // startTimeFrom
//                LocalTime.of(18, 0),    // startTimeTo
//                LocalTime.of(10, 0),    // endTimeFrom
//                LocalTime.of(18, 0),    // endTimeTo
//                true,                   // isPublic
//                false,                  // isDraft
//                false,                  // isCompleted
//                true,                   // isActive
//                10L,                    // creatorId
//                0, 20, "dateOfEvent", "ASC", userDetails);
//
//        // then
//        verify(eventService).getEventsWithFilters(
//                argThat(filter ->
//                        "Test Title".equals(filter.getTitle()) &&
//                                "Test Venue".equals(filter.getVenue()) &&
//                                "Test Description".equals(filter.getDescription()) &&
//                                "https://test.com".equals(filter.getReferenceToPosition()) &&
//                                filter.getDateFrom().equals(LocalDate.of(2024, 1, 1)) &&
//                                filter.getDateTo().equals(LocalDate.of(2024, 12, 31)) &&
//                                filter.getStartTimeFrom().equals(LocalTime.of(10, 0)) &&
//                                filter.getStartTimeTo().equals(LocalTime.of(18, 0)) &&
//                                filter.getEndTimeFrom().equals(LocalTime.of(10, 0)) &&
//                                filter.getEndTimeTo().equals(LocalTime.of(18, 0)) &&
//                                filter.getIsPublic() == true &&
//                                filter.getIsDraft() == false &&
//                                filter.getIsCompleted() == false &&
//                                filter.getIsActive() == true &&
//                                filter.getCreatorId().equals(10L)
//                ),
//                any(Pageable.class));
//    }
//
//    // ==================== TESTS FOR getMyEvents ====================
//
//    @Test
//    void getMyEvents_Success() {
//        // given
//        when(userDetails.getId()).thenReturn(1L);
//        when(eventService.getEventsByCreator(eq(1L), any(Pageable.class))).thenReturn(responsePage);
//
//        // when
//        ResponseEntity<Page<EventResponseDTO>> response = eventController.getMyEvents(
//                userDetails, 0, 10, "id", "DESC");
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(response.getBody()).isNotNull();
//        assertThat(response.getBody().getContent()).hasSize(1);
//
//        verify(eventService).getEventsByCreator(eq(1L), any(Pageable.class));
//    }
//
//    @Test
//    void getMyEvents_WithCustomPagination() {
//        // given
//        when(userDetails.getId()).thenReturn(1L);
//        when(eventService.getEventsByCreator(eq(1L), any(Pageable.class))).thenReturn(responsePage);
//
//        // when
//        ResponseEntity<Page<EventResponseDTO>> response = eventController.getMyEvents(
//                userDetails, 2, 20, "title", "ASC");
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//
//        verify(eventService).getEventsByCreator(eq(1L), eq(PageRequest.of(2, 20, Sort.by("title").ascending())));
//    }
//
//    @Test
//    void getMyEvents_WithoutUserDetails_ThrowsException() {
//        // when & then
//        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> {
//            eventController.getMyEvents(null, 0, 10, "id", "DESC");
//        });
//    }
//
//    // ==================== TESTS FOR updateEvent ====================
//
//    @Test
//    void updateEvent_Success() {
//        // given
//        when(userDetails.getId()).thenReturn(1L);
//        when(eventService.updateEvent(eq(1L), any(EventUpdateDTO.class), eq(1L))).thenReturn(responseDTO);
//
//        // when
//        ResponseEntity<EventResponseDTO> response = eventController.updateEvent(1L, updateDTO, userDetails);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(response.getBody()).isEqualTo(responseDTO);
//
//        verify(eventService).updateEvent(eq(1L), any(EventUpdateDTO.class), eq(1L));
//    }
//
//    // ==================== TESTS FOR addOrganizer ====================
//
//    @Test
//    void addOrganizer_Success() {
//        // given
//        when(userDetails.getId()).thenReturn(1L);
//        when(eventService.addOrganizer(eq(1L), eq(2L), eq(1L))).thenReturn(responseDTO);
//
//        // when
//        ResponseEntity<EventResponseDTO> response = eventController.addOrganizer(1L, 2L, userDetails);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(response.getBody()).isEqualTo(responseDTO);
//
//        verify(eventService).addOrganizer(eq(1L), eq(2L), eq(1L));
//    }
//
//    // ==================== TESTS FOR removeOrganizer ====================
//
//    @Test
//    void removeOrganizer_Success() {
//        // given
//        when(userDetails.getId()).thenReturn(1L);
//        when(eventService.removeOrganizer(eq(1L), eq(2L), eq(1L))).thenReturn(responseDTO);
//
//        // when
//        ResponseEntity<EventResponseDTO> response = eventController.removeOrganizer(1L, 2L, userDetails);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(response.getBody()).isEqualTo(responseDTO);
//
//        verify(eventService).removeOrganizer(eq(1L), eq(2L), eq(1L));
//    }
//
//    // ==================== TESTS FOR deleteEvent ====================
//
//    @Test
//    void deleteEvent_Success() {
//        // given
//        when(userDetails.getId()).thenReturn(1L);
//        doNothing().when(eventService).deleteEvent(eq(1L), eq(1L));
//
//        // when
//        ResponseEntity<Void> response = eventController.deleteEvent(1L, userDetails);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
//        assertThat(response.getBody()).isNull();
//
//        verify(eventService).deleteEvent(eq(1L), eq(1L));
//    }
//
//    // ==================== TESTS FOR completeEvent ====================
//
//    @Test
//    void completeEvent_Success() {
//        // given
//        when(userDetails.getId()).thenReturn(1L);
//        when(eventService.completeEvent(eq(1L), eq(1L))).thenReturn(responseDTO);
//
//        // when
//        ResponseEntity<EventResponseDTO> response = eventController.completeEvent(1L, userDetails);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(response.getBody()).isEqualTo(responseDTO);
//
//        verify(eventService).completeEvent(eq(1L), eq(1L));
//    }
//}
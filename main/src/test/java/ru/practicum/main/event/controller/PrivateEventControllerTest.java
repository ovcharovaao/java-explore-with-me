package ru.practicum.main.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.event.dto.*;
import ru.practicum.main.event.service.EventService;
import ru.practicum.main.location.dto.LocationDto;
import ru.practicum.main.request.dto.ParticipationRequestDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.category.dto.CategoryDto;
import ru.practicum.main.request.model.RequestStatus;
import ru.practicum.main.user.dto.UserShortDto;
import ru.practicum.main.event.model.EventState;


import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PrivateEventController.class)
class PrivateEventControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @Autowired
    private ObjectMapper objectMapper;

    private EventFullDto createEventFullDto(Long eventId, Long userId, Long categoryId, String title) {
        return EventFullDto.builder()
                .id(eventId)
                .title(title)
                .annotation("Annotation for " + title + " that is long enough")
                .description("Description for " + title + " that is long enough")
                .category(new CategoryDto(categoryId, "Category Name"))
                .initiator(UserShortDto.builder().id(userId).name("User Name").build())
                .eventDate(LocalDateTime.now().plusDays(5))
                .createdOn(LocalDateTime.now())
                .publishedOn(LocalDateTime.now().plusDays(1))
                .location(new LocationDto(10.0f, 20.0f))
                .paid(false)
                .participantLimit(0)
                .requestModeration(false)
                .confirmedRequests(0L)
                .views(0L)
                .state(EventState.PUBLISHED)
                .build();
    }

    private EventShortDto createEventShortDto(Long eventId, Long userId, String title) {
        return EventShortDto.builder()
                .id(eventId)
                .title(title)
                .annotation("Short Annotation for " + title + " that is long enough")
                .category(new CategoryDto(10L, "Category Name"))
                .initiator(UserShortDto.builder().id(userId).name("User Name").build())
                .eventDate(LocalDateTime.now().plusDays(5))
                .paid(false)
                .views(0L)
                .confirmedRequests(0L)
                .build();
    }

    private NewEventDto createNewEventDto(Long categoryId, String title) {
        return NewEventDto.builder()
                .title(title)
                .annotation("New event annotation must be at least 20 characters long for validation")
                .description("New event description must be at least 20 characters long for validation")
                .category(categoryId)
                .eventDate(LocalDateTime.now().plusDays(10))
                .location(new LocationDto(10.0f, 20.0f))
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .build();
    }

    private UpdateEventUserRequest createUpdateEventUserRequest(String title) {
        return UpdateEventUserRequest.builder()
                .title(title)
                .annotation("Updated event annotation must be at least 20 characters long for validation")
                .description("Updated event description must be at least 20 characters long for validation")
                .eventDate(LocalDateTime.now().plusDays(15))
                .location(new LocationDto(11.0f, 21.0f))
                .build();
    }

    private ParticipationRequestDto createParticipationRequestDto(Long requestId, Long eventId, Long requesterId,
                                                                  RequestStatus status) {
        return ParticipationRequestDto.builder()
                .id(requestId)
                .created(LocalDateTime.now())
                .event(eventId)
                .requester(requesterId)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("POST /users/{userId}/events - успешное создание события")
    void shouldCreateEvent() throws Exception {
        Long userId = 1L;
        Long categoryId = 10L;
        NewEventDto newEventDto = createNewEventDto(categoryId, "New Test Event");
        EventFullDto createdEventDto = createEventFullDto(1L, userId, categoryId, "New Test Event");

        Mockito.when(eventService.createEvent(eq(userId), any(NewEventDto.class))).thenReturn(createdEventDto);

        mockMvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEventDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(createdEventDto.getId()))
                .andExpect(jsonPath("$.title").value(createdEventDto.getTitle()))
                .andExpect(jsonPath("$.initiator.id").value(createdEventDto.getInitiator().getId()));
    }

    @Test
    @DisplayName("POST /users/{userId}/events - ошибка валидации при создании события (пустое название)")
    void shouldFailValidationWhenCreatingEventWithEmptyTitle() throws Exception {
        Long userId = 1L;
        Long categoryId = 10L;
        NewEventDto invalidEventDto = createNewEventDto(categoryId, "Valid Title For Now");
        invalidEventDto.setTitle("");

        mockMvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEventDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users/{userId}/events - ошибка валидации при создании события (короткое название)")
    void shouldFailValidationWhenCreatingEventWithTooShortTitle() throws Exception {
        Long userId = 1L;
        Long categoryId = 10L;
        NewEventDto invalidEventDto = createNewEventDto(categoryId, "ab");

        mockMvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEventDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users/{userId}/events - ошибка валидации при создании события (дата в прошлом)")
    void shouldFailValidationWhenCreatingEventWithPastDate() throws Exception {
        Long userId = 1L;
        Long categoryId = 10L;
        NewEventDto invalidEventDto = createNewEventDto(categoryId, "Event in past");
        invalidEventDto.setEventDate(LocalDateTime.now().minusDays(1));

        mockMvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEventDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users/{userId}/events - пользователь или категория не найдены при создании события")
    void shouldReturnNotFoundWhenCreatingEventUserOrCategoryNotFound() throws Exception {
        Long userId = 99L;
        Long categoryId = 10L;
        NewEventDto newEventDto = createNewEventDto(categoryId, "New Test Event");

        Mockito.when(eventService.createEvent(eq(userId), any(NewEventDto.class)))
                .thenThrow(new NotFoundException("Пользователь с ID " + userId + " не найден."));

        mockMvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEventDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /users/{userId}/events - успешное получение событий инициатора с пагинацией по умолчанию")
    void shouldGetEventsByInitiatorWithDefaultPagination() throws Exception {
        Long userId = 1L;
        EventShortDto event1 = createEventShortDto(1L, userId, "Event 1");
        EventShortDto event2 = createEventShortDto(2L, userId, "Event 2");
        List<EventShortDto> events = List.of(event1, event2);

        Mockito.when(eventService.getEventsByInitiator(eq(userId), eq(0), eq(10))).thenReturn(events);

        mockMvc.perform(get("/users/{userId}/events", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(event1.getId()))
                .andExpect(jsonPath("$[1].title").value(event2.getTitle()));
    }

    @Test
    @DisplayName("GET /users/{userId}/events - успешное получение событий инициатора с кастомной пагинацией")
    void shouldGetEventsByInitiatorWithCustomPagination() throws Exception {
        Long userId = 1L;
        EventShortDto event1 = createEventShortDto(1L, userId, "Event 1");
        List<EventShortDto> events = List.of(event1);

        Mockito.when(eventService.getEventsByInitiator(eq(userId), eq(5), eq(1))).thenReturn(events);

        mockMvc.perform(get("/users/{userId}/events", userId)
                        .param("from", "5")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(event1.getId()));
    }

    @Test
    @DisplayName("GET /users/{userId}/events - пользователь не найден при получении событий")
    void shouldReturnNotFoundWhenGettingEventsByInitiatorUserNotFound() throws Exception {
        Long userId = 99L;

        Mockito.when(eventService.getEventsByInitiator(eq(userId), anyInt(), anyInt()))
                .thenThrow(new NotFoundException("Пользователь с ID " + userId + " не найден."));

        mockMvc.perform(get("/users/{userId}/events", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /users/{userId}/events/{eventId} - успешное получение полного события по ID и инициатору")
    void shouldGetEventByIdAndInitiator() throws Exception {
        Long userId = 1L;
        Long eventId = 1L;
        EventFullDto eventFullDto = createEventFullDto(eventId, userId, 10L, "Specific Event");

        Mockito.when(eventService.getEventByIdAndInitiator(eq(userId), eq(eventId))).thenReturn(eventFullDto);

        mockMvc.perform(get("/users/{userId}/events/{eventId}", userId, eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.title").value("Specific Event"))
                .andExpect(jsonPath("$.initiator.id").value(userId));
    }

    @Test
    @DisplayName("GET /users/{userId}/events/{eventId} - событие или пользователь не найдены")
    void shouldReturnNotFoundWhenGettingEventByIdAndInitiatorEventOrUserNotFound() throws Exception {
        Long userId = 1L;
        Long eventId = 99L;

        Mockito.when(eventService.getEventByIdAndInitiator(eq(userId), eq(eventId)))
                .thenThrow(new NotFoundException("Событие с ID " + eventId + " не найдено для пользователя " + userId));

        mockMvc.perform(get("/users/{userId}/events/{eventId}", userId, eventId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /users/{userId}/events/{eventId} - успешное обновление события инициатором")
    void shouldUpdateEventByInitiator() throws Exception {
        Long userId = 1L;
        Long eventId = 1L;
        UpdateEventUserRequest updateRequest = createUpdateEventUserRequest("Updated Event Title");
        EventFullDto updatedEventDto = createEventFullDto(eventId, userId, 10L, "Updated Event Title");

        Mockito.when(eventService.updateEventByInitiator(eq(userId), eq(eventId), any(UpdateEventUserRequest.class)))
                .thenReturn(updatedEventDto);

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.title").value("Updated Event Title"));
    }

    @Test
    @DisplayName("PATCH /users/{userId}/events/{eventId} - ошибка валидации при обновлении события (пустое название)")
    void shouldFailValidationWhenUpdatingEventWithEmptyTitle() throws Exception {
        Long userId = 1L;
        Long eventId = 1L;
        UpdateEventUserRequest invalidUpdateRequest = createUpdateEventUserRequest("Valid Title");
        invalidUpdateRequest.setTitle("");

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUpdateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /users/{userId}/events/{eventId} - ошибка валидации при обновлении события (дата в прошлом)")
    void shouldFailValidationWhenUpdatingEventWithPastDate() throws Exception {
        Long userId = 1L;
        Long eventId = 1L;
        UpdateEventUserRequest invalidUpdateRequest = createUpdateEventUserRequest("Event in past");
        invalidUpdateRequest.setEventDate(LocalDateTime.now().minusDays(1));

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUpdateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /users/{userId}/events/{eventId} - событие или пользователь не найдены при обновлении")
    void shouldReturnNotFoundWhenUpdatingEventEventOrUserNotFound() throws Exception {
        Long userId = 1L;
        Long eventId = 99L;
        UpdateEventUserRequest updateRequest = createUpdateEventUserRequest("Updated Event Title");

        Mockito.when(eventService.updateEventByInitiator(eq(userId), eq(eventId), any(UpdateEventUserRequest.class)))
                .thenThrow(new NotFoundException("Событие с ID " + eventId + " не найдено."));

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /users/{userId}/events/{eventId}/requests - успешное получение запросов на участие в событии")
    void shouldGetEventRequests() throws Exception {
        Long userId = 1L;
        Long eventId = 1L;
        ParticipationRequestDto request1 = createParticipationRequestDto(10L, eventId, 2L,
                RequestStatus.PENDING);
        ParticipationRequestDto request2 = createParticipationRequestDto(11L, eventId, 3L,
                RequestStatus.CONFIRMED);
        List<ParticipationRequestDto> requests = List.of(request1, request2);

        Mockito.when(eventService.getEventRequests(eq(userId), eq(eventId))).thenReturn(requests);

        mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", userId, eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(request1.getId()))
                .andExpect(jsonPath("$[1].status").value(request2.getStatus().name()));
    }

    @Test
    @DisplayName("GET /users/{userId}/events/{eventId}/requests - событие или юзер не найдены при получении запросов")
    void shouldReturnNotFoundWhenGettingEventRequestsEventOrUserNotFound() throws Exception {
        Long userId = 1L;
        Long eventId = 99L;

        Mockito.when(eventService.getEventRequests(eq(userId), eq(eventId)))
                .thenThrow(new NotFoundException("Событие с ID " + eventId + " не найдено"));

        mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", userId, eventId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /users/{userId}/events/{eventId}/requests - успешное обновление статусов запросов")
    void shouldUpdateEventRequestStatus() throws Exception {
        Long userId = 1L;
        Long eventId = 1L;
        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(10L, 11L))
                .status(RequestStatus.CONFIRMED)
                .build();

        ParticipationRequestDto confirmedRequest = createParticipationRequestDto(10L, eventId, 2L,
                RequestStatus.CONFIRMED);
        ParticipationRequestDto rejectedRequest = createParticipationRequestDto(11L, eventId, 3L,
                RequestStatus.REJECTED);

        EventRequestStatusUpdateResult updateResult = EventRequestStatusUpdateResult.builder()
                .confirmedRequests(List.of(confirmedRequest))
                .rejectedRequests(List.of(rejectedRequest))
                .build();

        Mockito.when(eventService.updateEventRequestStatus(eq(userId), eq(eventId),
                        any(EventRequestStatusUpdateRequest.class)))
                .thenReturn(updateResult);

        mockMvc.perform(patch("/users/{userId}/events/{eventId}/requests", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests.length()").value(1))
                .andExpect(jsonPath("$.confirmedRequests[0].id").value(confirmedRequest.getId()))
                .andExpect(jsonPath("$.rejectedRequests.length()").value(1))
                .andExpect(jsonPath("$.rejectedRequests[0].id").value(rejectedRequest.getId()));
    }

    @Test
    @DisplayName("PATCH /users/{userId}/events/{eventId}/requests - конфликт при обновлении (лимит участников)")
    void shouldReturnConflictWhenUpdatingEventRequestStatusConflict() throws Exception {
        Long userId = 1L;
        Long eventId = 1L;
        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(10L))
                .status(RequestStatus.CONFIRMED)
                .build();

        Mockito.when(eventService.updateEventRequestStatus(eq(userId), eq(eventId),
                        any(EventRequestStatusUpdateRequest.class)))
                .thenThrow(new ConflictException("Лимит участников для события исчерпан."));

        mockMvc.perform(patch("/users/{userId}/events/{eventId}/requests", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /users/{userId}/events/{eventId}/requests - событие не найдено при обновлении статусов")
    void shouldReturnNotFoundWhenUpdatingEventRequestStatusEventOrUserNotFound() throws Exception {
        Long userId = 1L;
        Long eventId = 99L;
        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(10L))
                .status(RequestStatus.CONFIRMED)
                .build();

        Mockito.when(eventService.updateEventRequestStatus(eq(userId), eq(eventId),
                        any(EventRequestStatusUpdateRequest.class)))
                .thenThrow(new NotFoundException("Событие с ID " + eventId + " не найдено"));

        mockMvc.perform(patch("/users/{userId}/events/{eventId}/requests", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }
}

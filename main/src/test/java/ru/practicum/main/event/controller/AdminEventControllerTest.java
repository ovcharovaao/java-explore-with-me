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
import ru.practicum.main.category.dto.CategoryDto;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.UpdateEventAdminRequest;
import ru.practicum.main.event.model.EventState;
import ru.practicum.main.event.model.EventAdminStateAction;
import ru.practicum.main.event.service.EventService;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.location.dto.LocationDto;
import ru.practicum.main.user.dto.UserShortDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminEventController.class)
class AdminEventControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @Autowired
    private ObjectMapper objectMapper;

    private EventFullDto createEventFullDto(Long eventId, String title, EventState state) {
        return EventFullDto.builder()
                .id(eventId)
                .title(title)
                .annotation("Annotation for " + title + " that is long enough to pass validation rules.")
                .description("Description for " + title + " that is long enough to pass validation rules.")
                .category(new CategoryDto(1L, "Category Name"))
                .initiator(UserShortDto.builder().id(1L).name("User Name").build())
                .eventDate(LocalDateTime.now().plusDays(5))
                .createdOn(LocalDateTime.now())
                .publishedOn(LocalDateTime.now().plusDays(1))
                .location(new LocationDto(10.0f, 20.0f))
                .paid(false)
                .participantLimit(0)
                .requestModeration(false)
                .confirmedRequests(0L)
                .views(0L)
                .state(state)
                .build();
    }

    private UpdateEventAdminRequest createUpdateEventAdminRequest(String title, EventAdminStateAction stateAction) {
        return UpdateEventAdminRequest.builder()
                .title(title)
                .annotation("Updated admin annotation that is long enough to pass validation rules.")
                .description("Updated admin description that is long enough to pass validation rules.")
                .eventDate(LocalDateTime.now().plusDays(15))
                .location(new LocationDto(11.0f, 21.0f))
                .paid(true)
                .participantLimit(100)
                .requestModeration(true)
                .stateAction(stateAction)
                .build();
    }

    @Test
    @DisplayName("GET /admin/events - успешное получение событий для админа")
    void shouldGetEventsForAdmin() throws Exception {
        List<Long> users = List.of(1L);
        List<String> states = List.of("PUBLISHED");
        List<Long> categories = List.of(1L);
        LocalDateTime rangeStart = LocalDateTime.now().minusDays(1);
        LocalDateTime rangeEnd = LocalDateTime.now().plusDays(1);
        int from = 0;
        int size = 10;

        EventFullDto event1 = createEventFullDto(1L, "Admin Event 1", EventState.PUBLISHED);
        EventFullDto event2 = createEventFullDto(2L, "Admin Event 2", EventState.PUBLISHED);
        List<EventFullDto> expectedEvents = List.of(event1, event2);

        Mockito.when(eventService.getEventsForAdmin(
                        eq(users), eq(states), eq(categories),
                        any(LocalDateTime.class), any(LocalDateTime.class),
                        eq(from), eq(size)))
                .thenReturn(expectedEvents);

        mockMvc.perform(get("/admin/events")
                        .param("users", "1")
                        .param("states", "PUBLISHED")
                        .param("categories", "1")
                        .param("rangeStart", rangeStart.format(java.time.format.DateTimeFormatter
                                .ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .param("rangeEnd", rangeEnd.format(java.time.format.DateTimeFormatter
                                .ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .param("from", String.valueOf(from))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(event1.getId()))
                .andExpect(jsonPath("$[1].title").value(event2.getTitle()));
    }

    @Test
    @DisplayName("GET /admin/events - получение событий для админа без параметров")
    void shouldGetEventsForAdminWithoutParams() throws Exception {
        EventFullDto event1 = createEventFullDto(1L, "Any Event 1", EventState.PENDING);
        List<EventFullDto> expectedEvents = List.of(event1);

        Mockito.when(eventService.getEventsForAdmin(
                        isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(10)))
                .thenReturn(expectedEvents);

        mockMvc.perform(get("/admin/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(event1.getId()));
    }

    @Test
    @DisplayName("PATCH /admin/events/{eventId} - успешное обновление события админом (публикация)")
    void shouldUpdateEventByAdminPublish() throws Exception {
        Long eventId = 1L;
        UpdateEventAdminRequest updateRequest = createUpdateEventAdminRequest("Updated by Admin Publish",
                EventAdminStateAction.PUBLISH_EVENT);
        EventFullDto updatedEventDto = createEventFullDto(eventId, "Updated by Admin Publish",
                EventState.PUBLISHED);

        Mockito.when(eventService.updateEventByAdmin(eq(eventId), any(UpdateEventAdminRequest.class)))
                .thenReturn(updatedEventDto);

        mockMvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.title").value(updatedEventDto.getTitle()))
                .andExpect(jsonPath("$.state").value(EventState.PUBLISHED.name()));
    }

    @Test
    @DisplayName("PATCH /admin/events/{eventId} - успешное обновление события админом (отклонение)")
    void shouldUpdateEventByAdminReject() throws Exception {
        Long eventId = 1L;
        UpdateEventAdminRequest updateRequest = createUpdateEventAdminRequest("Updated by Admin Reject",
                EventAdminStateAction.REJECT_EVENT);
        EventFullDto updatedEventDto = createEventFullDto(eventId, "Updated by Admin Reject",
                EventState.CANCELED);

        Mockito.when(eventService.updateEventByAdmin(eq(eventId), any(UpdateEventAdminRequest.class)))
                .thenReturn(updatedEventDto);

        mockMvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.title").value(updatedEventDto.getTitle()))
                .andExpect(jsonPath("$.state").value(EventState.CANCELED.name()));
    }

    @Test
    @DisplayName("PATCH /admin/events/{eventId} - событие не найдено при обновлении админом")
    void shouldReturnNotFoundWhenUpdatingEventByAdminEventNotFound() throws Exception {
        Long eventId = 99L;
        UpdateEventAdminRequest updateRequest = createUpdateEventAdminRequest("Nonexistent Event",
                EventAdminStateAction.PUBLISH_EVENT);

        Mockito.when(eventService.updateEventByAdmin(eq(eventId), any(UpdateEventAdminRequest.class)))
                .thenThrow(new NotFoundException("Событие с ID " + eventId + " не найдено."));

        mockMvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /admin/events/{eventId} - ошибка валидации при обновлении события админом (короткое название)")
    void shouldFailValidationWhenUpdatingEventByAdminWithShortTitle() throws Exception {
        Long eventId = 1L;
        UpdateEventAdminRequest invalidUpdateRequest = createUpdateEventAdminRequest("ab",
                EventAdminStateAction.PUBLISH_EVENT);

        mockMvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUpdateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /admin/events/{eventId} - ошибка валидации при обновлении события админом (дата в прошлом)")
    void shouldFailValidationWhenUpdatingEventByAdminWithPastDate() throws Exception {
        Long eventId = 1L;
        UpdateEventAdminRequest invalidUpdateRequest = createUpdateEventAdminRequest("Event in past",
                EventAdminStateAction.PUBLISH_EVENT);
        invalidUpdateRequest.setEventDate(LocalDateTime.now().minusDays(1));

        mockMvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUpdateRequest)))
                .andExpect(status().isBadRequest());
    }
}

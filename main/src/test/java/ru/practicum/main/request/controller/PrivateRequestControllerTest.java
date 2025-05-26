package ru.practicum.main.request.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.request.dto.ParticipationRequestDto;
import ru.practicum.main.request.model.RequestStatus;
import ru.practicum.main.request.service.RequestService;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.exception.ConflictException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PrivateRequestController.class)
class PrivateRequestControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RequestService requestService;

    @Test
    @DisplayName("GET /users/{userId}/requests - успешное получение запросов пользователя")
    void shouldGetUserRequests() throws Exception {
        Long userId = 1L;
        ParticipationRequestDto requestDto1 = ParticipationRequestDto.builder()
                .id(1L)
                .created(LocalDateTime.now())
                .event(100L)
                .requester(userId)
                .status(RequestStatus.PENDING)
                .build();
        ParticipationRequestDto requestDto2 = ParticipationRequestDto.builder()
                .id(2L)
                .created(LocalDateTime.now())
                .event(101L)
                .requester(userId)
                .status(RequestStatus.CONFIRMED)
                .build();

        when(requestService.getUserRequests(userId)).thenReturn(List.of(requestDto1, requestDto2));

        mockMvc.perform(get("/users/{userId}/requests", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requestDto1.getId()))
                .andExpect(jsonPath("$[0].requester").value(requestDto1.getRequester()))
                .andExpect(jsonPath("$[1].id").value(requestDto2.getId()))
                .andExpect(jsonPath("$[1].status").value(requestDto2.getStatus().name()));
    }

    @Test
    @DisplayName("GET /users/{userId}/requests - пользователь не найден")
    void shouldReturnNotFoundWhenUserRequestsUserNotFound() throws Exception {
        Long userId = 99L;
        when(requestService.getUserRequests(userId)).thenThrow(new NotFoundException("Пользователь с ID " + userId
                + " не найден"));

        mockMvc.perform(get("/users/{userId}/requests", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /users/{userId}/requests - успешное создание запроса")
    void shouldCreateRequest() throws Exception {
        Long userId = 1L;
        Long eventId = 100L;
        ParticipationRequestDto responseDto = ParticipationRequestDto.builder()
                .id(1L)
                .created(LocalDateTime.now())
                .event(eventId)
                .requester(userId)
                .status(RequestStatus.PENDING)
                .build();

        when(requestService.createRequest(userId, eventId)).thenReturn(responseDto);

        mockMvc.perform(post("/users/{userId}/requests", userId)
                        .param("eventId", eventId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.requester").value(userId))
                .andExpect(jsonPath("$.event").value(eventId))
                .andExpect(jsonPath("$.status").value(RequestStatus.PENDING.name()));
    }

    @Test
    @DisplayName("POST /users/{userId}/requests - событие или пользователь не найдены")
    void shouldReturnNotFoundWhenCreateRequestEventOrUserNotFound() throws Exception {
        Long userId = 1L;
        Long eventId = 99L;

        when(requestService.createRequest(userId, eventId))
                .thenThrow(new NotFoundException("Событие с ID " + eventId + " не найдено"));

        mockMvc.perform(post("/users/{userId}/requests", userId)
                        .param("eventId", eventId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /users/{userId}/requests - запрос уже существует")
    void shouldReturnConflictWhenCreateRequestConflict() throws Exception {
        Long userId = 1L;
        Long eventId = 100L;

        when(requestService.createRequest(userId, eventId))
                .thenThrow(new ConflictException("Запрос на участие уже существует."));

        mockMvc.perform(post("/users/{userId}/requests", userId)
                        .param("eventId", eventId.toString()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /users/{userId}/requests/{requestId}/cancel - успешная отмена запроса")
    void shouldCancelRequest() throws Exception {
        Long userId = 1L;
        Long requestId = 1L;
        ParticipationRequestDto canceledDto = ParticipationRequestDto.builder()
                .id(requestId)
                .created(LocalDateTime.now())
                .event(100L)
                .requester(userId)
                .status(RequestStatus.CANCELED)
                .build();

        when(requestService.cancelRequest(userId, requestId)).thenReturn(canceledDto);

        mockMvc.perform(patch("/users/{userId}/requests/{requestId}/cancel", userId, requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId))
                .andExpect(jsonPath("$.status").value(RequestStatus.CANCELED.name()));
    }

    @Test
    @DisplayName("PATCH /users/{userId}/requests/{requestId}/cancel - запрос не найден")
    void shouldReturnNotFoundWhenCancelRequestNotFound() throws Exception {
        Long userId = 1L;
        Long requestId = 99L;

        when(requestService.cancelRequest(userId, requestId))
                .thenThrow(new NotFoundException("Запрос с ID " + requestId + " не найден."));

        mockMvc.perform(patch("/users/{userId}/requests/{requestId}/cancel", userId, requestId))
                .andExpect(status().isNotFound());
    }
}

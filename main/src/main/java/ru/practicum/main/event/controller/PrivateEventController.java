package ru.practicum.main.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.event.dto.*;
import ru.practicum.main.event.service.EventService;
import ru.practicum.main.request.dto.ParticipationRequestDto;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events")
public class PrivateEventController {
    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(@PathVariable Long userId,
                                    @Valid @RequestBody NewEventDto newEventDto) {
        log.info("Создание события пользователем ID: {}. Данные: {}", userId, newEventDto);
        return eventService.createEvent(userId, newEventDto);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getEventsByInitiator(@PathVariable Long userId,
                                                    @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                                    @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("Получение событий пользователя ID: {} from: {}, size: {}", userId, from, size);
        return eventService.getEventsByInitiator(userId, from, size);
    }

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto getEventByIdAndInitiator(@PathVariable Long userId,
                                                 @PathVariable Long eventId) {
        log.info("Получение события ID: {} для пользователя ID: {}", eventId, userId);
        return eventService.getEventByIdAndInitiator(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto updateEventByInitiator(@PathVariable Long userId,
                                               @PathVariable Long eventId,
                                               @Valid @RequestBody UpdateEventUserRequest updateEventUserRequest) {
        log.info("Обновление события ID: {} пользователем ID: {}. Данные: {}", eventId, userId, updateEventUserRequest);
        return eventService.updateEventByInitiator(userId, eventId, updateEventUserRequest);
    }

    @GetMapping("/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getEventRequests(@PathVariable Long userId,
                                                          @PathVariable Long eventId) {
        log.info("Получение запросов на участие для события ID: {} пользователя ID: {}", eventId, userId);
        return eventService.getEventRequests(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public EventRequestStatusUpdateResult updateEventRequestStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody EventRequestStatusUpdateRequest updateRequest) {
        log.info("Обновление статуса запросов для события ID: {} пользователем ID: {}. " +
                "Запросы: {}", eventId, userId, updateRequest.getRequestIds());
        return eventService.updateEventRequestStatus(userId, eventId, updateRequest);
    }
}

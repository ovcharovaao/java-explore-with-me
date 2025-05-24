package ru.practicum.main.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.service.EventService;
import ru.practicum.main.event.dto.UpdateEventAdminRequest;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/events")
public class AdminEventController {
    private final EventService eventService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventFullDto> getEventsForAdmin(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<String> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("Получение списка событий. Пользователи: {}, Состояния: {}, Категории: {}, Начало: {}, Конец: {}," +
                " from: {}, size: {}", users, states, categories, rangeStart, rangeEnd, from, size);
        return eventService.getEventsForAdmin(users, states, categories, rangeStart, rangeEnd, from, size);
    }

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto updateEventByAdmin(@PathVariable Long eventId,
                                           @Valid @RequestBody UpdateEventAdminRequest updateRequest) {
        log.info("Обновление события ID: {}. Данные: {}", eventId, updateRequest);
        return eventService.updateEventByAdmin(eventId, updateRequest);
    }
}

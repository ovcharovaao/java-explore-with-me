package ru.practicum.main.event.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.main.event.dto.*;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.request.dto.ParticipationRequestDto;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    List<EventShortDto> getEventsByInitiator(Long userId, int from, int size);

    EventFullDto getEventByIdAndInitiator(Long userId, Long eventId);

    EventFullDto updateEventByInitiator(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateEventRequestStatus(Long userId, Long eventId,
                                                            EventRequestStatusUpdateRequest updateRequest);

    List<EventFullDto> getEventsForAdmin(List<Long> users, List<String> states, List<Long> categories,
                                         LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest);

    List<EventShortDto> getPublishedEvents(String text, List<Long> categories, Boolean paid, LocalDateTime rangeStart,
                                           LocalDateTime rangeEnd, Boolean onlyAvailable, String sort, int from,
                                           int size, HttpServletRequest request);

    EventFullDto getPublishedEventById(Long eventId, HttpServletRequest request);

    List<EventShortDto> addViewsAndConfirmedRequestsToShortEvents(List<Event> events);
}

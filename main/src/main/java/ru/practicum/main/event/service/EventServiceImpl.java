package ru.practicum.main.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.category.model.Category;
import ru.practicum.main.category.repository.CategoryRepository;
import ru.practicum.main.event.dto.*;
import ru.practicum.main.event.mapper.EventMapper;
import ru.practicum.main.event.model.*;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.*;
import ru.practicum.main.location.dto.LocationDto;
import ru.practicum.main.location.model.Location;
import ru.practicum.main.location.repository.LocationRepository;
import ru.practicum.main.request.dto.ParticipationRequestDto;
import ru.practicum.main.request.mapper.RequestMapper;
import ru.practicum.main.request.model.*;
import ru.practicum.main.request.repository.RequestRepository;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ru.practicum.main.event.model.EventState.CANCELED;
import static ru.practicum.main.event.model.EventState.PENDING;
import static ru.practicum.main.event.model.EventState.PUBLISHED;
import static ru.practicum.main.request.model.RequestStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final RequestRepository requestRepository;
    private final EventMapper eventMapper;
    private final RequestMapper requestMapper;
    private final StatsClient statsClient;

    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        log.info("Создание события для пользователя ID: {}", userId);

        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с ID" + newEventDto.getCategory() + " не найдена"));

        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }

        Location location = locationRepository.findByLatAndLon(newEventDto.getLocation().getLat(),
                        newEventDto.getLocation().getLon())
                .orElseGet(() -> locationRepository.save(eventMapper.toEntity(newEventDto).getLocation()));

        Event event = eventMapper.toEntity(newEventDto);
        event.setInitiator(initiator);
        event.setCategory(category);
        event.setLocation(location);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(PENDING);
        event.setConfirmedRequests(0L);
        event.setViews(0L);

        Event savedEvent = eventRepository.save(event);

        log.info("Событие успешно создано с ID: {}", savedEvent.getId());
        return eventMapper.toFullDto(savedEvent);
    }

    public List<EventShortDto> getEventsByInitiator(Long userId, int from, int size) {
        log.info("Получение событий пользователя ID: {} from: {}, size: {}", userId, from, size);

        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findByInitiatorId(userId, pageRequest).getContent();

        log.info("Получено {} событий для пользователя ID: {}", events.size(), userId);
        return addViewsAndConfirmedRequestsToShortEvents(events);
    }

    public EventFullDto getEventByIdAndInitiator(Long userId, Long eventId) {
        log.info("Получение события ID: {} для пользователя ID: {}", eventId, userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с ID " + eventId
                        + " не найдено для пользователя " + userId));

        log.info("Событие ID: {} найдено", eventId);
        return addViewsAndConfirmedRequestsToFullEvent(event);
    }

    @Transactional
    public EventFullDto updateEventByInitiator(Long userId, Long eventId,
                                               UpdateEventUserRequest updateEventUserRequest) {
        log.info("Обновление события ID: {} для пользователя ID: {}", eventId, userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с ID " + eventId
                        + " не найдено для пользователя " + userId));

        if (event.getState() == PUBLISHED) {
            throw new ConflictException("Невозможно обновить опубликованное событие");
        }

        if (updateEventUserRequest.getParticipantLimit() != null &&
                updateEventUserRequest.getParticipantLimit() < 0) {
            throw new BadRequestException("participantLimit не может быть отрицательным");
        }

        if (updateEventUserRequest.getEventDate() != null &&
                updateEventUserRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }

        updateEventFields(
                event,
                updateEventUserRequest.getAnnotation(),
                updateEventUserRequest.getCategory(),
                updateEventUserRequest.getDescription(),
                updateEventUserRequest.getEventDate(),
                updateEventUserRequest.getLocation(),
                updateEventUserRequest.getPaid(),
                updateEventUserRequest.getParticipantLimit(),
                updateEventUserRequest.getRequestModeration(),
                updateEventUserRequest.getTitle()
        );

        if (updateEventUserRequest.getStateAction() != null) {
            if (updateEventUserRequest.getStateAction() == EventUserStateAction.SEND_TO_REVIEW) {
                event.setState(PENDING);
            } else if (updateEventUserRequest.getStateAction() == EventUserStateAction.CANCEL_REVIEW) {
                event.setState(CANCELED);
            }
        }

        Event updatedEvent = eventRepository.save(event);

        log.info("Событие ID: {} успешно обновлено пользователем ID: {}", eventId, userId);
        return addViewsAndConfirmedRequestsToFullEvent(updatedEvent);
    }

    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("Получение запросов на участие для события ID: {} пользователя ID: {}", eventId, userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        List<Request> requests = requestRepository.findByEventIdAndEventInitiatorId(eventId, userId);

        log.info("Найдено {} запросов для события ID: {}", requests.size(), eventId);
        return requests.stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventRequestStatusUpdateResult updateEventRequestStatus(Long userId, Long eventId,
                                                                   EventRequestStatusUpdateRequest updateRequest) {
        log.info("Обновление статуса запросов для события ID: {} пользователя ID: {}", eventId, userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с ID " + eventId
                        + " не найдено для пользователя " + userId));

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            throw new ConflictException("Для этого события модерация заявок не требуется или лимит участников равен 0");
        }

        long confirmedRequestsCount = requestRepository.countByEventIdAndStatus(eventId, CONFIRMED);

        if (event.getParticipantLimit() > 0 && confirmedRequestsCount >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников для события ID: " + eventId);
        }

        List<Request> requestsToUpdate = requestRepository.findAllByIdIn(updateRequest.getRequestIds());
        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        for (Request request : requestsToUpdate) {
            if (!request.getEvent().getId().equals(eventId)) {
                throw new ConflictException("Запрос ID: " + request.getId() + " не принадлежит событию ID: " + eventId);
            }
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Статус запроса ID: " + request.getId() + " не PENDING");
            }

            if (updateRequest.getStatus() == CONFIRMED) {
                if (event.getParticipantLimit() == 0 || (event.getParticipantLimit() > 0
                        && confirmedRequestsCount < event.getParticipantLimit())) {
                    request.setStatus(CONFIRMED);
                    confirmedRequestsCount++;
                    confirmedRequests.add(requestMapper.toDto(request));
                } else {
                    request.setStatus(REJECTED);
                    rejectedRequests.add(requestMapper.toDto(request));
                }
            } else if (updateRequest.getStatus() == REJECTED) {
                request.setStatus(REJECTED);
                rejectedRequests.add(requestMapper.toDto(request));
            }
        }

        requestRepository.saveAll(requestsToUpdate);

        log.info("Статусы запросов для события ID: {} обновлены", eventId);
        return new EventRequestStatusUpdateResult(confirmedRequests, rejectedRequests);
    }

    public List<EventFullDto> getEventsForAdmin(List<Long> users, List<String> states, List<Long> categories,
                                                LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        log.info("Получение событий. Пользователи: {}, Состояния: {}, Категории: {}, Начало: {}, Конец: {}, " +
                "from: {}, " + "size: {}", users, states, categories, rangeStart, rangeEnd, from, size);

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("Дата начала не может быть после даты окончания");
        }

        List<EventState> eventStates = null;

        if (states != null && !states.isEmpty()) {
            eventStates = states.stream()
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
        }

        PageRequest pageRequest = PageRequest.of(from / size, size);
        LocalDateTime actualRangeStart = (rangeStart != null) ? rangeStart
                : LocalDateTime.of(1, 1, 1, 0, 0);
        LocalDateTime actualRangeEnd = (rangeEnd != null) ? rangeEnd
                : LocalDateTime.of(9999, 12, 31, 23, 59);

        List<Long> safeUsers = (users != null && users.size() == 1 && users.getFirst() == 0L) ? null : users;
        List<Long> safeCategories = (categories != null && categories.size() == 1 && categories.getFirst()
                == 0L) ? null : categories;

        List<Event> events = eventRepository.findEventsForAdmin(
                safeUsers, eventStates, safeCategories, actualRangeStart, actualRangeEnd, pageRequest
        ).getContent();

        log.info("Найдено {} событий", events.size());
        return addViewsAndConfirmedRequestsToFullEvents(events);
    }

    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        log.info("Обновление события ID: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID " + eventId + " не найдено"));

        if (updateRequest.getEventDate() != null && updateRequest.getEventDate().isBefore(
                LocalDateTime.now().plusHours(1))) {
            throw new BadRequestException("Дата события должна быть не ранее чем через 1 час от текущего момента");
        }

        if (updateRequest.getStateAction() == EventAdminStateAction.PUBLISH_EVENT) {
            if (event.getState() != PENDING) {
                throw new ConflictException("Событие может быть опубликовано только в состоянии PENDING." +
                        " Текущее состояние: " + event.getState());
            }
            if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ConflictException("Дата начала события должна быть не ранее чем за час от даты публикации");
            }

            event.setState(PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if (updateRequest.getStateAction() == EventAdminStateAction.REJECT_EVENT) {
            if (event.getState() == PUBLISHED) {
                throw new ConflictException("Невозможно отклонить опубликованное событие");
            }

            event.setState(EventState.CANCELED);
        }

        updateEventFields(event, updateRequest.getAnnotation(), updateRequest.getCategory(),
                updateRequest.getDescription(), updateRequest.getEventDate(), updateRequest.getLocation(),
                updateRequest.getPaid(), updateRequest.getParticipantLimit(), updateRequest.getRequestModeration(),
                updateRequest.getTitle());

        Event updatedEvent = eventRepository.save(event);

        log.info("Событие ID: {} успешно обновлено админом", eventId);
        return addViewsAndConfirmedRequestsToFullEvent(updatedEvent);
    }

    public List<EventShortDto> getPublishedEvents(String text, List<Long> categories, Boolean paid,
                                                  LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                  Boolean onlyAvailable, String sort, int from, int size,
                                                  HttpServletRequest request) {
        log.info("Получение опубликованных событий. Текст: {}, Категории: {}, Платные: {}, Начало: {}, Конец: {}, " +
                        "Доступные: {}, Сортировка: {}, from: {}, size: {}", text, categories, paid, rangeStart,
                rangeEnd, onlyAvailable, sort, from, size);

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("Дата начала не может быть после даты окончания");
        }

        String safeText = (text != null) ? text : "";
        List<Long> safeCategories = (categories != null) ? categories : Collections.emptyList();
        LocalDateTime actualRangeStart = (rangeStart != null) ? rangeStart
                : LocalDateTime.of(1, 1, 1, 0, 0);
        LocalDateTime actualRangeEnd = (rangeEnd != null) ? rangeEnd
                : LocalDateTime.of(3000, 12, 31, 23, 59);

        Sort sorting = Sort.unsorted();

        if ("EVENT_DATE".equalsIgnoreCase(sort)) {
            sorting = Sort.by(Sort.Direction.ASC, "eventDate");
        }

        PageRequest pageRequest = PageRequest.of(from / size, size, sorting);

        List<Event> events;

        try {
            events = eventRepository.findEventsForPublic(safeText, safeCategories, paid,
                    actualRangeStart, actualRangeEnd, onlyAvailable, pageRequest).getContent();
        } catch (Exception e) {
            log.error("Ошибка при получении событий: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при выполнении запроса", e);
        }

        List<EventShortDto> result = addViewsAndConfirmedRequestsToShortEvents(events);

        if ("VIEWS".equalsIgnoreCase(sort)) {
            result.sort(Comparator.comparing(e -> e.getViews() != null ? e.getViews() : 0L));
        }

        sendHitToStatsService(request);

        log.info("Получено {} опубликованных событий", result.size());
        return result;
    }

    public EventFullDto getPublishedEventById(Long eventId, HttpServletRequest request) {
        log.info("Получение опубликованного события по ID: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID " + eventId + " не найдено"));

        if (event.getState() != PUBLISHED) {
            throw new NotFoundException("Событие с ID " + eventId + " не опубликовано");
        }

        EventFullDto eventFullDto = addViewsAndConfirmedRequestsToFullEvent(event);
        sendHitToStatsService(request);

        log.info("Опубликованное событие ID: {} найдено", eventId);
        return eventFullDto;
    }

    private void updateEventFields(Event event, String annotation, Long categoryId, String description,
                                   LocalDateTime eventDate, LocationDto locationDto,
                                   Boolean paid, Integer participantLimit, Boolean requestModeration, String title) {
        if (annotation != null) {
            event.setAnnotation(annotation);
        }
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new NotFoundException("Категория с ID " + categoryId + " не найдена."));
            event.setCategory(category);
        }
        if (description != null) {
            event.setDescription(description);
        }
        if (eventDate != null) {
            event.setEventDate(eventDate);
        }
        if (locationDto != null) {
            Location location = locationRepository.findByLatAndLon(locationDto.getLat(), locationDto.getLon())
                    .orElseGet(() -> locationRepository.save(new Location(null,
                            locationDto.getLat(), locationDto.getLon())));
            event.setLocation(location);
        }
        if (paid != null) {
            event.setPaid(paid);
        }
        if (participantLimit != null) {
            event.setParticipantLimit(participantLimit);
        }
        if (requestModeration != null) {
            event.setRequestModeration(requestModeration);
        }
        if (title != null) {
            event.setTitle(title);
        }
    }

    private void sendHitToStatsService(HttpServletRequest request) {
        EndpointHitDto hit = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build();
        statsClient.sendHit(hit);
    }

    private Map<Long, Long> getViews(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        LocalDateTime start = events.stream()
                .map(Event::getCreatedOn)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.of(1, 1, 1, 0, 0, 0));
        LocalDateTime end = LocalDateTime.now();
        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        List<ViewStats> stats = statsClient.getStats(start, end, uris, true);
        return stats.stream()
                .collect(Collectors.toMap(
                        stat -> Long.parseLong(stat.getUri().replace("/events/", "")),
                        ViewStats::getHits));
    }

    private Map<Long, Long> getConfirmedRequestsCount(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        return requestRepository.countConfirmedRequestsForEvents(eventIds).stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> (Long) arr[1]));
    }

    private EventFullDto addViewsAndConfirmedRequestsToFullEvent(Event event) {
        EventFullDto dto = eventMapper.toFullDto(event);
        Map<Long, Long> views = getViews(List.of(event));
        dto.setViews(views.getOrDefault(event.getId(), 0L));
        Map<Long, Long> confirmedRequests = getConfirmedRequestsCount(List.of(event));
        dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L));
        return dto;
    }

    public List<EventShortDto> addViewsAndConfirmedRequestsToShortEvents(List<Event> events) {
        Map<Long, Long> views = getViews(events);
        Map<Long, Long> confirmedRequests = getConfirmedRequestsCount(events);

        return events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toShortDto(event);
                    dto.setViews(views.getOrDefault(event.getId(), 0L));
                    dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private List<EventFullDto> addViewsAndConfirmedRequestsToFullEvents(List<Event> events) {
        Map<Long, Long> views = getViews(events);
        Map<Long, Long> confirmedRequests = getConfirmedRequestsCount(events);

        return events.stream()
                .map(event -> {
                    EventFullDto dto = eventMapper.toFullDto(event);
                    dto.setViews(views.getOrDefault(event.getId(), 0L));
                    dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }
}

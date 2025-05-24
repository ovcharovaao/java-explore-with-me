package ru.practicum.main.event.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import ru.practicum.main.category.model.Category;
import ru.practicum.main.category.repository.CategoryRepository;
import ru.practicum.main.event.dto.*;
import ru.practicum.main.event.mapper.EventMapper;
import ru.practicum.main.event.model.*;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.BadRequestException;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.location.dto.LocationDto;
import ru.practicum.main.location.model.Location;
import ru.practicum.main.location.repository.LocationRepository;
import ru.practicum.main.request.model.RequestStatus;
import ru.practicum.main.request.repository.RequestRepository;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventServiceImplTest {
    @InjectMocks
    private EventServiceImpl service;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private EventMapper eventMapper;

    @Captor
    private ArgumentCaptor<Event> eventCaptor;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    @DisplayName("Успешное создание события")
    void createEventSuccess() {
        Long userId = 1L;
        Long categoryId = 2L;
        NewEventDto dto = new NewEventDto();
        dto.setAnnotation("Анонс");
        dto.setCategory(categoryId);
        dto.setDescription("Описание");
        dto.setEventDate(LocalDateTime.now().plusHours(3));
        dto.setLocation(new LocationDto(55.0f, 37.0f));
        dto.setPaid(true);
        dto.setParticipantLimit(10);
        dto.setRequestModeration(true);
        dto.setTitle("Событие");

        User user = new User();
        user.setId(userId);

        Category category = new Category();
        category.setId(categoryId);

        Location location = new Location(null, 55.0f, 37.0f);

        Event event = new Event();
        event.setId(10L);
        event.setLocation(location);
        event.setInitiator(user);
        event.setCategory(category);
        event.setState(EventState.PENDING);

        Event savedEvent = new Event();
        savedEvent.setId(10L);
        savedEvent.setState(EventState.PENDING);

        EventFullDto resultDto = new EventFullDto();
        resultDto.setId(10L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(locationRepository.findByLatAndLon(55.0f, 37.0f)).thenReturn(Optional.empty());
        when(locationRepository.save(any())).thenReturn(location);
        when(eventMapper.toEntity(dto)).thenReturn(event);
        when(eventRepository.save(any())).thenReturn(savedEvent);
        when(eventMapper.toFullDto(savedEvent)).thenReturn(resultDto);

        EventFullDto result = service.createEvent(userId, dto);

        assertEquals(10L, result.getId());
        verify(eventRepository).save(eventCaptor.capture());
        assertEquals(EventState.PENDING, eventCaptor.getValue().getState());
    }

    @Test
    @DisplayName("Создание события с некорректной датой")
    void createEventTooSoon() {
        NewEventDto dto = new NewEventDto();
        dto.setEventDate(LocalDateTime.now().plusMinutes(30));
        dto.setCategory(1L);
        dto.setLocation(new LocationDto(1.0f, 1.0f));

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(new User()));
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.of(new Category()));

        assertThrows(BadRequestException.class,
                () -> service.createEvent(1L, dto));
    }

    @Test
    @DisplayName("Обновление события инициатором - нельзя обновить опубликованное")
    void updateEventByInitiatorPublishedConflict() {
        Long eventId = 1L;
        Long userId = 1L;

        Event event = new Event();
        event.setId(eventId);
        event.setState(EventState.PUBLISHED);

        UpdateEventUserRequest update = new UpdateEventUserRequest();

        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));

        assertThrows(ConflictException.class,
                () -> service.updateEventByInitiator(userId, eventId, update));
    }

    @Test
    @DisplayName("Обновление статуса заявок - превышен лимит")
    void updateRequestStatusLimitReached() {
        Long userId = 1L;
        Long eventId = 2L;

        Event event = new Event();
        event.setId(eventId);
        event.setParticipantLimit(1);
        event.setRequestModeration(true);

        EventRequestStatusUpdateRequest request = new EventRequestStatusUpdateRequest();
        request.setRequestIds(List.of(1L));
        request.setStatus(RequestStatus.CONFIRMED);

        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));
        when(requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED)).thenReturn(1L);

        assertThrows(ConflictException.class,
                () -> service.updateEventRequestStatus(userId, eventId, request));
    }

    @Test
    @DisplayName("Получение опубликованного события - не опубликовано")
    void getPublishedEventNotPublished() {
        Event event = new Event();
        event.setId(1L);
        event.setState(EventState.PENDING);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(NotFoundException.class, () -> service.getPublishedEventById(1L, mock()));
    }
}

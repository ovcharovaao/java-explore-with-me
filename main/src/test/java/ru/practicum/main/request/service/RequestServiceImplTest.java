package ru.practicum.main.request.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.model.EventState;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.dto.ParticipationRequestDto;
import ru.practicum.main.request.mapper.RequestMapper;
import ru.practicum.main.request.model.Request;
import ru.practicum.main.request.model.RequestStatus;
import ru.practicum.main.request.repository.RequestRepository;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {
    @InjectMocks
    private RequestServiceImpl requestService;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RequestMapper requestMapper;

    @Test
    @DisplayName("Успешное получение запросов пользователя")
    void getUserRequestsSuccess() {
        Long userId = 1L;
        Request request = new Request();
        ParticipationRequestDto dto = new ParticipationRequestDto();

        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(requestRepository.findByRequesterId(userId)).thenReturn(List.of(request));
        when(requestMapper.toDto(request)).thenReturn(dto);

        List<ParticipationRequestDto> result = requestService.getUserRequests(userId);

        assertEquals(1, result.size());
        verify(userRepository).findById(userId);
        verify(requestRepository).findByRequesterId(userId);
        verify(requestMapper).toDto(request);
    }

    @Test
    @DisplayName("Получение запросов пользователя — пользователь не найден")
    void getUserRequestsUserNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> requestService.getUserRequests(1L));
    }

    @Test
    @DisplayName("Успешное создание запроса")
    void createRequestSuccessNoModeration() {
        Long userId = 1L;
        Long eventId = 2L;
        User user = new User();
        user.setId(userId);

        Event event = new Event();
        User initiator = new User();
        initiator.setId(3L);
        event.setInitiator(initiator);
        event.setState(EventState.PUBLISHED);
        event.setParticipantLimit(0);
        event.setRequestModeration(false);
        Request saved = new Request();
        saved.setId(100L);
        ParticipationRequestDto dto = new ParticipationRequestDto();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(requestRepository.findByEventIdAndRequesterId(eventId, userId)).thenReturn(Optional.empty());
        when(requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED)).thenReturn(0L);
        when(requestRepository.save(any(Request.class))).thenReturn(saved);
        when(requestMapper.toDto(saved)).thenReturn(dto);

        ParticipationRequestDto result = requestService.createRequest(userId, eventId);

        assertEquals(dto, result);
    }

    @Test
    @DisplayName("Создание запроса — пользователь не найден")
    void createRequestUserNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> requestService.createRequest(1L, 2L));
    }

    @Test
    @DisplayName("Создание запроса — событие не найдено")
    void createRequestEventNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(new User()));
        when(eventRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> requestService.createRequest(1L, 2L));
    }

    @Test
    @DisplayName("Создание запроса — запрос уже существует")
    void createRequestAlreadyExists() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(new User()));
        when(eventRepository.findById(anyLong())).thenReturn(Optional.of(new Event()));
        when(requestRepository.findByEventIdAndRequesterId(anyLong(), anyLong()))
                .thenReturn(Optional.of(new Request()));

        assertThrows(ConflictException.class, () -> requestService.createRequest(1L, 2L));
    }

    @Test
    @DisplayName("Создание запроса — инициатор события")
    void createRequestByInitiator() {
        Long userId = 1L;
        User initiator = new User();
        initiator.setId(userId);
        Event event = new Event();
        event.setInitiator(initiator);

        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(eventRepository.findById(anyLong())).thenReturn(Optional.of(event));
        when(requestRepository.findByEventIdAndRequesterId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(ConflictException.class, () -> requestService.createRequest(userId, 2L));
    }

    @Test
    @DisplayName("Создание запроса — событие не опубликовано")
    void createRequestNotPublished() {
        Long userId = 1L;
        Long initiatorId = 2L;

        User requester = new User();
        requester.setId(userId);

        User initiator = new User();
        initiator.setId(initiatorId);

        Event event = new Event();
        event.setInitiator(initiator);
        event.setState(EventState.PENDING);

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(eventRepository.findById(anyLong())).thenReturn(Optional.of(event));
        when(requestRepository.findByEventIdAndRequesterId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(ConflictException.class, () -> requestService.createRequest(userId, 3L));
    }

    @Test
    @DisplayName("Создание запроса — лимит участников исчерпан")
    void createRequestParticipantLimitReached() {
        Long userId = 1L;
        Long initiatorId = 2L;

        User requester = new User();
        requester.setId(userId);

        User initiator = new User();
        initiator.setId(initiatorId);

        Event event = new Event();
        event.setInitiator(initiator);
        event.setState(EventState.PUBLISHED);
        event.setParticipantLimit(1);
        event.setRequestModeration(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(eventRepository.findById(anyLong())).thenReturn(Optional.of(event));
        when(requestRepository.findByEventIdAndRequesterId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(requestRepository.countByEventIdAndStatus(anyLong(), eq(RequestStatus.CONFIRMED)))
                .thenReturn(1L);

        assertThrows(ConflictException.class, () -> requestService.createRequest(userId, 3L));
    }

    @Test
    @DisplayName("Успешная отмена запроса")
    void cancelRequestSuccess() {
        Long userId = 1L;
        Long requestId = 5L;
        Request request = new Request();
        request.setId(requestId);
        request.setStatus(RequestStatus.PENDING);
        ParticipationRequestDto dto = new ParticipationRequestDto();

        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(requestRepository.findByIdAndRequesterId(requestId, userId)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenReturn(request);
        when(requestMapper.toDto(request)).thenReturn(dto);

        ParticipationRequestDto result = requestService.cancelRequest(userId, requestId);

        assertEquals(dto, result);
        assertEquals(RequestStatus.CANCELED, request.getStatus());
    }

    @Test
    @DisplayName("Отмена запроса — пользователь не найден")
    void cancelRequestUserNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> requestService.cancelRequest(1L, 2L));
    }

    @Test
    @DisplayName("Отмена запроса — запрос не найден")
    void cancelRequestNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(new User()));
        when(requestRepository.findByIdAndRequesterId(anyLong(), anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> requestService.cancelRequest(1L, 2L));
    }
}

package ru.practicum.main.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.event.model.EventState;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.model.RequestStatus;
import ru.practicum.main.request.dto.ParticipationRequestDto;
import ru.practicum.main.request.mapper.RequestMapper;
import ru.practicum.main.request.model.Request;
import ru.practicum.main.request.repository.RequestRepository;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestMapper requestMapper;

    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Получение запросов на участие для пользователя ID: {}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        List<Request> requests = requestRepository.findByRequesterId(userId);

        log.info("Найдено {} запросов для пользователя ID: {}", requests.size(), userId);
        return requests.stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("Создание запроса на участие: пользователь ID: {}, событие ID: {}", userId, eventId);

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID " + eventId + " не найдено"));

        if (requestRepository.findByEventIdAndRequesterId(eventId, userId).isPresent()) {
            throw new ConflictException("Запрос на участие в событии ID: " + eventId + " от пользователя ID: " + userId
                    + " уже существует");
        }
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своем событии");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

        if (event.getParticipantLimit() > 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException("Лимит участников для события ID: " + eventId + " исчерпан");
        }

        Request request = Request.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(requester)
                .status(RequestStatus.PENDING)
                .build();

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        }

        Request savedRequest = requestRepository.save(request);

        log.info("Запрос на участие успешно создан с ID: {}", savedRequest.getId());
        return requestMapper.toDto(savedRequest);
    }

    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Отмена запроса ID: {} пользователем ID: {}", requestId, userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        Request request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Запрос с ID " + requestId + " не найден для пользователя "
                        + userId));

        request.setStatus(RequestStatus.CANCELED);
        Request canceledRequest = requestRepository.save(request);

        log.info("Запрос ID: {} успешно отменен.", canceledRequest.getId());
        return requestMapper.toDto(canceledRequest);
    }
}

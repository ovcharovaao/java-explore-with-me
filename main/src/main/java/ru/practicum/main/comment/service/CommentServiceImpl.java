package ru.practicum.main.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.comment.dto.CommentDto;
import ru.practicum.main.comment.dto.NewCommentDto;
import ru.practicum.main.comment.dto.UpdateCommentDto;
import ru.practicum.main.comment.mapper.CommentMapper;
import ru.practicum.main.comment.model.Comment;
import ru.practicum.main.comment.repository.CommentRepository;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.model.EventState;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID " + eventId + " не найдено"));

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Невозможно оставить комментарий к неопубликованному событию");
        }

        Comment comment = commentMapper.toComment(newCommentDto, event, author);
        comment.setCreatedOn(LocalDateTime.now());
        comment.setEditedOn(null);

        Comment savedComment = commentRepository.save(comment);
        log.info("Добавлен комментарий: {}", savedComment);
        return commentMapper.toCommentDto(savedComment);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с ID " + commentId + " не найден"));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Пользователь с ID " + userId + " не является автором комментария с ID "
                    + commentId);
        }

        if (updateCommentDto.getText() != null && !updateCommentDto.getText().isBlank()) {
            comment.setText(updateCommentDto.getText());
            comment.setEditedOn(LocalDateTime.now());
        }

        Comment updatedComment = commentRepository.save(comment);
        log.info("Обновлен комментарий: {}", updatedComment);
        return commentMapper.toCommentDto(updatedComment);
    }

    @Override
    @Transactional
    public void deleteCommentByUser(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с ID " + commentId + " не найден"));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Пользователь с ID " + userId + " не является автором комментария с ID "
                    + commentId);
        }

        commentRepository.delete(comment);
        log.info("Комментарий с ID {} успешно удален пользователем с ID {}", commentId, userId);
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с ID " + commentId + " не найден"));
        commentRepository.delete(comment);
        log.info("Комментарий с ID {} успешно удален администратором", commentId);
    }

    @Override
    public List<CommentDto> getCommentsByEvent(Long eventId, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        List<Comment> comments = commentRepository.findAllByEventId(eventId, page);
        log.info("Получены комментарии для события с ID {}: {}", eventId, comments);
        return comments.stream()
                .map(commentMapper::toCommentDto)
                .collect(Collectors.toList());
    }
}

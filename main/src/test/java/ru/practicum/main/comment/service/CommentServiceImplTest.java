package ru.practicum.main.comment.service;

import org.junit.jupiter.api.*;
import org.mockito.*;
import ru.practicum.main.comment.dto.*;
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

import org.springframework.data.domain.PageRequest;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommentServiceImplTest {
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentServiceImpl commentService;

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
    @DisplayName("Успешное добавление комментария")
    void addComment_success() {
        Long userId = 1L, eventId = 2L;
        NewCommentDto newDto = new NewCommentDto("Text");
        User user = new User();
        user.setId(userId);
        Event event = new Event();
        event.setId(eventId);
        event.setState(EventState.PUBLISHED);
        Comment comment = new Comment();
        comment.setId(3L);
        comment.setAuthor(user);
        comment.setEvent(event);
        CommentDto commentDto = new CommentDto();
        commentDto.setId(3L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(commentMapper.toComment(newDto, event, user)).thenReturn(comment);
        when(commentRepository.save(any())).thenReturn(comment);
        when(commentMapper.toCommentDto(comment)).thenReturn(commentDto);

        CommentDto result = commentService.addComment(userId, eventId, newDto);

        assertThat(result).isEqualTo(commentDto);
    }

    @Test
    @DisplayName("Добавление комментария - пользователь не найден")
    void addComment_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(1L, 2L, new NewCommentDto("text")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Добавление комментария - событие не найдено")
    void addComment_eventNotFound() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(1L, 2L, new NewCommentDto("text")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Добавление комментария - событие не опубликовано")
    void addComment_eventNotPublished() {
        User user = new User();
        user.setId(1L);
        Event event = new Event();
        event.setId(2L);
        event.setState(EventState.PENDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(2L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> commentService.addComment(1L, 2L, new NewCommentDto("text")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Успешное обновление комментария")
    void updateComment_success() {
        Long commentId = 3L, userId = 1L;
        Comment comment = new Comment();
        comment.setId(commentId);
        User author = new User();
        author.setId(userId);
        comment.setAuthor(author);

        UpdateCommentDto updateDto = new UpdateCommentDto("Updated text");
        CommentDto commentDto = new CommentDto();
        commentDto.setId(commentId);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any())).thenReturn(comment);
        when(commentMapper.toCommentDto(comment)).thenReturn(commentDto);

        CommentDto result = commentService.updateComment(userId, commentId, updateDto);
        assertThat(result).isEqualTo(commentDto);
    }

    @Test
    @DisplayName("Обновление комментария - пользователь не автор")
    void updateComment_notAuthor() {
        Comment comment = new Comment();
        User otherUser = new User();
        otherUser.setId(2L);
        comment.setAuthor(otherUser);

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.updateComment(1L, 1L,
                new UpdateCommentDto("text")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Удаление комментария пользователем")
    void deleteCommentByUser_success() {
        Comment comment = new Comment();
        comment.setId(1L);
        User user = new User();
        user.setId(1L);
        comment.setAuthor(user);

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        commentService.deleteCommentByUser(1L, 1L);
        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("Удаление комментария пользователем - пользователь не автор")
    void deleteCommentByUser_notAuthor() {
        Comment comment = new Comment();
        comment.setAuthor(new User(2L, null, null, null));

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.deleteCommentByUser(1L, 1L))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Удаление комментария администратором")
    void deleteCommentByAdmin_success() {
        Comment comment = new Comment();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        commentService.deleteCommentByAdmin(1L);
        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("Удаление комментария админом - комментарий не найден")
    void deleteCommentByAdmin_notFound() {
        when(commentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteCommentByAdmin(1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Получение комментариев по событию")
    void getCommentsByEvent_success() {
        Long eventId = 1L;
        PageRequest page = PageRequest.of(0, 10);
        Comment comment = new Comment();
        comment.setId(1L);
        CommentDto commentDto = new CommentDto();
        commentDto.setId(1L);

        when(commentRepository.findAllByEventId(eventId, page)).thenReturn(List.of(comment));
        when(commentMapper.toCommentDto(comment)).thenReturn(commentDto);

        List<CommentDto> result = commentService.getCommentsByEvent(eventId, 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(commentDto);
    }
}
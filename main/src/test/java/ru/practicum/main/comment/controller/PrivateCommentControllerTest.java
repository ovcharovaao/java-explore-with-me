package ru.practicum.main.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.comment.dto.CommentDto;
import ru.practicum.main.comment.dto.NewCommentDto;
import ru.practicum.main.comment.dto.UpdateCommentDto;
import ru.practicum.main.comment.service.CommentService;
import ru.practicum.main.user.dto.UserShortDto;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PrivateCommentController.class)
class PrivateCommentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /users/{userId}/comments/events/{eventId} - успешное добавление комментария")
    void shouldAddComment() throws Exception {
        Long userId = 1L;
        Long eventId = 101L;
        NewCommentDto newCommentDto = new NewCommentDto("This is a new comment.");
        UserShortDto author = new UserShortDto(userId, "TestUser");

        CommentDto responseDto = CommentDto.builder()
                .id(1L)
                .text("This is a new comment.")
                .eventId(eventId)
                .author(author)
                .createdOn(LocalDateTime.now())
                .editedOn(null)
                .build();

        Mockito.when(commentService.addComment(anyLong(), anyLong(), any(NewCommentDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/users/{userId}/comments/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCommentDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(responseDto.getId()))
                .andExpect(jsonPath("$.text").value(responseDto.getText()))
                .andExpect(jsonPath("$.eventId").value(responseDto.getEventId()))
                .andExpect(jsonPath("$.author.id").value(responseDto.getAuthor().getId()))
                .andExpect(jsonPath("$.author.name").value(responseDto.getAuthor().getName()));
    }

    @Test
    @DisplayName("POST /users/{userId}/comments/events/{eventId} - ошибка валидации (пустой текст комментария)")
    void shouldFailToAddCommentWhenTextIsEmpty() throws Exception {
        Long userId = 1L;
        Long eventId = 101L;
        NewCommentDto newCommentDto = new NewCommentDto(""); // Empty comment text

        mockMvc.perform(post("/users/{userId}/comments/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCommentDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /users/{userId}/comments/{commentId} - успешное обновление комментария")
    void shouldUpdateComment() throws Exception {
        Long userId = 1L;
        Long commentId = 1L;
        UpdateCommentDto updateCommentDto = new UpdateCommentDto("Updated comment text.");
        UserShortDto author = new UserShortDto(userId, "TestUser");

        CommentDto responseDto = CommentDto.builder()
                .id(commentId)
                .text("Updated comment text.")
                .eventId(101L)
                .author(author)
                .createdOn(LocalDateTime.now().minusDays(1))
                .editedOn(LocalDateTime.now())
                .build();

        Mockito.when(commentService.updateComment(anyLong(), anyLong(), any(UpdateCommentDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(patch("/users/{userId}/comments/{commentId}", userId, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCommentDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(responseDto.getId()))
                .andExpect(jsonPath("$.text").value(responseDto.getText()))
                .andExpect(jsonPath("$.editedOn").exists());
    }

    @Test
    @DisplayName("PATCH /users/{userId}/comments/{commentId} - ошибка валидации (пустой текст обновления)")
    void shouldFailToUpdateCommentWhenTextIsEmpty() throws Exception {
        Long userId = 1L;
        Long commentId = 1L;
        UpdateCommentDto updateCommentDto = new UpdateCommentDto("");

        mockMvc.perform(patch("/users/{userId}/comments/{commentId}", userId, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCommentDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /users/{userId}/comments/{commentId} - успешное удаление комментария пользователем")
    void shouldDeleteCommentByUser() throws Exception {
        Long userId = 1L;
        Long commentId = 1L;

        mockMvc.perform(delete("/users/{userId}/comments/{commentId}", userId, commentId))
                .andExpect(status().isNoContent());

        Mockito.verify(commentService).deleteCommentByUser(userId, commentId);
    }
}
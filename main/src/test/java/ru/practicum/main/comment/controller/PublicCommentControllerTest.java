package ru.practicum.main.comment.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.comment.dto.CommentDto;
import ru.practicum.main.comment.service.CommentService;
import ru.practicum.main.user.dto.UserShortDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicCommentController.class)
class PublicCommentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @Test
    @DisplayName("GET /events/{eventId}/comments - успешное получение комментариев для события")
    void shouldGetCommentsByEvent() throws Exception {
        Long eventId = 1L;
        UserShortDto author1 = new UserShortDto(1L, "Author 1");
        UserShortDto author2 = new UserShortDto(2L, "Author 2");

        CommentDto comment1 = CommentDto.builder()
                .id(1L)
                .text("Text comment 1")
                .eventId(eventId)
                .author(author1)
                .createdOn(LocalDateTime.now().minusDays(1))
                .editedOn(null)
                .build();

        CommentDto comment2 = CommentDto.builder()
                .id(2L)
                .text("Text comment 2")
                .eventId(eventId)
                .author(author2)
                .createdOn(LocalDateTime.now())
                .editedOn(null)
                .build();

        List<CommentDto> expectedComments = List.of(comment1, comment2);

        Mockito.when(commentService.getCommentsByEvent(anyLong(), anyInt(), anyInt()))
                .thenReturn(expectedComments);

        mockMvc.perform(get("/events/{eventId}/comments", eventId)
                        .param("from", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(comment1.getId()))
                .andExpect(jsonPath("$[0].text").value(comment1.getText()))
                .andExpect(jsonPath("$[0].author.id").value(comment1.getAuthor().getId()))
                .andExpect(jsonPath("$[0].author.name").value(comment1.getAuthor().getName()))
                .andExpect(jsonPath("$[1].id").value(comment2.getId()))
                .andExpect(jsonPath("$[1].text").value(comment2.getText()))
                .andExpect(jsonPath("$[1].author.id").value(comment2.getAuthor().getId()))
                .andExpect(jsonPath("$[1].author.name").value(comment2.getAuthor().getName()));
    }

    @Test
    @DisplayName("GET /events/{eventId}/comments - получение комментариев с параметрами по умолчанию")
    void shouldGetCommentsByEventWithDefaultParams() throws Exception {
        Long eventId = 1L;
        List<CommentDto> expectedComments = List.of();

        Mockito.when(commentService.getCommentsByEvent(eventId, 0, 10))
                .thenReturn(expectedComments);

        mockMvc.perform(get("/events/{eventId}/comments", eventId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        Mockito.verify(commentService).getCommentsByEvent(eventId, 0, 10);
    }
}
package ru.practicum.main.comment.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.comment.service.CommentService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCommentController.class)
class AdminCommentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @Test
    @DisplayName("DELETE /admin/comments/{commentId} - успешное удаление комментария администратором")
    void shouldDeleteCommentByAdmin() throws Exception {
        Long commentId = 1L;

        mockMvc.perform(delete("/admin/comments/{commentId}", commentId))
                .andExpect(status().isNoContent());

        Mockito.verify(commentService).deleteCommentByAdmin(commentId);
    }
}
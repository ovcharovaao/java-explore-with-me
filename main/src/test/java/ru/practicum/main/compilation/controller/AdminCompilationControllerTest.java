package ru.practicum.main.compilation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.compilation.dto.CompilationDto;
import ru.practicum.main.compilation.dto.NewCompilationDto;
import ru.practicum.main.compilation.dto.UpdateCompilationRequest;
import ru.practicum.main.compilation.service.CompilationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCompilationController.class)
class AdminCompilationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompilationService compilationService;

    @Test
    @DisplayName("POST /admin/compilations - успешное создание подборки")
    void createCompilation_shouldReturnCreated() throws Exception {
        NewCompilationDto newDto = new NewCompilationDto();
        newDto.setTitle("Test");
        CompilationDto responseDto = new CompilationDto();
        responseDto.setId(1L);
        responseDto.setTitle("Test");

        when(compilationService.createCompilation(any())).thenReturn(responseDto);

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "title": "Test",
                                "pinned": false,
                                "events": []
                            }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("DELETE /admin/compilations/{compId} - успешное удаление подборки")
    void deleteCompilation_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/admin/compilations/1"))
                .andExpect(status().isNoContent());

        verify(compilationService).deleteCompilation(1L);
    }

    @Test
    @DisplayName("PATCH /admin/compilations/{compId} - успешное обновление подборки")
    void updateCompilation_shouldReturnUpdatedDto() throws Exception {
        UpdateCompilationRequest updateRequest = new UpdateCompilationRequest();
        updateRequest.setTitle("Updated");

        CompilationDto updated = new CompilationDto();
        updated.setId(1L);
        updated.setTitle("Updated");

        when(compilationService.updateCompilation(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(patch("/admin/compilations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "title": "Updated"
                            }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }
}

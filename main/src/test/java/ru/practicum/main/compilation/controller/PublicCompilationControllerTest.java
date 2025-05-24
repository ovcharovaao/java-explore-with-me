package ru.practicum.main.compilation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.compilation.dto.CompilationDto;
import ru.practicum.main.compilation.service.CompilationService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicCompilationController.class)
class PublicCompilationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompilationService compilationService;

    @Test
    @DisplayName("GET /compilations - успешное получение подборок")
    void getCompilations_shouldReturnList() throws Exception {
        CompilationDto dto = new CompilationDto();
        dto.setId(1L);
        dto.setTitle("Sample");

        when(compilationService.getCompilations(any(), anyInt(), anyInt()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/compilations?pinned=true&from=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("GET /compilations/{compId} - успешное получение подборки по ID")
    void getCompilationById_shouldReturnDto() throws Exception {
        CompilationDto dto = new CompilationDto();
        dto.setId(1L);
        dto.setTitle("Sample");

        when(compilationService.getCompilationById(1L)).thenReturn(dto);

        mockMvc.perform(get("/compilations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}

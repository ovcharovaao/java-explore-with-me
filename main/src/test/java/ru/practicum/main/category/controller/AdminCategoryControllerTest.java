package ru.practicum.main.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.category.dto.CategoryDto;
import ru.practicum.main.category.dto.NewCategoryDto;
import ru.practicum.main.category.service.CategoryService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCategoryController.class)
class AdminCategoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /admin/categories - успешное создание категории")
    void shouldCreateCategory() throws Exception {
        NewCategoryDto newDto = new NewCategoryDto("Test category");
        CategoryDto responseDto = new CategoryDto(1L, "Test category");

        Mockito.when(categoryService.createCategory(any())).thenReturn(responseDto);

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Test category"));
    }

    @Test
    @DisplayName("POST /admin/categories - ошибка валидации (пустое имя)")
    void shouldFailValidationWhenNameIsEmpty() throws Exception {
        NewCategoryDto invalidDto = new NewCategoryDto("");

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /admin/categories/{catId} - успешное обновление")
    void shouldUpdateCategory() throws Exception {
        CategoryDto updateDto = new CategoryDto(1L, "Updated category");
        Mockito.when(categoryService.updateCategory(eq(1L), any())).thenReturn(updateDto);

        mockMvc.perform(patch("/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Updated category"));
    }

    @Test
    @DisplayName("DELETE /admin/categories/{catId} - успешное удаление")
    void shouldDeleteCategory() throws Exception {
        mockMvc.perform(delete("/admin/categories/1"))
                .andExpect(status().isNoContent());

        Mockito.verify(categoryService).deleteCategory(1L);
    }
}

package ru.practicum.main.category.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.category.dto.CategoryDto;
import ru.practicum.main.category.service.CategoryService;
import ru.practicum.main.exception.NotFoundException;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicCategoryController.class)
class PublicCategoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Test
    @DisplayName("GET /categories - успешное получение всех категорий с пагинацией по умолчанию")
    void shouldGetCategoriesWithDefaultPagination() throws Exception {
        CategoryDto categoryDto1 = new CategoryDto(1L, "Concerts");
        CategoryDto categoryDto2 = new CategoryDto(2L, "Festivals");
        List<CategoryDto> categories = List.of(categoryDto1, categoryDto2);

        when(categoryService.getCategories(eq(0), eq(10))).thenReturn(categories);

        mockMvc.perform(get("/categories")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Concerts"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].name").value("Festivals"));

        Mockito.verify(categoryService).getCategories(0, 10);
    }

    @Test
    @DisplayName("GET /categories - успешное получение категорий с кастомной пагинацией")
    void shouldGetCategoriesWithCustomPagination() throws Exception {
        CategoryDto categoryDto = new CategoryDto(3L, "Workshops");
        List<CategoryDto> categories = List.of(categoryDto);

        when(categoryService.getCategories(eq(5), eq(1))).thenReturn(categories);

        mockMvc.perform(get("/categories")
                        .param("from", "5")
                        .param("size", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(3L))
                .andExpect(jsonPath("$[0].name").value("Workshops"));

        Mockito.verify(categoryService).getCategories(5, 1);
    }

    @Test
    @DisplayName("GET /categories - возвращает пустой список, если нет категорий")
    void shouldReturnEmptyListWhenNoCategories() throws Exception {
        when(categoryService.getCategories(anyInt(), anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/categories")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0))
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("GET /categories/{catId} - успешное получение категории по ID")
    void shouldGetCategoryById() throws Exception {
        Long categoryId = 1L;
        CategoryDto categoryDto = new CategoryDto(categoryId, "Sports");

        when(categoryService.getCategoryById(eq(categoryId))).thenReturn(categoryDto);

        mockMvc.perform(get("/categories/{catId}", categoryId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId))
                .andExpect(jsonPath("$.name").value("Sports"));

        Mockito.verify(categoryService).getCategoryById(categoryId);
    }

    @Test
    @DisplayName("GET /categories/{catId} - категория не найдена")
    void shouldReturnNotFoundWhenCategoryNotFound() throws Exception {
        Long categoryId = 99L;

        when(categoryService.getCategoryById(eq(categoryId)))
                .thenThrow(new NotFoundException("Категория с ID " + categoryId + " не найдена"));

        mockMvc.perform(get("/categories/{catId}", categoryId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}

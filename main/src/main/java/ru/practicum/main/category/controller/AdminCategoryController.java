package ru.practicum.main.category.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.category.dto.CategoryDto;
import ru.practicum.main.category.service.CategoryService;
import ru.practicum.main.category.dto.NewCategoryDto;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/categories")
public class AdminCategoryController {
    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody NewCategoryDto newCategoryDto) {
        log.info("Создание новой категории: {}", newCategoryDto);
        return categoryService.createCategory(newCategoryDto);
    }

    @PatchMapping("/{catId}")
    @ResponseStatus(HttpStatus.OK)
    public CategoryDto updateCategory(@PathVariable Long catId,
                                      @Valid @RequestBody CategoryDto categoryDto) {
        log.info("Обновление категории ID: {} на {}", catId, categoryDto);
        return categoryService.updateCategory(catId, categoryDto);
    }

    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long catId) {
        log.info("Удаление категории ID: {}", catId);
        categoryService.deleteCategory(catId);
    }
}

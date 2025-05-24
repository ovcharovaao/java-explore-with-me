package ru.practicum.main.category.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.practicum.main.category.dto.CategoryDto;
import ru.practicum.main.category.dto.NewCategoryDto;
import ru.practicum.main.category.mapper.CategoryMapper;
import ru.practicum.main.category.model.Category;
import ru.practicum.main.category.repository.CategoryRepository;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CategoryServiceImplTest {
    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

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
    @DisplayName("Успешное создание категории")
    void createCategory_success() {
        NewCategoryDto newDto = new NewCategoryDto();
        newDto.setName("NewCat");

        Category category = new Category();
        category.setId(1L);
        category.setName("NewCat");

        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(1L);
        categoryDto.setName("NewCat");

        when(categoryRepository.existsByName("NewCat")).thenReturn(false);
        when(categoryMapper.toEntity(newDto)).thenReturn(category);
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.toDto(category)).thenReturn(categoryDto);

        CategoryDto result = categoryService.createCategory(newDto);

        assertThat(result).isEqualTo(categoryDto);
    }

    @Test
    @DisplayName("Создание категории - категория уже существует")
    void createCategory_conflict() {
        NewCategoryDto newDto = new NewCategoryDto();
        newDto.setName("Existing");

        when(categoryRepository.existsByName("Existing")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(newDto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("уже существует");
    }

    @Test
    @DisplayName("Успешное обновление категории")
    void updateCategory_success() {
        Long catId = 1L;
        CategoryDto dto = new CategoryDto();
        dto.setId(catId);
        dto.setName("Updated");

        Category existing = new Category();
        existing.setId(catId);
        existing.setName("OldName");

        Category updated = new Category();
        updated.setId(catId);
        updated.setName("Updated");

        when(categoryRepository.findById(catId)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByName("Updated")).thenReturn(false);
        when(categoryRepository.save(existing)).thenReturn(updated);
        when(categoryMapper.toDto(updated)).thenReturn(dto);

        CategoryDto result = categoryService.updateCategory(catId, dto);

        assertThat(result).isEqualTo(dto);
    }

    @Test
    @DisplayName("Обновление категории - категория не найдена")
    void updateCategory_notFound() {
        Long catId = 1L;
        CategoryDto dto = new CategoryDto();
        dto.setName("NewName");

        when(categoryRepository.findById(catId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(catId, dto))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Обновление категории- имя категории уже существует")
    void updateCategory_conflict() {
        Long catId = 1L;
        CategoryDto dto = new CategoryDto();
        dto.setName("Duplicate");

        Category existing = new Category();
        existing.setId(catId);
        existing.setName("Original");

        when(categoryRepository.findById(catId)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByName("Duplicate")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(catId, dto))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Успешное удаление категории")
    void deleteCategory_success() {
        Long catId = 1L;

        when(categoryRepository.existsById(catId)).thenReturn(true);
        when(eventRepository.existsByCategoryId(catId)).thenReturn(false);

        categoryService.deleteCategory(catId);

        verify(categoryRepository).deleteById(catId);
    }

    @Test
    @DisplayName("Удаление категории - категория не найдена")
    void deleteCategory_notFound() {
        Long catId = 1L;

        when(categoryRepository.existsById(catId)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.deleteCategory(catId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Удаление категории - с категорией связаны события")
    void deleteCategory_conflict() {
        Long catId = 1L;

        when(categoryRepository.existsById(catId)).thenReturn(true);
        when(eventRepository.existsByCategoryId(catId)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(catId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Успешное получение категорий")
    void getCategories_success() {
        int from = 0;
        int size = 10;
        PageRequest pageRequest = PageRequest.of(0, size);

        Category category = new Category();
        category.setId(1L);
        category.setName("Test");

        CategoryDto dto = new CategoryDto();
        dto.setId(1L);
        dto.setName("Test");

        when(categoryRepository.findAll(pageRequest))
                .thenReturn(new PageImpl<>(List.of(category)));
        when(categoryMapper.toDto(category)).thenReturn(dto);

        List<CategoryDto> result = categoryService.getCategories(from, size);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("Test");
    }

    @Test
    @DisplayName("Успешное получение категории по ID")
    void getCategoryById_success() {
        Long catId = 1L;
        Category category = new Category();
        category.setId(catId);
        category.setName("Test");

        CategoryDto dto = new CategoryDto();
        dto.setId(catId);
        dto.setName("Test");

        when(categoryRepository.findById(catId)).thenReturn(Optional.of(category));
        when(categoryMapper.toDto(category)).thenReturn(dto);

        CategoryDto result = categoryService.getCategoryById(catId);

        assertThat(result).isEqualTo(dto);
    }

    @Test
    @DisplayName("Получение категории по ID - категория не найдена")
    void getCategoryById_notFound() {
        Long catId = 1L;

        when(categoryRepository.findById(catId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(catId))
                .isInstanceOf(NotFoundException.class);
    }
}

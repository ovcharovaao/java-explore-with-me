package ru.practicum.main.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.category.dto.CategoryDto;
import ru.practicum.main.category.dto.NewCategoryDto;
import ru.practicum.main.category.mapper.CategoryMapper;
import ru.practicum.main.category.model.Category;
import ru.practicum.main.category.repository.CategoryRepository;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        log.info("Создание новой категории: {}", newCategoryDto.getName());

        if (categoryRepository.existsByName(newCategoryDto.getName())) {
            throw new ConflictException("Категория с названием " + newCategoryDto.getName() + " уже существует");
        }

        Category category = categoryRepository.save(categoryMapper.toEntity(newCategoryDto));

        log.info("Категория успешно создана с ID: {}", category.getId());
        return categoryMapper.toDto(category);
    }

    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto categoryDto) {
        log.info("Обновление категории с ID: {} на {}", catId, categoryDto.getName());

        Category existingCategory = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с ID " + catId + " не найдена"));

        if (!existingCategory.getName().equals(categoryDto.getName())
                && categoryRepository.existsByName(categoryDto.getName())) {
            throw new ConflictException("Категория с названием " + categoryDto.getName() + " уже существует");
        }

        existingCategory.setName(categoryDto.getName());
        Category updatedCategory = categoryRepository.save(existingCategory);

        log.info("Категория с ID {} успешно обновлена", updatedCategory.getId());
        return categoryMapper.toDto(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long catId) {
        log.info("Удаление категории с ID: {}", catId);

        if (!categoryRepository.existsById(catId)) {
            throw new NotFoundException("Категория с ID " + catId + " не найдена");
        }
        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("Невозможно удалить категорию, так как с ней связаны события");
        }

        categoryRepository.deleteById(catId);
        log.info("Категория с ID {} успешно удалена", catId);
    }

    public List<CategoryDto> getCategories(int from, int size) {
        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<Category> categories = categoryRepository.findAll(pageRequest).getContent();

        log.info("Получено {} категорий", categories.size());
        return categories.stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    public CategoryDto getCategoryById(Long catId) {
        log.info("Получение категории по ID: {}", catId);

        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с ID " + catId + " не найдена"));

        log.info("Категория с ID {} найдена", catId);
        return categoryMapper.toDto(category);
    }
}

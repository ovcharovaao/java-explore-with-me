package ru.practicum.main.category.mapper;

import org.mapstruct.Mapper;
import ru.practicum.main.category.dto.CategoryDto;
import ru.practicum.main.category.dto.NewCategoryDto;
import ru.practicum.main.category.model.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    Category toEntity(NewCategoryDto dto);

    CategoryDto toDto(Category category);
}

package ru.practicum.main.event.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.main.category.mapper.CategoryMapper;
import ru.practicum.main.category.model.Category;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.dto.NewEventDto;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.location.mapper.LocationMapper;
import ru.practicum.main.user.mapper.UserMapper;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class, UserMapper.class, LocationMapper.class})
public interface EventMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "location", source = "location")
    @Mapping(target = "createdOn", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "state", expression = "java(ru.practicum.main.event.model.EventState.PENDING)")
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    Event toEntity(NewEventDto dto);

    @Mapping(source = "category", target = "category")
    @Mapping(source = "initiator", target = "initiator")
    @Mapping(source = "location", target = "location")
    EventFullDto toFullDto(Event event);

    @Mapping(source = "category", target = "category")
    @Mapping(source = "initiator", target = "initiator")
    EventShortDto toShortDto(Event event);

    default Category map(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = new Category();
        category.setId(categoryId);
        return category;
    }
}

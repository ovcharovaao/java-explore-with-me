package ru.practicum.stats.app.mapper;

import org.mapstruct.Mapper;
import ru.practicum.stats.app.model.EndpointHit;
import ru.practicum.stats.dto.EndpointHitDto;

@Mapper(componentModel = "spring")
public interface EndpointHitMapper {
    EndpointHit toEntity(EndpointHitDto dto);
}

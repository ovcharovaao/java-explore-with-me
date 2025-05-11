package ru.practicum.stats.app;

import org.mapstruct.Mapper;
import ru.practicum.stats.dto.EndpointHitDto;

@Mapper(componentModel = "spring")
public interface EndpointHitMapper {
    EndpointHit toEntity(EndpointHitDto dto);
}

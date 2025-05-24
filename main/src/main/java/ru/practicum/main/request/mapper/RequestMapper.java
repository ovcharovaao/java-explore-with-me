package ru.practicum.main.request.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.main.request.dto.ParticipationRequestDto;
import ru.practicum.main.request.model.Request;

@Mapper(componentModel = "spring")
public interface RequestMapper {
    @Mapping(source = "event.id", target = "event")
    @Mapping(source = "requester.id", target = "requester")
    ParticipationRequestDto toDto(Request request);
}

package ru.practicum.main.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Named;
import ru.practicum.main.user.dto.UserDto;
import ru.practicum.main.user.dto.UserShortDto;
import ru.practicum.main.user.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(UserDto dto);

    UserDto toDto(User user);

    @Named("toUserShortDto")
    UserShortDto toUserShortDto(User user);
}

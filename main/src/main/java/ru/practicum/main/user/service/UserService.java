package ru.practicum.main.user.service;

import ru.practicum.main.user.dto.UserDto;

import java.util.List;

public interface UserService {
    UserDto createUser(UserDto userDto);

    List<UserDto> getUsers(List<Long> ids, int from, int size);

    void deleteUser(Long userId);
}

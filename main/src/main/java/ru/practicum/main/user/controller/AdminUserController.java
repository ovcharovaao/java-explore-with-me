package ru.practicum.main.user.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.user.dto.UserDto;
import ru.practicum.main.user.service.UserService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserController {
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody UserDto userDto) {
        log.info("Создание пользователя: {}", userDto);
        return userService.createUser(userDto);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<UserDto> getUsers(@RequestParam(required = false) List<Long> ids,
                                  @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                  @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("Получение списка пользователей. IDs: {}, from: {}, size: {}", ids, from, size);
        return userService.getUsers(ids, from, size);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long userId) {
        log.info("Удаление пользователя с ID: {}", userId);
        userService.deleteUser(userId);
    }
}

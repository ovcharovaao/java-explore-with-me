package ru.practicum.main.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.user.dto.UserDto;
import ru.practicum.main.user.mapper.UserMapper;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Test
    @DisplayName("Успешное создание пользователя")
    void createUserSuccess() {
        UserDto userDto = new UserDto();
        userDto.setEmail("test@example.com");
        User user = new User();
        user.setId(1L);

        when(userMapper.toEntity(userDto)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = userService.createUser(userDto);

        assertEquals(userDto, result);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Создание пользователя — конфликт по email")
    void createUserConflict() {
        UserDto userDto = new UserDto();
        userDto.setEmail("duplicate@example.com");

        when(userMapper.toEntity(userDto)).thenReturn(new User());
        when(userRepository.save(any())).thenThrow(DataIntegrityViolationException.class);

        assertThrows(ConflictException.class, () -> userService.createUser(userDto));
    }

    @Test
    @DisplayName("Успешное получение всех пользователей без ID")
    void getUsersWithoutIds() {
        User user = new User();
        UserDto userDto = new UserDto();

        when(userRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(userMapper.toDto(user)).thenReturn(userDto);

        List<UserDto> result = userService.getUsers(null, 0, 10);

        assertEquals(1, result.size());
        verify(userRepository).findAll(any(PageRequest.class));
    }

    @Test
    @DisplayName("Успешное получение пользователей по списку ID")
    void getUsersWithIds() {
        User user = new User();
        UserDto userDto = new UserDto();
        List<Long> ids = List.of(1L, 2L);

        when(userRepository.findAllByIdIn(eq(ids), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(userMapper.toDto(user)).thenReturn(userDto);

        List<UserDto> result = userService.getUsers(ids, 0, 10);

        assertEquals(1, result.size());
        verify(userRepository).findAllByIdIn(eq(ids), any(PageRequest.class));
    }

    @Test
    @DisplayName("Успешное удаление пользователя")
    void deleteUserSuccess() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Удаление пользователя — пользователь не найден")
    void deleteUserNotFound() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> userService.deleteUser(99L));
    }

    @Test
    @DisplayName("Получение пользователей — пустой список при пустом запросе")
    void getUsersEmptyResult() {
        when(userRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        List<UserDto> result = userService.getUsers(null, 0, 10);

        assertTrue(result.isEmpty());
    }
}

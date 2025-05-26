package ru.practicum.main.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.user.dto.UserDto;
import ru.practicum.main.user.service.UserService;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
class AdminUserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /admin/users - успешное создание пользователя")
    void shouldCreateUser() throws Exception {
        UserDto newUserDto = new UserDto(null, "test@example.com", "Test User");
        UserDto createdUserDto = new UserDto(1L, "test@example.com", "Test User");

        Mockito.when(userService.createUser(any(UserDto.class))).thenReturn(createdUserDto);

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    @DisplayName("POST /admin/users - некорректный email")
    void shouldFailValidationWhenEmailIsInvalid() throws Exception {
        UserDto invalidUserDto = new UserDto(null, "invalid-email", "Test User");

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUserDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /admin/users - email уже существует")
    void shouldReturnConflictWhenEmailExists() throws Exception {
        UserDto newUserDto = new UserDto(null, "existing@example.com", "Existing User");

        Mockito.when(userService.createUser(any(UserDto.class)))
                .thenThrow(new ConflictException("Пользователь с таким email уже существует."));

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserDto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /admin/users - получение всех пользователей")
    void shouldGetAllUsers() throws Exception {
        UserDto user1 = new UserDto(1L, "user1@example.com", "User One");
        UserDto user2 = new UserDto(2L, "user2@example.com", "User Two");

        Mockito.when(userService.getUsers(Mockito.isNull(), eq(0), eq(10)))
                .thenReturn(List.of(user1, user2));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].name").value("User Two"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /admin/users - получение пользователей по ID")
    void shouldGetUsersByIds() throws Exception {
        UserDto user1 = new UserDto(1L, "user1@example.com", "User One");

        Mockito.when(userService.getUsers(eq(List.of(1L)), eq(0), eq(10)))
                .thenReturn(List.of(user1));

        mockMvc.perform(get("/admin/users")
                        .param("ids", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("DELETE /admin/users/{userId} - успешное удаление пользователя")
    void shouldDeleteUser() throws Exception {
        Long userId = 1L;

        mockMvc.perform(delete("/admin/users/{userId}", userId))
                .andExpect(status().isNoContent());

        Mockito.verify(userService).deleteUser(userId);
    }

    @Test
    @DisplayName("DELETE /admin/users/{userId} - пользователь не найден для удаления")
    void shouldReturnNotFoundWhenDeletingUserNotFound() throws Exception {
        Long userId = 99L;

        Mockito.doThrow(new NotFoundException("Пользователь с ID " + userId + " не найден."))
                .when(userService).deleteUser(userId);

        mockMvc.perform(delete("/admin/users/{userId}", userId))
                .andExpect(status().isNotFound());
    }
}

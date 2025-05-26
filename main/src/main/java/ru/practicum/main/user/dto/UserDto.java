package ru.practicum.main.user.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;

    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Некорректный формат email")
    @Size(min = 6, max = 254, message = "Email должен быть от 6 до 254 символов")
    private String email;

    @NotBlank(message = "Имя не может быть пустым")
    @Size(min = 2, max = 250, message = "Имя должно быть от 2 до 250 символов")
    private String name;
}

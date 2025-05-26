package ru.practicum.stats.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointHitDto {
    @NotBlank(message = "Название сервиса не должно быть пустым")
    private String app;

    @NotBlank(message = "URI не должен быть пустым")
    private String uri;

    @NotBlank(message = "IP-адрес не должен быть пустым")
    private String ip;

    @NotNull(message = "Дата и время не должны быть null")
    @Past(message = "Дата и время должны быть в прошлом")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}

package ru.practicum.main.location.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {
    @NotNull(message = "Широта не может быть null")
    private Float lat;

    @NotNull(message = "Долгота не может быть null")
    private Float lon;
}

package ru.practicum.main.user.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserShortDto {
    private Long id;
    private String name;
}

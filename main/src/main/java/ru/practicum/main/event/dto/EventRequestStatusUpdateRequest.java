package ru.practicum.main.event.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.practicum.main.request.model.RequestStatus;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateRequest {
    @NotEmpty(message = "Список ID запросов не может быть пустым")
    private List<Long> requestIds;

    @NotNull(message = "Статус не может быть null")
    private RequestStatus status;
}

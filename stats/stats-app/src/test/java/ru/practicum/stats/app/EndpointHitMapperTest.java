package ru.practicum.stats.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.practicum.stats.dto.EndpointHitDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EndpointHitMapperTest {
    private final EndpointHitMapper mapper = Mappers.getMapper(EndpointHitMapper.class);

    @Test
    @DisplayName("Должен корректно мапить DTO в Entity")
    void shouldMapDtoToEntity() {
        LocalDateTime timestamp = LocalDateTime.now();
        EndpointHitDto dto = new EndpointHitDto("my-app", "/test", "127.0.0.1", timestamp);

        EndpointHit entity = mapper.toEntity(dto);

        assertThat(entity).isNotNull();
        assertThat(entity.getApp()).isEqualTo("my-app");
        assertThat(entity.getUri()).isEqualTo("/test");
        assertThat(entity.getIp()).isEqualTo("127.0.0.1");
        assertThat(entity.getTimestamp()).isEqualTo(timestamp);
        assertThat(entity.getId()).isNull();
    }
}

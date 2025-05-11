package ru.practicum.stats.app;

import org.springframework.stereotype.Component;
import ru.practicum.stats.dto.EndpointHitDto;

@Component
public class EndpointHitMapper {
    public EndpointHit toEntity(EndpointHitDto dto) {
        EndpointHit hit = new EndpointHit();
        hit.setApp(dto.getApp());
        hit.setUri(dto.getUri());
        hit.setIp(dto.getIp());
        hit.setTimestamp(dto.getTimestamp());
        return hit;
    }
}

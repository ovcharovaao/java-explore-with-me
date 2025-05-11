package ru.practicum.stats.app;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {
    private final StatsRepository repository;
    private final EndpointHitMapper mapper;

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void save(EndpointHitDto dto) {
        EndpointHit entity = mapper.toEntity(dto);
        repository.save(entity);
    }

    public List<ViewStats> getStats(String start, String end, List<String> uris, boolean unique) {
        LocalDateTime st = LocalDateTime.parse(start, FORMAT);
        LocalDateTime en = LocalDateTime.parse(end, FORMAT);
        if (uris != null && uris.isEmpty()) uris = null;
        return unique
                ? repository.findUniqueStats(st, en, uris)
                : repository.findStats(st, en, uris);
    }
}

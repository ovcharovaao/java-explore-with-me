package ru.practicum.stats.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.stats.app.mapper.EndpointHitMapper;
import ru.practicum.stats.app.model.EndpointHit;
import ru.practicum.stats.app.repository.StatsRepository;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {
    private final StatsRepository repository;
    private final EndpointHitMapper mapper;

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void save(EndpointHitDto dto) {
        EndpointHit entity = mapper.toEntity(dto);
        repository.save(entity);
        log.info("Сохранён хит: {}", entity);
    }

    public List<ViewStats> getStats(String start, String end, List<String> uris, boolean unique) {
        LocalDateTime st = LocalDateTime.parse(start, FORMAT);
        LocalDateTime en = LocalDateTime.parse(end, FORMAT);

        if (uris != null && uris.isEmpty()) uris = null;

        log.info("Запрос статистики с параметрами: start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        List<ViewStats> stats = unique
                ? repository.findUniqueStats(st, en, uris)
                : repository.findStats(st, en, uris);

        log.info("Результат запроса статистики: {} записей", stats.size());
        return stats;
    }
}

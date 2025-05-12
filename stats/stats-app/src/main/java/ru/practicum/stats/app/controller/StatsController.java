package ru.practicum.stats.app.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.stats.app.service.StatsService;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStats;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class StatsController {
    private final StatsService service;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveHit(@RequestBody EndpointHitDto dto) {
        log.info("Получен запрос на сохранение хита: {}", dto);
        service.save(dto);
    }

    @GetMapping("/stats")
    public List<ViewStats> getStats(@RequestParam String start, @RequestParam String end,
                                    @RequestParam(required = false) List<String> uris,
                                    @RequestParam(defaultValue = "false") boolean unique) {
        log.info("Получен запрос на получение статистики: start={}, end={}, uris={}, unique={}",
                start, end, uris, unique);
        return service.getStats(start, end, uris, unique);
    }
}

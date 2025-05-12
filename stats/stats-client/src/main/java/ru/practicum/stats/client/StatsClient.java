package ru.practicum.stats.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClient {
    private final RestTemplate rest;
    private final String baseUrl;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClient(RestTemplateBuilder builder, @Value("${stats.url}") String url) {
        this.rest = builder.build();
        this.baseUrl = url;
    }

    public void sendHit(EndpointHitDto hit) {
        log.info("Отправка хита в сервис статистики: {}", hit);
        rest.postForEntity(baseUrl + "/hit", hit, Void.class);
        log.info("Хит успешно отправлен.");
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end,
                                    List<String> uris, boolean unique) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/stats")
                .queryParam("start", start.format(FMT))
                .queryParam("end", end.format(FMT))
                .queryParamIfPresent("uris", Optional.ofNullable(uris))
                .queryParam("unique", unique)
                .toUriString();

        log.info("Отправка запроса статистики: {}", url);

        ResponseEntity<ViewStats[]> resp = rest.getForEntity(url, ViewStats[].class);

        ViewStats[] body = resp.getBody();
        int count = body == null ? 0 : body.length;

        log.info("Получено {} записей статистики", count);
        return Arrays.asList(Objects.requireNonNull(body));
    }
}

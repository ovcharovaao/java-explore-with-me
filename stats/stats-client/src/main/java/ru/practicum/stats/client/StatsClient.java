package ru.practicum.stats.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
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
        StringBuilder urlBuilder = new StringBuilder(baseUrl + "/stats");
        urlBuilder.append("?start=").append(start.format(FMT));
        urlBuilder.append("&end=").append(end.format(FMT));
        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                urlBuilder.append("&uris=").append(uri);
            }
        }
        urlBuilder.append("&unique=").append(unique);

        String url = urlBuilder.toString();
        log.info("Отправка запроса статистики: {}", url);

        ResponseEntity<ViewStats[]> resp = rest.getForEntity(url, ViewStats[].class);
        ViewStats[] body = resp.getBody();
        int count = body == null ? 0 : body.length;

        log.info("Получено {} записей статистики", count);
        return Arrays.asList(Objects.requireNonNull(body));
    }
}

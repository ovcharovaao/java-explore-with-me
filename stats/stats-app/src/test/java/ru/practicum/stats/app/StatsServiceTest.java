package ru.practicum.stats.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StatsServiceTest {
    private StatsRepository repository;
    private EndpointHitMapper mapper;
    private StatsService service;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @BeforeEach
    void setUp() {
        repository = mock(StatsRepository.class);
        mapper = mock(EndpointHitMapper.class);
        service = new StatsService(repository, mapper);
    }

    @DisplayName("Сохранение хита должно маппировать DTO в сущность и сохранить её")
    @Test
    void save_ShouldMapAndSaveEntity() {
        EndpointHitDto dto = new EndpointHitDto("app", "/uri", "127.0.0.1",
                LocalDateTime.parse("2025-05-11 10:00:00", FMT));
        EndpointHit entity = new EndpointHit();
        when(mapper.toEntity(dto)).thenReturn(entity);

        service.save(dto);

        verify(mapper, times(1)).toEntity(dto);
        verify(repository, times(1)).save(entity);
    }

    @DisplayName("Получение статистики без уникальности и без фильтрации URI должно вызывать findStats")
    @Test
    void getStats_ShouldCallFindStats_WhenUniqueFalseAndUrisNull() {
        String start = "2025-05-01 00:00:00";
        String end = "2025-05-31 23:59:59";
        when(repository.findStats(any(), any(), isNull()))
                .thenReturn(List.of(new ViewStats("app", "/uri", 5L)));

        List<ViewStats> stats = service.getStats(start, end, null, false);

        ArgumentCaptor<LocalDateTime> captStart = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> captEnd = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).findStats(captStart.capture(), captEnd.capture(), isNull());
        assertThat(captStart.getValue()).isEqualTo(LocalDateTime.parse(start, FMT));
        assertThat(captEnd.getValue()).isEqualTo(LocalDateTime.parse(end, FMT));
        assertThat(stats).hasSize(1)
                .allMatch(v -> v.getHits() == 5L);
    }

    @DisplayName("Получение уникальной статистики должно вызывать findUniqueStats")
    @Test
    void getStats_ShouldCallFindUniqueStats_WhenUniqueTrue() {
        String start = "2025-05-01 00:00:00";
        String end = "2025-05-31 23:59:59";
        List<String> uris = List.of("/a", "/b");

        when(repository.findUniqueStats(any(), any(), eq(uris)))
                .thenReturn(List.of(new ViewStats("app", "/a", 2L)));

        List<ViewStats> stats = service.getStats(start, end, uris, true);

        verify(repository, never()).findStats(any(), any(), any());
        verify(repository, times(1)).findUniqueStats(
                eq(LocalDateTime.parse(start, FMT)),
                eq(LocalDateTime.parse(end, FMT)),
                eq(uris)
        );
        assertThat(stats).extracting(ViewStats::getUri)
                .containsExactly("/a");
    }

    @DisplayName("Пустой список URI должен трактоваться как null при получении статистики")
    @Test
    void getStats_ShouldTreatEmptyUrisAsNull() {
        String start = "2025-05-01 00:00:00";
        String end = "2025-05-31 23:59:59";
        List<String> empty = Collections.emptyList();

        when(repository.findStats(any(), any(), isNull()))
                .thenReturn(Collections.emptyList());

        service.getStats(start, end, empty, false);

        verify(repository).findStats(any(), any(), isNull());
    }
}

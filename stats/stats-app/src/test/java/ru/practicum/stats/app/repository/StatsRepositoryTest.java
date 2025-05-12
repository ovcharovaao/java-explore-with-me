package ru.practicum.stats.app.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.practicum.stats.app.model.EndpointHit;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class StatsRepositoryTest {
    @Autowired
    private StatsRepository repository;

    @Test
    @DisplayName("EndpointHit должен корректно сохраняться и читаться из базы")
    void shouldPersistAndReadEndpointHitCorrectly() {
        LocalDateTime ts = LocalDateTime.now().minusMinutes(10);
        EndpointHit hit = new EndpointHit(null, "service", "/test", "8.8.8.8", ts);

        EndpointHit saved = repository.save(hit);
        EndpointHit found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getApp()).isEqualTo("service");
        assertThat(found.getUri()).isEqualTo("/test");
        assertThat(found.getIp()).isEqualTo("8.8.8.8");
        assertThat(found.getTimestamp()).isEqualTo(ts);
    }

    @Test
    @DisplayName("findStats должен вернуть данные о просмотрах с сортировкой")
    void shouldReturnStatsSortedByCount() {
        LocalDateTime now = LocalDateTime.now();
        repository.save(new EndpointHit(null, "app", "/a", "1.1.1.1", now.minusDays(1)));
        repository.save(new EndpointHit(null, "app", "/a", "1.1.1.2", now.minusDays(1)));
        repository.save(new EndpointHit(null, "app", "/b", "1.1.1.3", now.minusDays(1)));

        List<ViewStats> stats = repository.findStats(
                now.minusDays(2), now.plusDays(1), null
        );

        assertThat(stats).hasSize(2);
        assertThat(stats.get(0).getUri()).isEqualTo("/a");
        assertThat(stats.get(0).getHits()).isEqualTo(2);
        assertThat(stats.get(1).getHits()).isEqualTo(1);
    }

    @Test
    @DisplayName("findUniqueStats должен учитывать уникальные IP")
    void shouldReturnUniqueStatsSortedByIpCount() {
        LocalDateTime now = LocalDateTime.now();
        repository.save(new EndpointHit(null, "app", "/x", "1.1.1.1", now.minusHours(1)));
        repository.save(new EndpointHit(null, "app", "/x", "1.1.1.1", now.minusHours(2)));
        repository.save(new EndpointHit(null, "app", "/x", "1.1.1.2", now.minusHours(3)));

        List<ViewStats> stats = repository.findUniqueStats(
                now.minusDays(1), now.plusDays(1), null
        );

        assertThat(stats).hasSize(1);
        assertThat(stats.get(0).getUri()).isEqualTo("/x");
        assertThat(stats.get(0).getHits()).isEqualTo(2);
    }
}

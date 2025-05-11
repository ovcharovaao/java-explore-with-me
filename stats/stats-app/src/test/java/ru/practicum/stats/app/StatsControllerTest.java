package ru.practicum.stats.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private StatsService service;

    @Autowired
    private ObjectMapper mapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    @DisplayName("POST /hit должен вернуть 201 и вызвать сервис")
    void saveHit_ShouldReturnCreated() throws Exception {
        EndpointHitDto dto = new EndpointHitDto(
                "main-service", "/event", "192.168.0.1",
                LocalDateTime.parse("2025-05-11 12:00:00", FMT)
        );

        mvc.perform(post("/hit")
                        .content(mapper.writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        Mockito.verify(service).save(any());
    }

    @Test
    @DisplayName("GET /stats должен вернуть список статистики")
    void getStats_ShouldReturnStatsList() throws Exception {
        String start = "2020-01-01 00:00:00";
        String end = "2030-01-01 00:00:00";

        List<ViewStats> stats = List.of(
                new ViewStats("main", "/x", 10L),
                new ViewStats("main", "/y", 5L)
        );

        Mockito.when(service.getStats(start, end, null, false)).thenReturn(stats);

        mvc.perform(get("/stats")
                        .param("start", start)
                        .param("end", end)
                        .param("unique", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].uri").value("/x"))
                .andExpect(jsonPath("$[0].hits").value(10));
    }
}

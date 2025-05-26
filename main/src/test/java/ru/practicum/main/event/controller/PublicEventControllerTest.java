package ru.practicum.main.event.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.service.EventService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicEventController.class)
class PublicEventControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @Test
    @DisplayName("GET /events - успешное получение опубликованных событий")
    void getPublishedEvents_shouldReturnOk() throws Exception {
        Mockito.when(eventService.getPublishedEvents(
                        any(), any(), any(), any(), any(), anyBoolean(),
                        any(), anyInt(), anyInt(), any()))
                .thenReturn(List.of(new EventShortDto()));

        mockMvc.perform(get("/events")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /events/{id} - успешное получение опубликованного события по ID")
    void getPublishedEventById_shouldReturnOk() throws Exception {
        Mockito.when(eventService.getPublishedEventById(eq(1L), any()))
                .thenReturn(new EventFullDto());

        mockMvc.perform(get("/events/1"))
                .andExpect(status().isOk());
    }
}

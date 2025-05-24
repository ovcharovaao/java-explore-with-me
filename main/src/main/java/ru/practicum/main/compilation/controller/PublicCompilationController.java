package ru.practicum.main.compilation.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.compilation.dto.CompilationDto;
import ru.practicum.main.compilation.service.CompilationService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/compilations")
public class PublicCompilationController {
    private final CompilationService compilationService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CompilationDto> getCompilations(@RequestParam(required = false) Boolean pinned,
                                                @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                                @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("Получение подборок {}, from: {}, size: {}", pinned, from, size);
        return compilationService.getCompilations(pinned, from, size);
    }

    @GetMapping("/{compId}")
    @ResponseStatus(HttpStatus.OK)
    public CompilationDto getCompilationById(@PathVariable Long compId) {
        log.info("Получение подборки по ID: {}", compId);
        return compilationService.getCompilationById(compId);
    }
}

package ru.practicum.main.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.compilation.dto.CompilationDto;
import ru.practicum.main.compilation.dto.NewCompilationDto;
import ru.practicum.main.compilation.dto.UpdateCompilationRequest;
import ru.practicum.main.compilation.mapper.CompilationMapper;
import ru.practicum.main.compilation.model.Compilation;
import ru.practicum.main.compilation.repository.CompilationRepository;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.event.service.EventService;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;
    private final EventService eventService;

    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        log.info("Создание новой подборки: {}", newCompilationDto.getTitle());

        if (compilationRepository.findByTitle(newCompilationDto.getTitle()).isPresent()) {
            throw new ConflictException("Подборка с таким названием уже существует: " + newCompilationDto.getTitle());
        }

        Compilation compilation = Compilation.builder()
                .title(newCompilationDto.getTitle())
                .pinned(newCompilationDto.getPinned())
                .build();

        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            Set<Long> eventIds = new HashSet<>(newCompilationDto.getEvents());
            Set<Event> events = new HashSet<>(eventRepository.findAllByIdIn(eventIds));

            if (events.size() != newCompilationDto.getEvents().size()) {
                List<Long> foundEventIds = events.stream().map(Event::getId).toList();
                List<Long> notFoundEventIds = newCompilationDto.getEvents().stream()
                        .filter(id -> !foundEventIds.contains(id))
                        .collect(Collectors.toList());
                log.warn("Некоторые события не найдены: {}", notFoundEventIds);
            }

            compilation.setEvents(events);
        }

        Compilation savedCompilation = compilationRepository.save(compilation);

        log.info("Подборка успешно создана с ID: {}", savedCompilation.getId());
        return getFullCompilationDto(savedCompilation);
    }

    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateCompilationRequest) {
        log.info("Обновление подборки ID: {}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с ID " + compId + " не найдена"));

        if (updateCompilationRequest.getTitle() != null
                && !compilation.getTitle().equals(updateCompilationRequest.getTitle())) {
            if (compilationRepository.findByTitle(updateCompilationRequest.getTitle()).isPresent()) {
                throw new ConflictException("Подборка с таким названием уже существует: "
                        + updateCompilationRequest.getTitle());
            }

            compilation.setTitle(updateCompilationRequest.getTitle());
        }

        if (updateCompilationRequest.getPinned() != null) {
            compilation.setPinned(updateCompilationRequest.getPinned());
        }

        if (updateCompilationRequest.getEvents() != null) {
            if (updateCompilationRequest.getEvents().isEmpty()) {
                compilation.setEvents(Collections.emptySet());
            } else {
                Set<Long> eventIds = new HashSet<>(updateCompilationRequest.getEvents());
                Set<Event> events = new HashSet<>(eventRepository.findAllByIdIn(eventIds));
                if (events.size() != updateCompilationRequest.getEvents().size()) {
                    List<Long> foundEventIds = events.stream().map(Event::getId).toList();
                    List<Long> notFoundEventIds = updateCompilationRequest.getEvents().stream()
                            .filter(id -> !foundEventIds.contains(id))
                            .collect(Collectors.toList());
                    log.warn("Некоторые события не найдены при обновлении подборки: {}", notFoundEventIds);
                }

                compilation.setEvents(events);
            }
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);

        log.info("Подборка ID: {} успешно обновлена", updatedCompilation.getId());
        return getFullCompilationDto(updatedCompilation);
    }

    @Transactional
    public void deleteCompilation(Long compId) {
        log.info("Удаление подборки ID: {}", compId);

        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Подборка с ID " + compId + " не найдена");
        }

        compilationRepository.deleteById(compId);
        log.info("Подборка ID: {} успешно удалена", compId);
    }

    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        log.info("Получение подборок. Закрепленные: {}, from: {}, size: {}", pinned, from, size);

        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<Compilation> compilations;

        if (pinned == null) {
            compilations = compilationRepository.findAll(pageRequest).getContent();
        } else {
            compilations = compilationRepository.findByPinned(pinned, pageRequest).getContent();
        }

        log.info("Получено {} подборок.", compilations.size());
        return compilations.stream()
                .map(this::getFullCompilationDto)
                .collect(Collectors.toList());
    }

    public CompilationDto getCompilationById(Long compId) {
        log.info("Получение подборки по ID: {}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с ID " + compId + " не найдена"));

        log.info("Подборка ID: {} найдена", compId);
        return getFullCompilationDto(compilation);
    }

    private CompilationDto getFullCompilationDto(Compilation compilation) {
        CompilationDto dto = compilationMapper.toDto(compilation);

        if (compilation.getEvents() != null && !compilation.getEvents().isEmpty()) {
            List<Event> eventsList = new ArrayList<>(compilation.getEvents());
            dto.setEvents(eventService.addViewsAndConfirmedRequestsToShortEvents(eventsList));
        } else {
            dto.setEvents(Collections.emptyList());
        }

        return dto;
    }
}

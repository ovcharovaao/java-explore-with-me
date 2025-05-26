package ru.practicum.main.compilation.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.main.compilation.dto.CompilationDto;
import ru.practicum.main.compilation.dto.NewCompilationDto;
import ru.practicum.main.compilation.dto.UpdateCompilationRequest;
import ru.practicum.main.compilation.mapper.CompilationMapper;
import ru.practicum.main.compilation.model.Compilation;
import ru.practicum.main.compilation.repository.CompilationRepository;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.event.service.EventService;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

class CompilationServiceImplTest {
    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CompilationMapper compilationMapper;

    @Mock
    private EventService eventService;

    @InjectMocks
    private CompilationServiceImpl compilationService;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    @DisplayName("Успешное создание подборки без событий")
    void createCompilation_successWithoutEvents() {
        NewCompilationDto newDto = new NewCompilationDto();
        newDto.setTitle("New Compilation");
        newDto.setPinned(false);
        newDto.setEvents(Collections.emptyList());

        Compilation compilation = new Compilation();
        compilation.setId(1L);
        compilation.setTitle("New Compilation");
        compilation.setPinned(false);
        compilation.setEvents(Collections.emptySet());

        CompilationDto compilationDto = new CompilationDto();
        compilationDto.setId(1L);
        compilationDto.setTitle("New Compilation");
        compilationDto.setPinned(false);
        compilationDto.setEvents(Collections.emptyList());

        when(compilationRepository.findByTitle(anyString())).thenReturn(Optional.empty());
        when(compilationRepository.save(any(Compilation.class))).thenReturn(compilation);
        when(compilationMapper.toDto(any(Compilation.class))).thenReturn(compilationDto);
        when(eventService.addViewsAndConfirmedRequestsToShortEvents(anyList())).thenReturn(Collections.emptyList());

        CompilationDto result = compilationService.createCompilation(newDto);

        assertThat(result).isEqualTo(compilationDto);
        verify(compilationRepository).findByTitle("New Compilation");
        verify(compilationRepository).save(any(Compilation.class));
        verify(compilationMapper).toDto(any(Compilation.class));
        verify(eventService, never()).addViewsAndConfirmedRequestsToShortEvents(anyList());
    }

    @Test
    @DisplayName("Успешное создание подборки")
    void createCompilation_successWithEvents() {
        NewCompilationDto newDto = new NewCompilationDto();
        newDto.setTitle("New Compilation With Events");
        newDto.setPinned(true);
        newDto.setEvents(List.of(1L, 2L));

        Event event1 = new Event();
        event1.setId(1L);
        Event event2 = new Event();
        event2.setId(2L);
        Set<Event> events = new HashSet<>(List.of(event1, event2));

        Compilation compilation = new Compilation();
        compilation.setId(1L);
        compilation.setTitle("New Compilation With Events");
        compilation.setPinned(true);
        compilation.setEvents(events);

        EventShortDto eventShortDto1 = new EventShortDto();
        eventShortDto1.setId(1L);
        EventShortDto eventShortDto2 = new EventShortDto();
        eventShortDto2.setId(2L);
        List<EventShortDto> eventShortDtos = List.of(eventShortDto1, eventShortDto2);

        CompilationDto compilationDto = new CompilationDto();
        compilationDto.setId(1L);
        compilationDto.setTitle("New Compilation With Events");
        compilationDto.setPinned(true);
        compilationDto.setEvents(eventShortDtos);

        when(compilationRepository.findByTitle(anyString())).thenReturn(Optional.empty());
        when(eventRepository.findAllByIdIn(anySet())).thenReturn(new ArrayList<>(events));
        when(compilationRepository.save(any(Compilation.class))).thenReturn(compilation);
        when(compilationMapper.toDto(any(Compilation.class))).thenReturn(compilationDto);
        when(eventService.addViewsAndConfirmedRequestsToShortEvents(anyList())).thenReturn(eventShortDtos);

        CompilationDto result = compilationService.createCompilation(newDto);

        assertThat(result).isEqualTo(compilationDto);
        verify(compilationRepository).findByTitle("New Compilation With Events");
        verify(eventRepository).findAllByIdIn(new HashSet<>(List.of(1L, 2L)));
        verify(compilationRepository).save(any(Compilation.class));
        verify(compilationMapper).toDto(any(Compilation.class));
        verify(eventService).addViewsAndConfirmedRequestsToShortEvents(new ArrayList<>(events));
    }

    @Test
    @DisplayName("Создание подборки - название уже существует")
    void createCompilation_conflictTitleExists() {
        NewCompilationDto newDto = new NewCompilationDto();
        newDto.setTitle("Existing Title");
        newDto.setPinned(false);

        when(compilationRepository.findByTitle(anyString())).thenReturn(Optional.of(new Compilation()));

        assertThatThrownBy(() -> compilationService.createCompilation(newDto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Подборка с таким названием уже существует");

        verify(compilationRepository).findByTitle("Existing Title");
        verify(compilationRepository, never()).save(any(Compilation.class));
    }

    @Test
    @DisplayName("Создание подборки - некоторые события не найдены")
    void createCompilation_someEventsNotFound() {
        NewCompilationDto newDto = new NewCompilationDto();
        newDto.setTitle("New Compilation With Missing Events");
        newDto.setPinned(false);
        newDto.setEvents(List.of(1L, 99L));

        Event event1 = new Event();
        event1.setId(1L);
        Set<Event> foundEvents = new HashSet<>(List.of(event1));

        Compilation compilation = new Compilation();
        compilation.setId(1L);
        compilation.setTitle("New Compilation With Missing Events");
        compilation.setPinned(false);
        compilation.setEvents(foundEvents);

        EventShortDto eventShortDto1 = new EventShortDto();
        eventShortDto1.setId(1L);
        List<EventShortDto> eventShortDtos = List.of(eventShortDto1);

        CompilationDto compilationDto = new CompilationDto();
        compilationDto.setId(1L);
        compilationDto.setTitle("New Compilation With Missing Events");
        compilationDto.setPinned(false);
        compilationDto.setEvents(eventShortDtos);

        when(compilationRepository.findByTitle(anyString())).thenReturn(Optional.empty());
        when(eventRepository.findAllByIdIn(anySet())).thenReturn(new ArrayList<>(foundEvents));
        when(compilationRepository.save(any(Compilation.class))).thenReturn(compilation);
        when(compilationMapper.toDto(any(Compilation.class))).thenReturn(compilationDto);
        when(eventService.addViewsAndConfirmedRequestsToShortEvents(anyList())).thenReturn(eventShortDtos);

        CompilationDto result = compilationService.createCompilation(newDto);

        assertThat(result).isEqualTo(compilationDto);
        verify(compilationRepository).findByTitle("New Compilation With Missing Events");
        verify(eventRepository).findAllByIdIn(new HashSet<>(List.of(1L, 99L)));
        verify(compilationRepository).save(any(Compilation.class));
        verify(compilationMapper).toDto(any(Compilation.class));
        verify(eventService).addViewsAndConfirmedRequestsToShortEvents(new ArrayList<>(foundEvents));
    }

    @Test
    @DisplayName("Успешное обновление подборки")
    void updateCompilation_successFullUpdate() {
        Long compId = 1L;
        UpdateCompilationRequest updateRequest = new UpdateCompilationRequest();
        updateRequest.setTitle("Updated Title");
        updateRequest.setPinned(true);
        updateRequest.setEvents(List.of(10L, 20L));

        Compilation existingCompilation = new Compilation();
        existingCompilation.setId(compId);
        existingCompilation.setTitle("Old Title");
        existingCompilation.setPinned(false);
        existingCompilation.setEvents(Collections.emptySet());

        Event event10 = new Event();
        event10.setId(10L);
        Event event20 = new Event();
        event20.setId(20L);
        Set<Event> updatedEvents = new HashSet<>(List.of(event10, event20));

        Compilation updatedCompilation = new Compilation();
        updatedCompilation.setId(compId);
        updatedCompilation.setTitle("Updated Title");
        updatedCompilation.setPinned(true);
        updatedCompilation.setEvents(updatedEvents);

        EventShortDto eventShortDto10 = new EventShortDto();
        eventShortDto10.setId(10L);
        EventShortDto eventShortDto20 = new EventShortDto();
        eventShortDto20.setId(20L);
        List<EventShortDto> updatedEventShortDtos = List.of(eventShortDto10, eventShortDto20);

        CompilationDto compilationDto = new CompilationDto();
        compilationDto.setId(compId);
        compilationDto.setTitle("Updated Title");
        compilationDto.setPinned(true);
        compilationDto.setEvents(updatedEventShortDtos);

        when(compilationRepository.findById(compId)).thenReturn(Optional.of(existingCompilation));
        when(compilationRepository.findByTitle(anyString())).thenReturn(Optional.empty());
        when(eventRepository.findAllByIdIn(anySet())).thenReturn(new ArrayList<>(updatedEvents));
        when(compilationRepository.save(any(Compilation.class))).thenReturn(updatedCompilation);
        when(compilationMapper.toDto(any(Compilation.class))).thenReturn(compilationDto);
        when(eventService.addViewsAndConfirmedRequestsToShortEvents(anyList())).thenReturn(updatedEventShortDtos);

        CompilationDto result = compilationService.updateCompilation(compId, updateRequest);

        assertThat(result).isEqualTo(compilationDto);
        verify(compilationRepository).findById(compId);
        verify(compilationRepository).findByTitle("Updated Title");
        verify(eventRepository).findAllByIdIn(new HashSet<>(List.of(10L, 20L)));
        verify(compilationRepository).save(existingCompilation);
        verify(compilationMapper).toDto(any(Compilation.class));
        verify(eventService).addViewsAndConfirmedRequestsToShortEvents(new ArrayList<>(updatedEvents));
    }

    @Test
    @DisplayName("Успешное обновление подборки (только изменение закрепления)")
    void updateCompilation_successOnlyPinnedChange() {
        Long compId = 1L;
        UpdateCompilationRequest updateRequest = new UpdateCompilationRequest();
        updateRequest.setPinned(true);

        Compilation existingCompilation = new Compilation();
        existingCompilation.setId(compId);
        existingCompilation.setTitle("Original Title");
        existingCompilation.setPinned(false);
        existingCompilation.setEvents(Collections.emptySet());

        Compilation updatedCompilation = new Compilation();
        updatedCompilation.setId(compId);
        updatedCompilation.setTitle("Original Title");
        updatedCompilation.setPinned(true);
        updatedCompilation.setEvents(Collections.emptySet());

        CompilationDto compilationDto = new CompilationDto();
        compilationDto.setId(compId);
        compilationDto.setTitle("Original Title");
        compilationDto.setPinned(true);
        compilationDto.setEvents(Collections.emptyList());

        when(compilationRepository.findById(compId)).thenReturn(Optional.of(existingCompilation));
        when(compilationRepository.save(any(Compilation.class))).thenReturn(updatedCompilation);
        when(compilationMapper.toDto(any(Compilation.class))).thenReturn(compilationDto);
        when(eventService.addViewsAndConfirmedRequestsToShortEvents(anyList())).thenReturn(Collections.emptyList());

        CompilationDto result = compilationService.updateCompilation(compId, updateRequest);

        assertThat(result).isEqualTo(compilationDto);
        assertThat(existingCompilation.getPinned()).isTrue();
        verify(compilationRepository).findById(compId);
        verify(compilationRepository, never()).findByTitle(anyString());
        verify(compilationRepository).save(existingCompilation);
        verify(compilationMapper).toDto(any(Compilation.class));
        verify(eventService, never()).addViewsAndConfirmedRequestsToShortEvents(anyList());
    }

    @Test
    @DisplayName("Успешное обновление подборки (удаление всех событий)")
    void updateCompilation_successRemoveAllEvents() {
        Long compId = 1L;
        UpdateCompilationRequest updateRequest = new UpdateCompilationRequest();
        updateRequest.setEvents(Collections.emptyList());

        Event event1 = new Event();
        event1.setId(1L);
        Compilation existingCompilation = new Compilation();
        existingCompilation.setId(compId);
        existingCompilation.setTitle("Title");
        existingCompilation.setPinned(false);
        existingCompilation.setEvents(new HashSet<>(List.of(event1)));

        Compilation updatedCompilation = new Compilation();
        updatedCompilation.setId(compId);
        updatedCompilation.setTitle("Title");
        updatedCompilation.setPinned(false);
        updatedCompilation.setEvents(Collections.emptySet());

        CompilationDto compilationDto = new CompilationDto();
        compilationDto.setId(compId);
        compilationDto.setTitle("Title");
        compilationDto.setPinned(false);
        compilationDto.setEvents(Collections.emptyList());

        when(compilationRepository.findById(compId)).thenReturn(Optional.of(existingCompilation));
        when(compilationRepository.save(any(Compilation.class))).thenReturn(updatedCompilation);
        when(compilationMapper.toDto(any(Compilation.class))).thenReturn(compilationDto);
        when(eventService.addViewsAndConfirmedRequestsToShortEvents(anyList())).thenReturn(Collections.emptyList());

        CompilationDto result = compilationService.updateCompilation(compId, updateRequest);

        assertThat(result).isEqualTo(compilationDto);
        assertThat(existingCompilation.getEvents()).isEmpty();
        verify(compilationRepository).findById(compId);
        verify(compilationRepository).save(existingCompilation);
        verify(compilationMapper).toDto(any(Compilation.class));
        verify(eventService, never()).addViewsAndConfirmedRequestsToShortEvents(anyList());
    }

    @Test
    @DisplayName("Обновление подборки - подборка не найдена")
    void updateCompilation_notFound() {
        Long compId = 99L;
        UpdateCompilationRequest updateRequest = new UpdateCompilationRequest();
        updateRequest.setTitle("New Title");

        when(compilationRepository.findById(compId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compilationService.updateCompilation(compId, updateRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Подборка с ID " + compId + " не найдена");

        verify(compilationRepository).findById(compId);
        verify(compilationRepository, never()).save(any(Compilation.class));
    }

    @Test
    @DisplayName("Обновление подборки - новое название уже существует")
    void updateCompilation_conflictTitleExists() {
        Long compId = 1L;
        UpdateCompilationRequest updateRequest = new UpdateCompilationRequest();
        updateRequest.setTitle("Existing Title");

        Compilation existingCompilation = new Compilation();
        existingCompilation.setId(compId);
        existingCompilation.setTitle("Old Title");

        when(compilationRepository.findById(compId)).thenReturn(Optional.of(existingCompilation));
        when(compilationRepository.findByTitle("Existing Title")).thenReturn(Optional.of(new Compilation()));

        assertThatThrownBy(() -> compilationService.updateCompilation(compId, updateRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Подборка с таким названием уже существует");

        verify(compilationRepository).findById(compId);
        verify(compilationRepository).findByTitle("Existing Title");
        verify(compilationRepository, never()).save(any(Compilation.class));
    }

    @Test
    @DisplayName("Успешное удаление подборки")
    void deleteCompilation_success() {
        Long compId = 1L;

        when(compilationRepository.existsById(compId)).thenReturn(true);

        compilationService.deleteCompilation(compId);

        verify(compilationRepository).existsById(compId);
        verify(compilationRepository).deleteById(compId);
    }

    @Test
    @DisplayName("Удаление подборки - подборка не найдена")
    void deleteCompilation_notFound() {
        Long compId = 99L;

        when(compilationRepository.existsById(compId)).thenReturn(false);

        assertThatThrownBy(() -> compilationService.deleteCompilation(compId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Подборка с ID " + compId + " не найдена");

        verify(compilationRepository).existsById(compId);
        verify(compilationRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Успешное получение подборок (pinned = true)")
    void getCompilations_successPinnedIsTrue() {
        int from = 0;
        int size = 10;
        PageRequest pageRequest = PageRequest.of(from / size, size);

        Compilation compilation1 = new Compilation();
        compilation1.setId(1L);
        compilation1.setTitle("Comp1");
        compilation1.setPinned(true);
        compilation1.setEvents(Collections.emptySet());

        CompilationDto compilationDto1 = new CompilationDto();
        compilationDto1.setId(1L);
        compilationDto1.setTitle("Comp1");
        compilationDto1.setPinned(true);
        compilationDto1.setEvents(Collections.emptyList());

        when(compilationRepository.findByPinned(true, pageRequest))
                .thenReturn(new PageImpl<>(List.of(compilation1)));
        when(compilationMapper.toDto(compilation1)).thenReturn(compilationDto1);
        when(eventService.addViewsAndConfirmedRequestsToShortEvents(anyList())).thenReturn(Collections.emptyList());

        List<CompilationDto> result = compilationService.getCompilations(true, from, size);

        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(compilationDto1);
        verify(compilationRepository).findByPinned(true, pageRequest);
        verify(compilationRepository, never()).findAll(any(PageRequest.class));
    }

    @Test
    @DisplayName("Успешное получение подборок (пустой список)")
    void getCompilations_successEmptyList() {
        int from = 0;
        int size = 10;
        PageRequest pageRequest = PageRequest.of(from / size, size);

        when(compilationRepository.findAll(pageRequest)).thenReturn(new PageImpl<>(Collections.emptyList()));

        List<CompilationDto> result = compilationService.getCompilations(null, from, size);

        assertThat(result).isEmpty();
        verify(compilationRepository).findAll(pageRequest);
    }

    @Test
    @DisplayName("Успешное получение подборки по ID")
    void getCompilationById_success() {
        Long compId = 1L;
        Compilation compilation = new Compilation();
        compilation.setId(compId);
        compilation.setTitle("Test Comp");
        compilation.setPinned(false);
        compilation.setEvents(Collections.emptySet());

        CompilationDto compilationDto = new CompilationDto();
        compilationDto.setId(compId);
        compilationDto.setTitle("Test Comp");
        compilationDto.setPinned(false);
        compilationDto.setEvents(Collections.emptyList());

        when(compilationRepository.findById(compId)).thenReturn(Optional.of(compilation));
        when(compilationMapper.toDto(compilation)).thenReturn(compilationDto);
        when(eventService.addViewsAndConfirmedRequestsToShortEvents(anyList())).thenReturn(Collections.emptyList());

        CompilationDto result = compilationService.getCompilationById(compId);

        assertThat(result).isEqualTo(compilationDto);
        verify(compilationRepository).findById(compId);
        verify(compilationMapper).toDto(compilation);
        verify(eventService, never()).addViewsAndConfirmedRequestsToShortEvents(anyList());
    }

    @Test
    @DisplayName("Получение подборки по ID - подборка не найдена")
    void getCompilationById_notFound() {
        Long compId = 99L;

        when(compilationRepository.findById(compId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compilationService.getCompilationById(compId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Подборка с ID " + compId + " не найдена");

        verify(compilationRepository).findById(compId);
        verify(compilationMapper, never()).toDto(any(Compilation.class));
    }
}

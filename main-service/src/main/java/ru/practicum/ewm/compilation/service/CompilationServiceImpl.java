package ru.practicum.ewm.compilation.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.client.StatsClient;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.compilation.mapper.CompilationMapper;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.repository.CompilationRepository;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exceptions.NotFoundException;
import ru.practicum.ewm.request.service.ParticipationRequestService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private static final LocalDateTime DEFAULT_START =
            LocalDateTime.of(1970, 1, 1, 0, 0);

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;
    private final ParticipationRequestService requestService;
    private final EntityManager entityManager;

    // --------------------------------------------------------------
    // Создание новой компиляции
    // --------------------------------------------------------------
    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {

        Compilation compilation = compilationMapper.toCompilation(newCompilationDto);

        // pinned по умолчанию false
        if (compilation.getPinned() == null) {
            compilation.setPinned(false);
        }

        List<Event> events = newCompilationDto.getEvents() == null
                ? Collections.emptyList()
                : eventRepository
                .findAllByIdsWithCategoryAndInitiator(newCompilationDto.getEvents(), entityManager);

        compilation.setEvents(events);

        Compilation saved = compilationRepository.save(compilation);


        CompilationDto result = compilationMapper.toCompilationDto(saved);
        result.setEvents(enrichEventsWithStats(saved.getEvents()));

        return result;
    }

    // --------------------------------------------------------------
    // Удаление компиляции
    // --------------------------------------------------------------
    @Override
    @Transactional
    public void deleteCompilation(Long id) {

        if (!compilationRepository.existsById(id)) {
            throw new NotFoundException("Compilation with id=" + id + " was not found");
        }

        compilationRepository.deleteById(id);
    }

    // --------------------------------------------------------------
    // Обновление компиляции
    // --------------------------------------------------------------
    @Override
    @Transactional
    public CompilationDto updateCompilation(Long id, UpdateCompilationRequest dto) {

        Compilation compilation = compilationRepository.findById(id)
                                                       .orElseThrow(() ->
                                                               new NotFoundException(
                                                                       "Compilation with id=" + id + " was not found"));

        if (dto.getTitle() != null) {
            compilation.setTitle(dto.getTitle());
        }

        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }

        if (dto.getEvents() != null) {

            // гарантируем, что коллекция инициализирована
            if (compilation.getEvents() == null) {
                compilation.setEvents(new ArrayList<>());
            } else {
                compilation.getEvents().clear();
            }

            if (!dto.getEvents().isEmpty()) {
                List<Event> events = eventRepository
                        .findAllByIdsWithCategoryAndInitiator(dto.getEvents(), entityManager);
                compilation.getEvents().addAll(events);
            }
        }

        Compilation saved = compilationRepository.save(compilation);


        CompilationDto result = compilationMapper.toCompilationDto(saved);
        result.setEvents(enrichEventsWithStats(saved.getEvents()));

        return result;
    }

    // --------------------------------------------------------------
    // Получение списка компиляций (с фильтром pinned и пагинацией)
    // --------------------------------------------------------------
    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {

        // Загружаем компиляции с событиями + category + initiator через fetch join
        List<Compilation> compilations =
                compilationRepository.findCompilationsWithEvents(pinned, from, size, entityManager);

        // Собираем все события на странице
        List<Event> allEvents = compilations.stream()
                                            .flatMap(comp -> comp.getEvents().stream())
                                            .toList();

        // статистика по событиям
        Map<Long, Long> viewsMap = getViews(allEvents);
        Map<Long, Long> confirmedMap = getConfirmedRequests(allEvents);

        // compilation ->  DTO с обогащёнными событиями
        return compilations.stream()
                           .map(comp -> {
                               CompilationDto dto = compilationMapper.toCompilationDto(comp);
                               List<EventShortDto> enrichedEvents = comp.getEvents().stream()
                                                                        .map(event -> {
                                                                            EventShortDto eventDto = eventMapper.toEventShortDto(event);
                                                                            eventDto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                                                                            eventDto.setConfirmedRequests(confirmedMap.getOrDefault(event.getId(), 0L));
                                                                            return eventDto;
                                                                        })
                                                                        .toList();
                               dto.setEvents(enrichedEvents);
                               return dto;
                           })
                           .toList();
    }

    // --------------------------------------------------------------
    // Получение отдельной компиляции по ID
    // --------------------------------------------------------------
    @Override
    public CompilationDto getCompilationById(Long compId) {

        Compilation compilation = compilationRepository.findByIdWithEvents(compId, entityManager)
                                                       .orElseThrow(() ->
                                                               new NotFoundException(
                                                                       "Compilation with id=" + compId + " was not found"));

        CompilationDto dto = compilationMapper.toCompilationDto(compilation);
        dto.setEvents(enrichEventsWithStats(compilation.getEvents()));

        return dto;
    }

    // --------------------------------------------------------------
    // Приватный метод: обогащение списка событий статистикой
    // --------------------------------------------------------------
    private List<EventShortDto> enrichEventsWithStats(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> viewsMap = getViews(events);  // batch запрос к StatsClient
        Map<Long, Long> confirmedMap = getConfirmedRequests(events); // batch запрос к базе

        return events.stream()
                     .map(event -> {
                         EventShortDto dto = eventMapper.toEventShortDto(event);
                         dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                         dto.setConfirmedRequests(confirmedMap.getOrDefault(event.getId(), 0L));
                         return dto;
                     })
                     .toList();
    }

    // --------------------------------------------------------------
    // Приватный метод: получение просмотров событий из StatsClient
    // --------------------------------------------------------------
    private Map<Long, Long> getViews(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<String> uris = events.stream()
                                  .map(e -> "/events/" + e.getId())
                                  .toList();

        return statsClient.getStats(DEFAULT_START.toString(),
                                  LocalDateTime.now().toString(),
                                  uris,
                                  true)

                          .stream()
                          .collect(Collectors.toMap(
                                  s -> Long.parseLong(s.getUri().split("/")[2]),
                                  s -> s.getHits(),
                                  (existing, replacement) -> existing
                          ));
    }

    // --------------------------------------------------------------
    // Приватный метод: получение количества подтверждённых заявок для списка событий
    // --------------------------------------------------------------
    private Map<Long, Long> getConfirmedRequests(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> eventIds = events.stream()
                                    .map(Event::getId)
                                    .toList();

        return requestService.getConfirmedRequestsCountMap(eventIds);
    }
}


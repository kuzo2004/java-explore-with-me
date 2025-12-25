package ru.practicum.ewm.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.client.StatsClient;
import ru.practicum.ewm.dto.EndpointHitDto;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.params.EventSearchParams;
import ru.practicum.ewm.event.service.EventService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class PublicEventController {

    private final EventService eventService;
    private final StatsClient statsClient;

    @Value("${service.name}")  // <-- берём из application.properties
    private String serviceName;


    // ============================================================
    // GET /events
    // Поиск публичных событий с фильтрацией и пагинацией
    // ============================================================
    @GetMapping
    public List<EventShortDto> searchEvents(
            HttpServletRequest request,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(required = false, defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false, defaultValue = "EVENT_DATE") String sort,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size
    ) {

        EventSearchParams params = EventSearchParams.builder()
                                                    .text(text)
                                                    .categories(categories)
                                                    .paid(paid)
                                                    .rangeStart(rangeStart)
                                                    .rangeEnd(rangeEnd)
                                                    .onlyAvailable(onlyAvailable)
                                                    .sort(sort)
                                                    .build();


        // 1. Получаем список событий
        List<EventShortDto> events = eventService.searchPublicEvents(params, from, size);

        // 2. Отправляем в сервис статистики
        recordHit(request);

        return events;
    }


    // ============================================================
    // GET /events/{id}
    // Получение публичного события по id
    // ============================================================
    @GetMapping("{id}")
    public EventFullDto getEventById(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        // 1. Получаем событие через сервис
        EventFullDto event = eventService.getPublicEventById(id);

        // 2. Отправляем информацию о запросе в Stats
        recordHit(request);

        // 3. Возвращаем DTO клиенту
        return event;
    }


    // ============================================================
    // Отправляет информацию о запросе в сервис статистики.
    // ============================================================
    private void recordHit(HttpServletRequest request) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        EndpointHitDto hitDto = EndpointHitDto.builder()
                                              .app(serviceName) // берём из конфигурации имя сервиса
                                              .uri(request.getRequestURI())
                                              .ip(request.getRemoteAddr())
                                              .timestamp(LocalDateTime.now())
                                              .build();

        statsClient.saveHit(hitDto);
    }
}

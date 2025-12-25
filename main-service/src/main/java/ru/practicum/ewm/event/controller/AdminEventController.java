package ru.practicum.ewm.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.event.dto.params.EventAdminSearchParams;
import ru.practicum.ewm.event.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/events")
public class AdminEventController {

    private final EventService eventService;

    // ============================================================
    // GET /admin/events
    // Поиск событий администратором с фильтрацией.
    // ============================================================
    @GetMapping
    public ResponseEntity<List<EventFullDto>> searchEvents(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<String> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {

        EventAdminSearchParams params = EventAdminSearchParams.builder()
                                                              .users(users)
                                                              .states(states)
                                                              .categories(categories)
                                                              .rangeStart(rangeStart)
                                                              .rangeEnd(rangeEnd)
                                                              .build();

        List<EventFullDto> events = eventService.searchEvents(params, from, size);

        return ResponseEntity.ok(events);
    }

    // ============================================================
    // PATCH /admin/events/{eventId}
    // Обновление события администратором.
    // ============================================================
    @PatchMapping("/{eventId}")
    public EventFullDto updateEventByAdmin(
            @PathVariable Long eventId,
            @RequestBody @Valid UpdateEventAdminRequest request) {

        return eventService.updateEventByAdmin(eventId, request);
    }
}


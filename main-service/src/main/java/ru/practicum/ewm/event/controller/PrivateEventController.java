package ru.practicum.ewm.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventUserRequest;
import ru.practicum.ewm.event.dto.params.EventParams;
import ru.practicum.ewm.event.service.EventService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events")
public class PrivateEventController {

    private final EventService eventService;

    // ============================================================
    // POST /users/{userId}/events
    // Создание нового события пользователем
    // ============================================================
    @PostMapping
    public ResponseEntity<EventFullDto> createEvent(
            @PathVariable Long userId,
            @Valid @RequestBody NewEventDto dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(eventService.createEvent(userId, dto));
    }

    // ============================================================
    // PATCH /users/{userId}/events/{eventId}
    // Редактирование события пользователем
    // ============================================================
    @PatchMapping("/{eventId}")
    public ResponseEntity<EventFullDto> updateEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventUserRequest dto) {

        EventParams params = EventParams.builder()
                                        .userId(userId)
                                        .eventId(eventId)
                                        .dto(dto)
                                        .build();

        return ResponseEntity.ok(eventService.updateEvent(params));
    }

    // ============================================================
    // GET /users/{userId}/events/{eventId}
    // Получение собственного события пользователем
    // ============================================================
    @GetMapping("/{eventId}")
    public ResponseEntity<EventFullDto> getUserEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId) {

        return ResponseEntity.ok(eventService.getUserEvent(userId, eventId));
    }

    // ============================================================
    // GET /users/{userId}/events
    // Получение собственного списка событий пользователя с пагинацией
    // ============================================================
    @GetMapping
    public ResponseEntity<List<EventShortDto>> getUserEvents(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(
                from / size,
                size,
                Sort.by(Sort.Direction.ASC, "id")
        );

        return ResponseEntity.ok(eventService.getUserEvents(userId, pageable));
    }
}
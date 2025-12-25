package ru.practicum.ewm.request.controller;

import lombok.RequiredArgsConstructor;
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
import ru.practicum.ewm.event.dto.params.EventRequestUpdateParams;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.service.ParticipationRequestService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
public class PrivateParticipationRequestController {

    private final ParticipationRequestService requestService;


    // ============================================================
    // POST /users/{userId}/requests?eventId={eventId}
    // Создание новой заявки на участие пользователя userId в событии eventId.
    // ============================================================
    @PostMapping("/requests")
    public ResponseEntity<ParticipationRequestDto> createRequest(
            @PathVariable Long userId,
            @RequestParam Long eventId) {

        ParticipationRequestDto dto = requestService.createRequest(userId, eventId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    // ============================================================
    // GET /users/{userId}/requests
    // Получение всех заявок пользователя userId на участие в любых событиях.
    // в ТЗ нет пагинации
    // ============================================================
    @GetMapping("/requests")
    public ResponseEntity<List<ParticipationRequestDto>> getUserRequests(
            @PathVariable Long userId) {

        List<ParticipationRequestDto> requests = requestService.getUserRequests(userId);
        return ResponseEntity.ok(requests);
    }


    // ============================================================
    // PATCH /users/{userId}/requests/{requestId}/cancel
    // Отмена заявки на участие с requestId пользователем userId.
    // Возвращает обновлённый ParticipationRequestDto с новым статусом CANCELED.
    // ============================================================
    @PatchMapping("/requests/{requestId}/cancel")
    public ResponseEntity<ParticipationRequestDto> cancelRequest(
            @PathVariable Long userId,
            @PathVariable Long requestId) {

        ParticipationRequestDto dto = requestService.cancelRequest(userId, requestId);
        return ResponseEntity.ok(dto);
    }

    // ============================================================
    // GET /users/{userId}/events/{eventId}/requests
    // Получение всех заявок на участие в событии, которым владеет userId
    // в ТЗ нет пагинации
    // ============================================================
    @GetMapping("/events/{eventId}/requests")
    public ResponseEntity<List<ParticipationRequestDto>> getEventRequests(
            @PathVariable Long userId,
            @PathVariable Long eventId) {

        List<ParticipationRequestDto> requests = requestService.getRequestsForEvent(userId, eventId);
        return ResponseEntity.ok(requests);
    }

    // ============================================================
    // PATCH /users/{userId}/events/{eventId}/requests
    // Массовое обновление статусов заявок (CONFIRMED или REJECTED)
    // ============================================================
    @PatchMapping("/events/{eventId}/requests")
    public ResponseEntity<EventRequestStatusUpdateResult> updateEventRequestsStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody EventRequestStatusUpdateRequest updateRequest) {

        EventRequestUpdateParams params = EventRequestUpdateParams.builder()
                                                                  .userId(userId)
                                                                  .eventId(eventId)
                                                                  .updateRequest(updateRequest)
                                                                  .build();

        EventRequestStatusUpdateResult result =
                requestService.updateRequestsStatus(params);

        return ResponseEntity.ok(result);
    }
}

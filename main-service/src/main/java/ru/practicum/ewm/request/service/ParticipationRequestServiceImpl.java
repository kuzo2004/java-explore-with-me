package ru.practicum.ewm.request.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.dto.params.EventRequestUpdateParams;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exceptions.BadRequestException;
import ru.practicum.ewm.exceptions.ConflictException;
import ru.practicum.ewm.exceptions.NotFoundException;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.mapper.ParticipationRequestMapper;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestMapper mapper;
    private final EntityManager entityManager;


    // ============================================================
    // Создание заявки на участие в событии
    // ============================================================
    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {

        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> new NotFoundException("User not found"));

        Event event = eventRepository.findById(eventId)
                                     .orElseThrow(() -> new NotFoundException("Event not found"));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Event initiator cannot request participation");
        }

        if (!EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException("Cannot participate in unpublished event");
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Request already exists");
        }

        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        // participantLimit == 0 означает "без ограничений"
        if (event.getParticipantLimit() != null && event.getParticipantLimit() > 0
                && confirmedCount >= event.getParticipantLimit()) {
            throw new ConflictException("Participant limit reached");
        }

        ParticipationRequest request =
                ParticipationRequest.builder()
                                    .event(event)
                                    .requester(user)
                                    .created(LocalDateTime.now())
                                    .status(
                                            // Если пре-модерация выключена или лимит = 0 → подтверждаем автоматически
                                            (!event.getRequestModeration() ||
                                                    event.getParticipantLimit() == 0)
                                                    ? RequestStatus.CONFIRMED
                                                    : RequestStatus.PENDING)
                                    .build();

        ParticipationRequest saved = requestRepository.save(request);

        return mapper.toDto(saved);
    }


    // ============================================================
    // Получение всех заявок пользователя
    // ============================================================
    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        userRepository.findById(userId)
                      .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));

        return requestRepository.findAllByRequesterId(userId)
                                .stream()
                                .map(mapper::toDto)
                                .toList();
    }


    // ============================================================
    // Отмена заявки пользователем
    // ============================================================
    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                                                        .orElseThrow(() ->
                                                                new NotFoundException("Request with id=" + requestId + " not found for user id=" + userId));

        if (request.getStatus() == RequestStatus.CANCELED) {
            throw new ConflictException("Request is already canceled");
        }

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest saved = requestRepository.save(request);
        return mapper.toDto(saved);
    }


    // ============================================================
    // Получение всех заявок на событие (для инициатора)
    // ============================================================
    @Override
    public List<ParticipationRequestDto> getRequestsForEvent(Long userId, Long eventId) {

        userRepository.findById(userId)
                      .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));

        Event event = eventRepository.findById(eventId)
                                     .orElseThrow(() ->
                                             new NotFoundException("Event with id=" + eventId + " not found"));

        // Проверяем, что пользователь является инициатором события
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("User is not the initiator of this event");
        }

        return requestRepository.findAllByEventId(eventId)
                                .stream()
                                // Mapper использует только event.id и requester.id.
                                // Обращение к другим полям event/requester может вызвать N+1 при большом числе заявок.
                                .map(mapper::toDto)
                                .toList();
    }


    // ============================================================
    // Массовое обновление статусов заявок на событие
    // ============================================================
    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestsStatus(EventRequestUpdateParams params) {

        Long userId = params.getUserId();
        Long eventId = params.getEventId();
        EventRequestStatusUpdateRequest updateRequest = params.getUpdateRequest();

        RequestStatus status;
        try {
            status = RequestStatus.valueOf(updateRequest.getStatus());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException("Invalid status: " + updateRequest.getStatus());
        }

        userRepository.findById(userId)
                      .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));

        Event event = eventRepository.findById(eventId)
                                     .orElseThrow(() ->
                                             new NotFoundException("Event with id=" + eventId + " not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("User is not the initiator of the event");
        }


        // Получаем и валидируем заявки
        List<Long> requestIds = updateRequest.getRequestIds();

        List<ParticipationRequest> requests =
                requestRepository.findAllByIdInAndEventId(requestIds, eventId);

        if (requests.size() != requestIds.size()) {
            throw new ConflictException("Some participation requests were not found");
        }

        // Менять можно только заявки в статусе PENDING
        if (requests.stream().anyMatch(r -> r.getStatus() != RequestStatus.PENDING)) {
            throw new ConflictException("Only PENDING requests can be updated");
        }

        // Если нужно ОТКЛОНИТЬ заявки — лимит не проверяем
        if (status == RequestStatus.REJECTED) {

            requests.forEach(r -> r.setStatus(RequestStatus.REJECTED));
            requestRepository.saveAll(requests);

            return EventRequestStatusUpdateResult.builder()
                                                 .confirmedRequests(List.of())
                                                 .rejectedRequests(
                                                         requests.stream()
                                                                 .map(mapper::toDto)
                                                                 .toList()
                                                 )
                                                 .build();
        }

        // Подсчёт уже подтверждённых заявок
        long confirmedCount =
                requestRepository.countByEventIdAndStatus(
                        eventId, RequestStatus.CONFIRMED);

        // приводим null → 0
        long limit = event.getParticipantLimit() == null
                ? 0
                : event.getParticipantLimit();

        // если лимит достигнут — подтверждать нельзя
        if (limit > 0 && confirmedCount >= limit) {
            throw new ConflictException("Participant limit reached");
        }

        // Если модерация выключена или лимита нет
        if (limit == 0 || !event.getRequestModeration()) {

            List<ParticipationRequest> pendingRequests =
                    requestRepository.findAllByIdInAndStatus(requestIds,
                            RequestStatus.PENDING);

            pendingRequests.forEach(r -> r.setStatus(RequestStatus.CONFIRMED));
            requestRepository.saveAll(pendingRequests);

            return EventRequestStatusUpdateResult.builder()
                                                 .confirmedRequests(
                                                         pendingRequests.stream()
                                                                        .map(mapper::toDto)
                                                                        .toList()
                                                 )
                                                 .rejectedRequests(List.of())
                                                 .build();
        }

        // Модерация включена и лимит есть
        long available = limit - confirmedCount;

        if (available <= 0) {
            throw new ConflictException("Participant limit reached");
        }

        // Получаем PENDING заявки по дате создания
        List<ParticipationRequest> pendingRequests =
                requestRepository.findAllByIdInAndStatusOrderByCreatedAsc(requestIds,
                        RequestStatus.PENDING);

        // Делим заявки в памяти
        List<ParticipationRequest> toConfirm =
                pendingRequests.stream()
                               .limit(available)
                               .toList();

        List<ParticipationRequest> toReject =
                pendingRequests.stream()
                               .skip(available)
                               .toList();

        // Обновляем статусы
        toConfirm.forEach(r -> r.setStatus(RequestStatus.CONFIRMED));
        toReject.forEach(r -> r.setStatus(RequestStatus.REJECTED));

        requestRepository.saveAll(toConfirm);
        requestRepository.saveAll(toReject);

        return EventRequestStatusUpdateResult.builder()
                                             .confirmedRequests(
                                                     toConfirm.stream()
                                                              .map(mapper::toDto)
                                                              .toList()
                                             )
                                             .rejectedRequests(
                                                     toReject.stream()
                                                             .map(mapper::toDto)
                                                             .toList()
                                             )
                                             .build();

    }


    // ============================================================
    // public API: Получение количества подтверждённых заявок для события
    // ============================================================
    @Override
    public long getConfirmedRequestsCount(Long eventId) {
        return requestRepository.countByEventIdAndStatus(
                eventId, RequestStatus.CONFIRMED
        );
    }

    // ============================================================
    // public API: Получение мапы подтверждённых заявок по списку событий
    // ============================================================
    @Override
    public Map<Long, Long> getConfirmedRequestsCountMap(List<Long> eventIds) {

        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        return requestRepository.getConfirmedRequestsCountMap(eventIds, entityManager);
    }

    // ============================================================
    // admin API: Получение количества заявок для события
    // ============================================================
    @Override
    public long getAllRequestsCount(Long eventId) {
        return requestRepository.countByEventId(eventId);
    }

    // ============================================================
    // admin API: Получение мапы  заявок по списку событий
    // ============================================================
    @Override
    public Map<Long, Long> getAllRequestsCountMap(List<Long> eventIds) {

        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        return requestRepository.getAllRequestsCountMap(eventIds, entityManager);
    }
}
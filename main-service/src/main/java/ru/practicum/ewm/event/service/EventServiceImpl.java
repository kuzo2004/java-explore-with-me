package ru.practicum.ewm.event.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.client.StatsClient;
import ru.practicum.ewm.comment.service.CommentService;
import ru.practicum.ewm.dto.ViewStats;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.event.dto.UpdateEventUserRequest;
import ru.practicum.ewm.event.dto.params.EventAdminSearchParams;
import ru.practicum.ewm.event.dto.params.EventParams;
import ru.practicum.ewm.event.dto.params.EventSearchParams;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventSort;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exceptions.BadRequestException;
import ru.practicum.ewm.exceptions.ConflictException;
import ru.practicum.ewm.exceptions.NotFoundException;
import ru.practicum.ewm.exceptions.ValidationException;
import ru.practicum.ewm.request.service.ParticipationRequestService;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * EventServiceImpl
 * <p>
 * ВАЖНО:
 * Поля views и confirmedRequests не являются частью сущности Event
 * и не хранятся в таблице events.
 * <p>
 * - views получаются из Stats-сервиса
 * - confirmedRequests вычисляются через ParticipationRequest
 * <p>
 * Эти значения агрегируют на уровне сервиса
 * и добавляются только в DTO.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private static final LocalDateTime DEFAULT_START =
            LocalDateTime.of(1970, 1, 1, 0, 0);

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;
    private final EntityManager entityManager;
    private final ParticipationRequestService requestService;
    private final CommentService commentService;


    // ============================================================
    // Создание события
    // ============================================================
    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {

        User user = userRepository.findById(userId)
                                  .orElseThrow(() ->
                                          new NotFoundException("User with id=" + userId + " was not found"));

        Category category = categoryRepository.findById(dto.getCategory())
                                              .orElseThrow(() ->
                                                      new NotFoundException("Category with id=" + dto.getCategory()
                                                              + " was not found"));

        LocalDateTime eventDate = dto.getEventDate();
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Event date must be at least 2 hours in the future");
        }

        if (dto.getParticipantLimit() != null && dto.getParticipantLimit() < 0) {
            throw new IllegalArgumentException("Participant limit cannot be negative");
        }


        // Создаем сущность и заполняем поля
        Event event = eventMapper.toEvent(dto);
        event.setCategory(category);
        event.setInitiator(user);
        event.setCreatedOn(LocalDateTime.now());
        event.setEventDate(eventDate);
        event.setState(EventState.PENDING);

        if (event.getPaid() == null) event.setPaid(false);
        if (event.getParticipantLimit() == null) event.setParticipantLimit(0);
        if (event.getRequestModeration() == null) event.setRequestModeration(true);

        Event saved = eventRepository.save(event);

        EventFullDto eventFullDto = eventMapper.toEventFullDto(saved);
        eventFullDto.setViews(0L);
        eventFullDto.setConfirmedRequests(0L);
        eventFullDto.setComments(0L);

        return eventFullDto;
    }

    // ============================================================
    // Редактирование своего события пользователем
    // ============================================================
    @Override
    @Transactional
    public EventFullDto updateEvent(EventParams params) {

        Long userId = params.getUserId();
        Long eventId = params.getEventId();
        UpdateEventUserRequest dto = params.getDto();

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                                     .orElseThrow(() -> new NotFoundException(
                                             "Event with id=" + eventId + " for user id=" + userId + " not found"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Cannot edit a published event");
        }
        if (!(event.getState() == EventState.PENDING || event.getState() == EventState.CANCELED)) {
            throw new ConflictException("Event can only be edited in PENDING or CANCELED state");
        }

        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Event date must be at least 2 hours in the future"); // в postman код 400
        }

        if (dto.getParticipantLimit() != null && dto.getParticipantLimit() < 0) {
            throw new BadRequestException("Participant limit cannot be negative"); // в postman код 400
        }

        eventMapper.updateEventFromUserRequest(dto, event);

        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                                                  .orElseThrow(() -> new NotFoundException(
                                                          "Category with id=" + dto.getCategory() + " not found"));
            event.setCategory(category);
        }

        if (dto.getStateAction() != null) {

            switch (dto.getStateAction()) {

                case SEND_TO_REVIEW -> {
                    // отправлять можно только из CANCELED или PENDING
                    if (event.getState() != EventState.CANCELED &&
                            event.getState() != EventState.PENDING) {
                        throw new ConflictException("Only CANCELED or PENDING events can be sent to review");
                    }
                    event.setState(EventState.PENDING);
                }

                case CANCEL_REVIEW -> {
                    // отменять можно только из PENDING
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Only PENDING events can be canceled");
                    }
                    event.setState(EventState.CANCELED);
                }
            }
        }

        Event saved = eventRepository.save(event);

        // Обогащение DTO
        LocalDateTime start = DEFAULT_START;
        LocalDateTime end = LocalDateTime.now();

        Map<Long, Long> viewsMap = getViews(List.of(saved), start, end);
        long confirmedRequests = requestService.getConfirmedRequestsCount(saved.getId());
        long comments = commentService.countPublishedByEventId(saved.getId());

        EventFullDto result = eventMapper.toEventFullDto(saved);
        result.setViews(viewsMap.getOrDefault(saved.getId(), 0L));
        result.setConfirmedRequests(confirmedRequests);
        result.setComments(comments);

        return result;
    }

    // ============================================================
    // Получение события пользователя
    // ============================================================
    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                                     .orElseThrow(() ->
                                             new NotFoundException("Event with id=" + eventId
                                                     + " was not found for user id=" + userId));


        LocalDateTime start = DEFAULT_START;
        LocalDateTime end = LocalDateTime.now();

        Map<Long, Long> viewsMap = getViews(List.of(event), start, end);
        long confirmedRequests = requestService.getConfirmedRequestsCount(event.getId());
        long comments = commentService.countPublishedByEventId(event.getId());

        EventFullDto dto = eventMapper.toEventFullDto(event);
        dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
        dto.setConfirmedRequests(confirmedRequests);
        dto.setComments(comments);

        return dto;
    }

    // ============================================================
    // Получение списка событий пользователя (короткая DTO)
    // ============================================================
    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {

        List<Event> events = eventRepository.findAllByInitiatorId(userId, from, size, entityManager);

        if (events.isEmpty()) {
            return List.of();
        }

        // агрегация данных по связанным событиям (просмотры, подтвержденные заявки)
        LocalDateTime start = DEFAULT_START;
        LocalDateTime end = LocalDateTime.now();

        Map<Long, Long> viewsMap = getViews(events, start, end);
        Map<Long, Long> confirmedMap = getConfirmedRequests(events);
        Map<Long, Long> commentsMap = getCommentsCount(events);

        return events.stream()
                     .map(event -> {
                         EventShortDto dto = eventMapper.toEventShortDto(event);
                         dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                         dto.setConfirmedRequests(confirmedMap.getOrDefault(event.getId(), 0L));
                         dto.setComments(commentsMap.getOrDefault(event.getId(), 0L));
                         return dto;
                     })
                     .toList();
    }


    // ============================================================
    // Поиск событий админом
    // ============================================================
    @Override
    public List<EventFullDto> searchEvents(EventAdminSearchParams params, int from, int size) {

        // Получаем события с fetch join и фильтрами
        List<Event> events = eventRepository.searchEventsWithPagination(params, from, size, entityManager);

        if (events.isEmpty()) {
            return List.of();
        }

        // даты для StatsClient
        LocalDateTime start = params.getRangeStart() != null
                ? params.getRangeStart()
                : DEFAULT_START;
        LocalDateTime end = params.getRangeEnd() != null
                ? params.getRangeEnd()
                : LocalDateTime.now();

        // Просмотры, подтверждённые заявки, публичные комментарии
        Map<Long, Long> viewsMap = getViews(events, start, end);
        Map<Long, Long> confirmedMap = getConfirmedRequests(events);
        Map<Long, Long> commentsMap = getCommentsCount(events);

        return events.stream()
                     .map(e -> {
                         EventFullDto dto = eventMapper.toEventFullDto(e);
                         dto.setViews(viewsMap.getOrDefault(e.getId(), 0L));
                         dto.setConfirmedRequests(confirmedMap.getOrDefault(e.getId(), 0L));
                         dto.setComments(commentsMap.getOrDefault(e.getId(), 0L));
                         return dto;
                     })
                     .toList();
    }


    // ============================================================
    // Поиск публичных событий
    // ============================================================
    @Override
    public List<EventShortDto> searchPublicEvents(EventSearchParams params, int from, int size) {

        if (params.getRangeStart() != null && params.getRangeEnd() != null &&
                params.getRangeStart().isAfter(params.getRangeEnd())) {
            throw new BadRequestException("rangeStart must be before rangeEnd");
        }

        // Получаем события с fetch join и фильтрами
        List<Event> events =
                eventRepository.searchPublicEvents(params, entityManager);

        if (events.isEmpty()) {
            return List.of();
        }

        // даты для StatsClient
        LocalDateTime start = params.getRangeStart() != null
                ? params.getRangeStart()
                : DEFAULT_START;
        LocalDateTime end = params.getRangeEnd() != null
                ? params.getRangeEnd()
                : LocalDateTime.now();


        // Batch-агрегация просмотров, заявок, публичных комментариев
        Map<Long, Long> viewsMap = getViews(events, start, end);
        Map<Long, Long> confirmedMap = getConfirmedRequests(events);
        Map<Long, Long> commentsMap = getCommentsCount(events);

        // 4. onlyAvailable — В ПАМЯТИ (события с доступным лимитом)
        if (Boolean.TRUE.equals(params.getOnlyAvailable())) {
            events = events.stream()
                           .filter(event -> {
                               int limit = event.getParticipantLimit();
                               if (limit == 0) {
                                   return true;
                               }
                               long confirmed = confirmedMap.getOrDefault(event.getId(), 0L);
                               return confirmed < limit;
                           })
                           .toList();
        }

        // 5. Сортировка — В ПАМЯТИ
        EventSort sort = EventSort.from(params.getSort());

        if (sort == EventSort.VIEWS) {
            events.sort(
                    Comparator.comparing(
                            e -> viewsMap.getOrDefault(e.getId(), 0L), //возвращает кол-во просмотров
                            Comparator.reverseOrder() // сортировать по убыванию
                    )
            );
        } else {
            // EVENT_DATE — сортировка уже есть в репозитории,
            // но дублируем для гарантии после фильтрации
            events.sort(Comparator.comparing(Event::getEventDate));
        }

        // 6. Пагинация
        // для корректного результата пагинация применяется
        // после агрегации данных на уровне сервиса.
        int fromIndex = Math.min(from, events.size());
        int toIndex = Math.min(from + size, events.size());

        List<Event> page = events.subList(fromIndex, toIndex);


        return page.stream()
                   .map(e -> {
                       EventShortDto dto = eventMapper.toEventShortDto(e);
                       dto.setViews(viewsMap.getOrDefault(e.getId(), 0L));
                       dto.setConfirmedRequests(confirmedMap.getOrDefault(e.getId(), 0L));
                       dto.setComments(commentsMap.getOrDefault(e.getId(), 0L));
                       return dto;
                   })
                   .toList();
    }


    // ============================================================
    // Получение публичного события по id
    // ============================================================
    @Override
    public EventFullDto getPublicEventById(Long eventId) {
        Event event = eventRepository.findPublicEventById(eventId, entityManager);

        // диапазон для StatsClient
        LocalDateTime start = DEFAULT_START;           //  с начала эпохи
        LocalDateTime end = LocalDateTime.now();       // до текущего времени

        // просмотры, заявки(подтвержденные), публичные комментарии
        Map<Long, Long> viewsMap = getViews(List.of(event), start, end);
        long confirmedRequests = requestService.getConfirmedRequestsCount(event.getId());
        long comments = commentService.countPublishedByEventId(event.getId());

        EventFullDto dto = eventMapper.toEventFullDto(event);
        dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
        dto.setConfirmedRequests(confirmedRequests);
        dto.setComments(comments);

        return dto;
    }


    // ============================================================
    // Редактирование события админом
    // ============================================================
    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest dto) {

        Event event = eventRepository.findById(eventId)
                                     .orElseThrow(() -> new NotFoundException(
                                             "Event with id=" + eventId + " not found"));

        // Проверка изменения даты события
        if (dto.getEventDate() != null) {
            LocalDateTime newDate = dto.getEventDate();

            if (event.getPublishedOn() != null) {
                // событие уже опубликовано — проверка +1 час
                LocalDateTime earliestAllowedDate = event.getPublishedOn().plusHours(1);
                if (newDate.isBefore(earliestAllowedDate)) {
                    throw new ConflictException(
                            "Event date must be at least 1 hour after publication");
                }
            } else {
                // событие ещё не опубликовано — проверка, что дата не в прошлом
                if (newDate.isBefore(LocalDateTime.now().plusHours(1))) {
                    throw new BadRequestException(
                            "Event date must be at least 1 hour from now"); // postman требует код 400
                }
            }
        }

        if (dto.getParticipantLimit() != null && dto.getParticipantLimit() < 0) {
            throw new IllegalArgumentException("Participant limit cannot be negative");
        }

        if (dto.getStateAction() != null) {

            switch (dto.getStateAction()) {

                case PUBLISH_EVENT -> {
                    // публиковать можно только PENDING
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException(
                                "Only events in PENDING state can be published");
                    }

                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                }

                case REJECT_EVENT -> {
                    // нельзя отклонить опубликованное
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException(
                                "Cannot reject a published event");
                    }

                    event.setState(EventState.CANCELED);
                }
            }
        }

        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                                                  .orElseThrow(() -> new NotFoundException(
                                                          "Category with id=" + dto.getCategory() + " not found"));
            event.setCategory(category);
        }

        eventMapper.updateEventFromAdminRequest(dto, event);

        Event saved = eventRepository.save(event);

        LocalDateTime start = DEFAULT_START;
        LocalDateTime end = LocalDateTime.now();

        Map<Long, Long> viewsMap = getViews(List.of(saved), start, end);
        long confirmedRequests = requestService.getConfirmedRequestsCount(saved.getId());
        long comments = commentService.countPublishedByEventId(saved.getId());

        EventFullDto eventFullDto = eventMapper.toEventFullDto(saved);
        eventFullDto.setViews(viewsMap.getOrDefault(saved.getId(), 0L));
        eventFullDto.setConfirmedRequests(confirmedRequests);
        eventFullDto.setComments(comments);

        return eventFullDto;
    }

    // --------------------------------------------------------------
    // Приватный метод: получение просмотров событий из StatsClient
    // views не являются полем Event и не хранятся в БД
    // --------------------------------------------------------------
    private Map<Long, Long> getViews(List<Event> events, LocalDateTime start, LocalDateTime end) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<String> uris = events.stream()
                                  .map(e -> "/events/" + e.getId())
                                  .toList();

        return statsClient.getStats(start.toString(),
                                  end.toString(),
                                  uris,
                                  true)

                          .stream()
                          .collect(Collectors.toMap(
                                  s -> Long.parseLong(s.getUri().split("/")[2]),
                                  ViewStats::getHits,
                                  (existing, replacement) -> existing
                          ));
    }

    // --------------------------------------------------------------
    // получение количества подтверждённых заявок для списка событий
    // confirmedRequests не являются полем Event и вычисляются агрегатно
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

    // --------------------------------------------------------------
    // comments — агрегируются и не хранятся в Event
    // --------------------------------------------------------------
    private Map<Long, Long> getCommentsCount(List<Event> events) {

        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> ids = events.stream()
                               .map(Event::getId)
                               .toList();

        return commentService.countPublishedByEventIds(ids);
    }
}



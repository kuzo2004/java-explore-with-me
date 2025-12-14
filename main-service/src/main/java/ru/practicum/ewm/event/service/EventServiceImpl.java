package ru.practicum.ewm.event.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.client.StatsClient;
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
 * Эти значения агрегируются на уровне сервиса
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


    // ============================================================
    // Создание события
    // ============================================================
    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {

        // 1. Проверки
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


        // 2. Создаем сущность и заполняем поля
        Event event = eventMapper.toEvent(dto);
        event.setCategory(category);
        event.setInitiator(user);
        event.setCreatedOn(LocalDateTime.now());
        event.setEventDate(eventDate);
        event.setState(EventState.PENDING);

        if (event.getPaid() == null) event.setPaid(false);
        if (event.getParticipantLimit() == null) event.setParticipantLimit(0);
        if (event.getRequestModeration() == null) event.setRequestModeration(true);

        // 3. Сохраняем событие
        Event saved = eventRepository.save(event);

        // 4. Маппим и обогащаем DTO
        // views и confirmedRequests не хранятся в БД,
        // для нового события они всегда равны 0
        EventFullDto eventFullDto = eventMapper.toEventFullDto(saved);
        eventFullDto.setViews(0L);
        eventFullDto.setConfirmedRequests(0L);

        return eventFullDto;
    }

    // ============================================================
    // Редактирование события пользователем
    // ============================================================
    @Override
    @Transactional
    public EventFullDto updateEvent(EventParams params) {

        Long userId = params.getUserId();
        Long eventId = params.getEventId();
        UpdateEventUserRequest dto = params.getDto();

        // 1. Найти событие пользователя
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                                     .orElseThrow(() -> new NotFoundException(
                                             "Event with id=" + eventId + " for user id=" + userId + " not found"));

        // 2. Проверка состояния события
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Cannot edit a published event");
        }
        if (!(event.getState() == EventState.PENDING || event.getState() == EventState.CANCELED)) {
            throw new ConflictException("Event can only be edited in PENDING or CANCELED state");
        }

        // 3. Проверка даты
        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Event date must be at least 2 hours in the future");
        }

        if (dto.getParticipantLimit() != null && dto.getParticipantLimit() < 0) {
            throw new IllegalArgumentException("Participant limit cannot be negative");
        }

        // 4. Обновление полей через MapStruct
        eventMapper.updateEventFromUserRequest(dto, event);

        // 5. Обработка category вручную
        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                                                  .orElseThrow(() -> new NotFoundException(
                                                          "Category with id=" + dto.getCategory() + " not found"));
            event.setCategory(category);
        }

        // 6. Обработка stateAction
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

        // 7. Сохраняем изменения
        Event saved = eventRepository.save(event);

        // 8. Обогащение DTO
        // views и confirmedRequests вычисляются агрегатно,
        // не являются полями Event
        LocalDateTime start = DEFAULT_START;
        LocalDateTime end = LocalDateTime.now();

        Map<Long, Long> viewsMap = getViews(List.of(saved), start, end);
        long confirmedRequests = requestService.getConfirmedRequestsCount(saved.getId());
        EventFullDto result = eventMapper.toEventFullDto(saved);
        result.setViews(viewsMap.getOrDefault(saved.getId(), 0L));
        result.setConfirmedRequests(confirmedRequests);

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

        EventFullDto dto = eventMapper.toEventFullDto(event);
        dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
        dto.setConfirmedRequests(confirmedRequests);

        return dto;
    }

    // ============================================================
    // Получение списка событий пользователя (короткая DTO)
    // ============================================================
    @Override
    public List<EventShortDto> getUserEvents(Long userId, Pageable pageable) {

        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable).getContent();

        if (events.isEmpty()) {
            return List.of();
        }

        // Диапазон для stats
        LocalDateTime start = DEFAULT_START;
        LocalDateTime end = LocalDateTime.now();

        // Просмотры и подтверждённые заявки (batch)
        Map<Long, Long> viewsMap = getViews(events, start, end);
        Map<Long, Long> confirmedMap = getConfirmedRequests(events);

        // Преобразование + обогащение DTO
        return events.stream()
                     .map(event -> {
                         EventShortDto dto = eventMapper.toEventShortDto(event);
                         dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                         dto.setConfirmedRequests(confirmedMap.getOrDefault(event.getId(), 0L));
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

        // задаём диапазон дат для StatsClient
        LocalDateTime start = params.getRangeStart() != null
                ? params.getRangeStart()
                : DEFAULT_START;
        LocalDateTime end = params.getRangeEnd() != null
                ? params.getRangeEnd()
                : LocalDateTime.now();

        // Просмотры и подтверждённые заявки (batch)
        Map<Long, Long> viewsMap = getViews(events, start, end);
        Map<Long, Long> confirmedMap = getConfirmedRequests(events);

        // Преобразуем в DTO и добавляем просмотры
        return events.stream()
                     .map(e -> {
                         EventFullDto dto = eventMapper.toEventFullDto(e);
                         dto.setViews(viewsMap.getOrDefault(e.getId(), 0L));
                         dto.setConfirmedRequests(confirmedMap.getOrDefault(e.getId(), 0L));
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

        // 1. Получаем события с fetch join и фильтрами
        List<Event> events =
                eventRepository.searchPublicEvents(params, entityManager);

        if (events.isEmpty()) {
            return List.of();
        }

        // 2.Диапазон дат для StatsClient
        LocalDateTime start = params.getRangeStart() != null
                ? params.getRangeStart()
                : DEFAULT_START;
        LocalDateTime end = params.getRangeEnd() != null
                ? params.getRangeEnd()
                : LocalDateTime.now();


        // 3. Batch-агрегация
        Map<Long, Long> viewsMap = getViews(events, start, end);
        Map<Long, Long> confirmedMap = getConfirmedRequests(events);

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


        //  7. Преобразование + обогащение DTO
        return page.stream()
                   .map(e -> {
                       EventShortDto dto = eventMapper.toEventShortDto(e);
                       dto.setViews(viewsMap.getOrDefault(e.getId(), 0L));
                       dto.setConfirmedRequests(confirmedMap.getOrDefault(e.getId(), 0L));
                       return dto;
                   })
                   .toList();
    }


    // ============================================================
    // Получение публичного события по id
    // ============================================================
    @Override
    public EventFullDto getPublicEventById(Long eventId) {
        // 1. Получаем событие через репозиторий с fetch join
        Event event = eventRepository.findPublicEventById(eventId, entityManager);

        // 2. Подготавливаем диапазон для StatsClient
        LocalDateTime start = DEFAULT_START;           //  с начала эпохи
        LocalDateTime end = LocalDateTime.now();       // до текущего времени

        // 3. Получаем просмотры
        Map<Long, Long> viewsMap = getViews(List.of(event), start, end);

        // 4. Получаем confirmedRequests (для одного события)
        long confirmedRequests = requestService.getConfirmedRequestsCount(event.getId());

        // 5. Преобразуем в DTO и обогащаем
        EventFullDto dto = eventMapper.toEventFullDto(event);
        dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
        dto.setConfirmedRequests(confirmedRequests);

        return dto;
    }


    // ============================================================
    // Редактирование события админом
    // ============================================================
    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest dto) {

        // 1. Получаем событие по id (админ может редактировать любое)
        Event event = eventRepository.findById(eventId)
                                     .orElseThrow(() -> new NotFoundException(
                                             "Event with id=" + eventId + " not found"));

        // 2. Проверка изменения даты события
        if (dto.getEventDate() != null) {
            LocalDateTime newDate = dto.getEventDate();

            if (event.getPublishedOn() != null) {
                // событие уже опубликовано — проверка +1 час
                LocalDateTime earliestAllowedDate = event.getPublishedOn().plusHours(1);
                if (newDate.isBefore(earliestAllowedDate)) {
                    throw new BadRequestException(
                            "Event date must be at least 1 hour after publication");
                }
            } else {
                // событие ещё не опубликовано — проверка, что дата не в прошлом
                if (newDate.isBefore(LocalDateTime.now().plusHours(1))) {
                    throw new BadRequestException(
                            "Event date must be at least 1 hour from now");
                }
            }
        }

        if (dto.getParticipantLimit() != null && dto.getParticipantLimit() < 0) {
            throw new IllegalArgumentException("Participant limit cannot be negative");
        }

        // 3. Обработка stateAction
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

        // 4. Обработка Category
        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                                                  .orElseThrow(() -> new NotFoundException(
                                                          "Category with id=" + dto.getCategory() + " not found"));
            event.setCategory(category);
        }

        // 5. Обновление простых полей через MapStruct
        eventMapper.updateEventFromAdminRequest(dto, event);

        // 6. Сохранение
        Event saved = eventRepository.save(event);

        // 7. Обогащение DTO
        LocalDateTime start = DEFAULT_START;
        LocalDateTime end = LocalDateTime.now();

        Map<Long, Long> viewsMap = getViews(List.of(saved), start, end);
        long confirmedRequests = requestService.getConfirmedRequestsCount(saved.getId());

        EventFullDto eventFullDto = eventMapper.toEventFullDto(saved);
        eventFullDto.setViews(viewsMap.getOrDefault(saved.getId(), 0L));
        eventFullDto.setConfirmedRequests(confirmedRequests);

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
                                  s -> s.getHits(),
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
}



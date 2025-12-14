package ru.practicum.ewm.event.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.category.model.QCategory;
import ru.practicum.ewm.event.dto.params.EventAdminSearchParams;
import ru.practicum.ewm.event.dto.params.EventSearchParams;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.QEvent;
import ru.practicum.ewm.exceptions.NotFoundException;
import ru.practicum.ewm.user.model.QUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * NOTE:
 * Поля views и confirmedRequests отсутствуют в сущности Event
 * и таблице events, поэтому:
 * - сортировка
 * - фильтрация
 * по этим значениям на уровне БД невозможна.
 * <p>
 * Данные агрегируются на уровне сервиса.
 * При изменении модели — данный код требует пересмотра.
 */

@Repository
public interface EventRepository extends JpaRepository<Event, Long>,
        QuerydslPredicateExecutor<Event> {

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    Page<Event> findAllByInitiatorId(Long userId, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);


    // для GET/admin/events
    default List<Event> searchEventsWithPagination(
            EventAdminSearchParams params,
            int from,
            int size,
            EntityManager entityManager) {

        QEvent event = QEvent.event;

        // Динамические условия
        BooleanExpression[] predicates = new BooleanExpression[]{
                (params.getUsers() != null && !params.getUsers().isEmpty())
                        ? event.initiator.id.in(params.getUsers())
                        : null,
                (params.getStates() != null && !params.getStates().isEmpty())
                        ? event.state.in(params.getStates().stream()
                                               .map(EventState::valueOf).toList())
                        : null,
                (params.getCategories() != null && !params.getCategories().isEmpty())
                        ? event.category.id.in(params.getCategories())
                        : null,
                params.getRangeStart() != null
                        ? event.eventDate.goe(params.getRangeStart())  // Greater or equal (>=)
                        : null,
                params.getRangeEnd() != null
                        ? event.eventDate.loe(params.getRangeEnd())  // Less or equal (<=)
                        : null
        };

        // ===== ШАГ 1. Получаем ID =====
        List<Long> eventIds = new JPAQuery<Long>(entityManager)
                .select(event.id)
                .from(event)
                .where(Expressions.allOf(predicates))
                .orderBy(event.id.asc())
                .offset(from)
                .limit(size)
                .fetch();

        if (eventIds.isEmpty()) {
            return List.of();
        }

        // ===== ШАГ 2. Загружаем сущности =====
        QCategory category = QCategory.category;
        QUser initiator = QUser.user;

        return new JPAQuery<Event>(entityManager)
                .select(event)
                .from(event)
                .leftJoin(event.category, category).fetchJoin()
                .leftJoin(event.initiator, initiator).fetchJoin()
                .where(event.id.in(eventIds))
                .orderBy(event.id.asc())
                .fetch();
    }


    // ============================================================
    // Поиск публичных событий (ТОЛЬКО по полям Event)  ->  GET/events
    // NOTE:
    // Пагинация применяется на уровне сервиса,
    // т.к. фильтрация onlyAvailable и сортировка по views
    // выполняются in-memory.
    // ============================================================
    default List<Event> searchPublicEvents(EventSearchParams params,
                                           EntityManager em) {
        QEvent event = QEvent.event;
        QCategory category = QCategory.category;
        QUser initiator = QUser.user;


        BooleanExpression textPredicate = null;

        // Фильтрация по тексту
        if (params.getText() != null && !params.getText().isBlank()) {
            String text = params.getText().trim();
            textPredicate = event.annotation.containsIgnoreCase(text)
                                            .or(event.description.containsIgnoreCase(text));
        }

        BooleanExpression[] predicates = new BooleanExpression[]{
                event.state.eq(EventState.PUBLISHED), // только опубликованные
                textPredicate,
                params.getCategories() != null && !params.getCategories().isEmpty()
                        ? event.category.id.in(params.getCategories())
                        : null,
                params.getPaid() != null
                        ? event.paid.eq(params.getPaid())
                        : null,
                params.getRangeStart() != null
                        ? event.eventDate.goe(params.getRangeStart())
                        : event.eventDate.goe(LocalDateTime.now()),
                params.getRangeEnd() != null
                        ? event.eventDate.loe(params.getRangeEnd())
                        : null,
        };

        JPAQuery<Event> query = new JPAQuery<>(em);

        return query.select(event)
                    .from(event)
                    .distinct()
                    .leftJoin(event.category, category).fetchJoin()
                    .leftJoin(event.initiator, initiator).fetchJoin()
                    .where(Expressions.allOf(predicates))
                    .fetch();
    }

    // ----------------------------------------------------------
    // Получение одного публичного события по id
    // ----------------------------------------------------------
    default Event findPublicEventById(Long eventId, EntityManager em) {
        QEvent event = QEvent.event;
        QCategory category = QCategory.category;
        QUser initiator = QUser.user;

        Event e = new JPAQuery<Event>(em)
                .select(event)
                .from(event)
                .leftJoin(event.category, category).fetchJoin()
                .leftJoin(event.initiator, initiator).fetchJoin()
                .where(event.id.eq(eventId)
                               .and(event.state.eq(EventState.PUBLISHED)))
                .fetchOne();

        if (e == null) {
            throw new NotFoundException("Event not found or not published: " + eventId);
        }

        return e;
    }


    // ----------------------------------------------------------
    // Получение списка событий по списку ID с подгрузкой category и initiator
    // fetch join, чтобы при маппинге в EventShortDto не возникал N+1
    // ----------------------------------------------------------
    default List<Event> findAllByIdsWithCategoryAndInitiator(List<Long> eventIds, EntityManager em) {
        if (eventIds.isEmpty()) return List.of();

        QEvent e = QEvent.event;
        QCategory c = QCategory.category;
        QUser u = QUser.user;

        return new JPAQuery<Event>(em)
                .select(e)
                .from(e)
                .leftJoin(e.category, c).fetchJoin()
                .leftJoin(e.initiator, u).fetchJoin()
                .where(e.id.in(eventIds))
                .fetch();
    }
}

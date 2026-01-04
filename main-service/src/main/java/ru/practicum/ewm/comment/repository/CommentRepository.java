package ru.practicum.ewm.comment.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.comment.dto.params.CommentSearchParams;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.comment.model.CommentState;
import ru.practicum.ewm.comment.model.QComment;
import ru.practicum.ewm.comment.model.SortOrder;
import ru.practicum.ewm.user.model.QUser;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    default List<Comment> searchComments(Long userId, Long eventId, CommentSearchParams params, EntityManager em) {
        QComment comment = QComment.comment;
        QUser author = QUser.user;

        // ============================
        // Динамическая фильтрация
        // ============================
        BooleanExpression[] predicates = new BooleanExpression[]{
                comment.author.id.eq(userId),
                comment.event.id.eq(eventId),
                params.getState() != null ? comment.commentState.eq(params.getState()) : null
        };

        // ============================
        // Построение запроса
        // ============================
        JPAQuery<Comment> query = new JPAQuery<>(em);

        query.select(comment)
             .from(comment)
             .distinct()
             .leftJoin(comment.author, author).fetchJoin()
             .where(Expressions.allOf(predicates));

        // ============================
        // Сортировка
        // ============================
        if (params.getSort() == null || params.getSort() == SortOrder.ASC) {
            query.orderBy(comment.createdOn.asc());
        } else {
            query.orderBy(comment.createdOn.desc());
        }

        // ============================
        // Пагинация
        // ============================
        int from = params.getFrom() != null ? params.getFrom() : 0;
        int size = params.getSize() != null ? params.getSize() : 10;
        query.offset(from).limit(size);

        return query.fetch();
    }

    default List<Comment> searchUserComments(Long userId, CommentSearchParams params, EntityManager em) {

        QComment comment = QComment.comment;
        QUser author = QUser.user;

        BooleanExpression[] predicates = new BooleanExpression[]{
                comment.author.id.eq(userId),
                params.getState() != null ? comment.commentState.eq(params.getState()) : null
        };

        JPAQuery<Comment> query = new JPAQuery<>(em);

        query.select(comment)
             .from(comment)
             .distinct()
             .leftJoin(comment.author, author).fetchJoin()
             .where(Expressions.allOf(predicates));

        // ===== сортировка =====
        if (params.getSort() == null || params.getSort() == SortOrder.ASC) {
            query.orderBy(comment.createdOn.asc());
        } else {
            query.orderBy(comment.createdOn.desc());
        }

        // ===== пагинация =====
        int from = params.getFrom() != null ? params.getFrom() : 0;
        int size = params.getSize() != null ? params.getSize() : 10;

        query.offset(from).limit(size);

        return query.fetch();
    }

    default List<Comment> searchCommentsAdmin(Long userId, Long eventId, CommentSearchParams params, EntityManager em) {
        QComment comment = QComment.comment;
        QUser author = QUser.user;

        // ============================ фильтры ============================
        BooleanExpression[] predicates = new BooleanExpression[]{
                userId != null ? comment.author.id.eq(userId) : null,
                eventId != null ? comment.event.id.eq(eventId) : null,
                params.getState() != null ? comment.commentState.eq(params.getState()) : null
        };

        // ============================ строим запрос ============================
        JPAQuery<Comment> query = new JPAQuery<>(em);

        query.select(comment)
             .from(comment)
             .distinct()
             .leftJoin(comment.author, author).fetchJoin()
             .where(Expressions.allOf(predicates));

        // ============================ сортировка ============================
        if (params.getSort() == null || params.getSort() == SortOrder.ASC) {
            query.orderBy(comment.createdOn.asc());
        } else {
            query.orderBy(comment.createdOn.desc());
        }

        // ============================ пагинация ============================
        int from = params.getFrom() != null ? params.getFrom() : 0;
        int size = params.getSize() != null ? params.getSize() : 10;
        query.offset(from).limit(size);

        return query.fetch();
    }

    default List<Comment> searchEventCommentsPublic(Long eventId, CommentSearchParams params, EntityManager em) {
        QComment comment = QComment.comment;
        QUser author = QUser.user;

        BooleanExpression[] predicates = new BooleanExpression[]{
                comment.event.id.eq(eventId),
                comment.commentState.eq(CommentState.PUBLISHED)
        };

        JPAQuery<Comment> query = new JPAQuery<>(em);
        query.select(comment)
             .from(comment)
             .leftJoin(comment.author, author).fetchJoin()
             .where(Expressions.allOf(predicates));

        // сортировка
        if (params.getSort() == null || params.getSort() == SortOrder.ASC) {
            query.orderBy(comment.createdOn.asc());
        } else {
            query.orderBy(comment.createdOn.desc());
        }

        // пагинация
        int from = params.getFrom() != null ? params.getFrom() : 0;
        int size = params.getSize() != null ? params.getSize() : 10;
        query.offset(from).limit(size);

        return query.fetch();
    }

    // Подсчёт только PUBLISHED-комментариев по списку eventIds
    default Map<Long, Long> countPublishedByEventIds(List<Long> eventIds,
                                                     EntityManager em) {

        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        QComment c = QComment.comment;

        return new JPAQuery<Tuple>(em)
                .select(c.event.id, c.count())
                .from(c)
                .where(
                        c.event.id.in(eventIds)
                                  .and(c.commentState.eq(CommentState.PUBLISHED))
                )
                .groupBy(c.event.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        t -> t.get(c.event.id),
                        t -> t.get(c.count())
                ));
    }


    // Подсчёт только PUBLISHED-комментариев по одному событию
    long countByEventIdAndCommentState(Long eventId, CommentState state);
}
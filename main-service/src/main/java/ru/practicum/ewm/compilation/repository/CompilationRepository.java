package ru.practicum.ewm.compilation.repository;

import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.model.QCompilation;
import ru.practicum.ewm.event.model.QEvent;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompilationRepository extends JpaRepository<Compilation, Long>,
        QuerydslPredicateExecutor<Compilation> {

    QCompilation c = QCompilation.compilation;
    QEvent e = QEvent.event;

    // ============================================================
    // Получение одной компиляции с событиями + category + initiator
    // ============================================================
    public default Optional<Compilation> findByIdWithEvents(Long id, EntityManager em) {
        Compilation compilation = new JPAQuery<Compilation>(em)
                .select(c)
                .from(c)
                .leftJoin(c.events, e).fetchJoin()
                .leftJoin(e.category).fetchJoin()
                .leftJoin(e.initiator).fetchJoin()
                .where(c.id.eq(id))
                .fetchOne();

        return Optional.ofNullable(compilation);
    }

    // ============================================================
    // Получение компиляций с фильтром pinned и пагинацией
    // ============================================================
    public default List<Compilation> findCompilationsWithEvents(
            Boolean pinned,
            Pageable pageable,
            EntityManager em
    ) {
        // ----------------------------
        // 1. Получаем ID компиляций для текущей страницы
        // ----------------------------
        List<Long> ids = new JPAQuery<Long>(em)
                .select(c.id)
                .from(c)
                .where(pinned != null ? c.pinned.eq(pinned) : null)
                .orderBy(c.id.asc()) // стабильная сортировка для пагинации
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        if (ids.isEmpty()) {
            return List.of();
        }

        // ----------------------------
        // 2. Загружаем compilations + events + category + initiator
        // ----------------------------
        return new JPAQuery<Compilation>(em)
                .select(c)
                .from(c)
                .distinct()
                .leftJoin(c.events, e).fetchJoin()
                .leftJoin(e.category).fetchJoin()
                .leftJoin(e.initiator).fetchJoin()
                .where(c.id.in(ids))
                .orderBy(c.id.asc()) // сохраняем порядок
                .fetch();
    }
}


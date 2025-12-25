package ru.practicum.ewm.category.repository;

import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.model.QCategory;
import ru.practicum.ewm.event.model.Event;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>,
        QuerydslPredicateExecutor<Event> {


    default List<Category> findCategoriesWithPagination(int from, int size, EntityManager em) {
        QCategory category = QCategory.category;

        return new JPAQuery<Category>(em)
                .select(category)
                .from(category)
                .orderBy(category.id.asc())
                .offset(from)
                .limit(size)
                .fetch();
    }
}

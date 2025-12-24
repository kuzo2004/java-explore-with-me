package ru.practicum.ewm.user.repository;

import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.user.model.QUser;
import ru.practicum.ewm.user.model.User;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long>,
        QuerydslPredicateExecutor<User> {

    // ===== offset-based пагинация со списком id =====
    default List<User> findAllByIdIn(List<Long> ids, int from, int size, EntityManager em) {
        QUser user = QUser.user;

        return new JPAQuery<User>(em)
                .select(user)
                .from(user)
                .where(user.id.in(ids))
                .orderBy(user.id.asc())
                .offset(from)
                .limit(size)
                .fetch();
    }

    // ===== offset-based пагинация без фильтра =====
    default List<User> findAll(int from, int size, EntityManager em) {
        QUser user = QUser.user;

        return new JPAQuery<User>(em)
                .select(user)
                .from(user)
                .orderBy(user.id.asc())
                .offset(from)
                .limit(size)
                .fetch();
    }
}

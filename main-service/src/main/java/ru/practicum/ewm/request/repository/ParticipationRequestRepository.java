package ru.practicum.ewm.request.repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.QParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long>,
        QuerydslPredicateExecutor<ParticipationRequest> {

    boolean existsByEventIdAndRequesterId(Long eventId,
                                          Long requesterId);

    boolean existsByEventIdAndRequesterIdAndStatus(Long eventId,
                                                   Long requesterId,
                                                   RequestStatus status);


    /**
     * NOTE:
     * join fetch не используется осознанно —
     * текущий DTO требует только id связанных сущностей.
     * При расширении DTO необходимо пересмотреть стратегию загрузки.
     */
    List<ParticipationRequest> findAllByEventId(Long eventId);

    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    long countByEventIdAndStatus(Long eventId,
                                 RequestStatus status);

    Optional<ParticipationRequest> findByIdAndRequesterId(Long id,
                                                          Long requesterId);

    List<ParticipationRequest> findAllByIdInAndStatusOrderByCreatedAsc(List<Long> requestIds,
                                                                       RequestStatus requestStatus);

    List<ParticipationRequest> findAllByIdInAndStatus(List<Long> requestIds,
                                                      RequestStatus requestStatus);

    List<ParticipationRequest> findAllByIdInAndEventId(List<Long> requestIds, Long eventId);

    long countByIdInAndStatusNot(List<Long> requestIds,
                                 RequestStatus requestStatus);

    long countByEventId(Long eventId);


    public default Map<Long, Long> getConfirmedRequestsCountMap(List<Long> eventIds, EntityManager em) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        QParticipationRequest r = QParticipationRequest.participationRequest;

        JPAQuery<Tuple> query = new JPAQuery<>(em);
        List<Tuple> results = query
                .select(r.event.id, r.count())
                .from(r)
                .where(r.event.id.in(eventIds)
                                 .and(r.status.eq(RequestStatus.CONFIRMED)))
                .groupBy(r.event.id)
                .fetch();

        return results.stream()
                      .collect(Collectors.toMap(
                              tuple -> tuple.get(r.event.id),
                              tuple -> tuple.get(r.count())
                      ));
    }

    public default Map<Long, Long> getAllRequestsCountMap(List<Long> eventIds, EntityManager em) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        QParticipationRequest r = QParticipationRequest.participationRequest;

        return new JPAQuery<Tuple>(em)
                .select(r.event.id, r.count())
                .from(r)
                .where(r.event.id.in(eventIds))
                .groupBy(r.event.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        t -> t.get(r.event.id),
                        t -> t.get(r.count())
                ));
    }
}


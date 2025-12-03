package ru.practicum.ewm.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.dto.ViewStats;
import ru.practicum.ewm.model.EndpointHit;
import ru.practicum.ewm.repository.mapper.ViewStatsRowMapper;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class StatsRepositoryJdbc implements StatsRepository {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public void save(EndpointHit hit) {
        String sql = """
                INSERT INTO endpoint_hit (app, uri, ip, timestamp)
                VALUES (:app, :uri, :ip, :timestamp)
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("app", hit.getApp())
                .addValue("uri", hit.getUri())
                .addValue("ip", hit.getIp())
                .addValue("timestamp", Timestamp.valueOf(hit.getTimestamp()));

        jdbc.update(sql, params);
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start,
                                    LocalDateTime end,
                                    List<String> uris,
                                    boolean unique) {

        String countExpr = unique ? "COUNT(DISTINCT ip)" : "COUNT(*)";

        StringBuilder sql = new StringBuilder(
                "SELECT app, uri, %s AS hits FROM endpoint_hit WHERE timestamp BETWEEN :start AND :end"
                        .formatted(countExpr)
        );

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("start", Timestamp.valueOf(start))
                .addValue("end", Timestamp.valueOf(end));

        // Фильтрация по URI
        if (uris != null && !uris.isEmpty()) {
            sql.append(" AND uri IN (:uris)");
            params.addValue("uris", uris); // передаём List<String>, не массив
        }

        sql.append(" GROUP BY app, uri ORDER BY hits DESC");

        return jdbc.query(
                sql.toString(),
                params,
                new ViewStatsRowMapper()
        );
    }
}

package ru.practicum.ewm.repository.mapper;

import org.springframework.jdbc.core.RowMapper;
import ru.practicum.ewm.dto.ViewStats;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ViewStatsRowMapper implements RowMapper<ViewStats> {

    @Override
    public ViewStats mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ViewStats(
                rs.getString("app"),
                rs.getString("uri"),
                rs.getLong("hits")
        );
    }
}

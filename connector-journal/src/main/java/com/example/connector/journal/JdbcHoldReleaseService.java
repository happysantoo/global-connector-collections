package com.example.connector.journal;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Hold/release using connector_hold table and native SQL.
 */
public final class JdbcHoldReleaseService implements HoldReleaseService {

    private static final String INSERT_HOLD = "INSERT INTO connector_hold (correlation_id, held_until, reason) VALUES (?, ?, ?)";
    private static final String DELETE_HOLD = "DELETE FROM connector_hold WHERE correlation_id = ?";
    private static final String SELECT_DUE = "SELECT correlation_id FROM connector_hold WHERE held_until <= ?";

    private final JdbcTemplate jdbcTemplate;

    public JdbcHoldReleaseService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void hold(String correlationId, Instant heldUntil, String reason) {
        jdbcTemplate.update(INSERT_HOLD, correlationId, Timestamp.from(heldUntil), reason != null ? reason : "");
    }

    @Override
    public boolean release(String correlationId) {
        int n = jdbcTemplate.update(DELETE_HOLD, correlationId);
        return n > 0;
    }

    @Override
    public List<String> listDueForRelease() {
        return jdbcTemplate.query(SELECT_DUE, (rs, rowNum) -> rs.getString("correlation_id"), Timestamp.from(Instant.now()));
    }

    @Override
    public int releaseAllDue() {
        List<String> due = listDueForRelease();
        due.forEach(this::release);
        return due.size();
    }
}

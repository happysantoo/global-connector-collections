package com.example.connector.journal;

import com.example.connector.core.journal.JournalEntry;
import com.example.connector.core.journal.JournalWriter;
import com.example.connector.core.model.ConnectorMessage;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;

/**
 * JournalWriter implementation using Spring JDBC and native SQL. No JPA.
 */
public final class JdbcJournalWriter implements JournalWriter {

    private static final String INSERT = """
            INSERT INTO connector_journal (correlation_id, direction, transport, payload_type, payload_blob, headers_json, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, 'RECEIVED', ?)
            """;

    private static final String INSERT_RESPONSE = """
            INSERT INTO connector_journal (correlation_id, direction, transport, payload_type, payload_blob, headers_json, status, created_at, processed_at, error_message)
            SELECT correlation_id, 'response', transport, ?, ?, headers_json, ?, created_at, ?, ?
            FROM connector_journal WHERE correlation_id = ? AND direction = 'request' LIMIT 1
            """;

    private static final RowMapper<JournalEntry> ROW_MAPPER = (rs, rowNum) -> mapRow(rs);

    private final JdbcTemplate jdbcTemplate;

    public JdbcJournalWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Long> appendRequest(ConnectorMessage message) {
        var keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(INSERT, new String[]{"id"});
            ps.setString(1, message.correlationId());
            ps.setString(2, "request");
            ps.setString(3, message.transportType());
            ps.setString(4, "application/octet-stream");
            ps.setBytes(5, message.payload());
            ps.setString(6, "{}");
            ps.setObject(7, message.timestamp());
            return ps;
        }, keyHolder);
        var key = keyHolder.getKey();
        return key != null ? Optional.of(key.longValue()) : Optional.empty();
    }

    @Override
    public void updateResponse(String correlationId, String status, byte[] responsePayload, String errorMessage) {
        jdbcTemplate.update(INSERT_RESPONSE,
                "application/octet-stream",
                responsePayload != null ? responsePayload : new byte[0],
                status,
                Instant.now(),
                errorMessage,
                correlationId);
    }

    public Optional<JournalEntry> getByCorrelationId(String correlationId) {
        var list = jdbcTemplate.query(
                "SELECT id, correlation_id, direction, transport, payload_type, payload_blob, headers_json, status, created_at, processed_at, error_message FROM connector_journal WHERE correlation_id = ? AND direction = 'request'",
                ROW_MAPPER,
                correlationId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    private static JournalEntry mapRow(ResultSet rs) throws SQLException {
        return new JournalEntry(
                rs.getLong("id"),
                rs.getString("correlation_id"),
                rs.getString("direction"),
                rs.getString("transport"),
                rs.getString("payload_type"),
                rs.getBytes("payload_blob"),
                rs.getString("headers_json"),
                rs.getString("status"),
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null,
                rs.getTimestamp("processed_at") != null ? rs.getTimestamp("processed_at").toInstant() : null,
                rs.getString("error_message")
        );
    }
}

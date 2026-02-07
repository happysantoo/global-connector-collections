-- Connector journal and hold (same as connector-journal)
CREATE TABLE IF NOT EXISTS connector_journal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    correlation_id VARCHAR(255) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    transport VARCHAR(64) NOT NULL,
    payload_type VARCHAR(128),
    payload_blob BLOB,
    headers_json CLOB,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    error_message CLOB
);
CREATE INDEX IF NOT EXISTS idx_connector_journal_correlation_id ON connector_journal (correlation_id);
CREATE INDEX IF NOT EXISTS idx_connector_journal_created_at ON connector_journal (created_at);

CREATE TABLE IF NOT EXISTS connector_hold (
    correlation_id VARCHAR(255) PRIMARY KEY,
    held_until TIMESTAMP NOT NULL,
    reason VARCHAR(512)
);

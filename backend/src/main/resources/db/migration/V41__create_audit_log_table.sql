CREATE TABLE IF NOT EXISTS audit_log (
                                         id BIGSERIAL PRIMARY KEY,
                                         action VARCHAR(255),
                                         entity_type VARCHAR(255),
                                         entity_id VARCHAR(255),
                                         user_id BIGINT,
                                         details TEXT,
                                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- V12: Create Reports Table
-- For user and listing reports/flags

CREATE TABLE IF NOT EXISTS reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL,
    reported_entity_type VARCHAR(50) NOT NULL,
    reported_entity_id UUID NOT NULL,
    reason VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(50) DEFAULT 'PENDING',
    priority VARCHAR(50) DEFAULT 'MEDIUM',
    reviewed_by UUID,
    reviewed_at TIMESTAMP,
    resolution_notes TEXT,
    action_taken VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Create indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status);
CREATE INDEX IF NOT EXISTS idx_reports_entity ON reports(reported_entity_type, reported_entity_id);
CREATE INDEX IF NOT EXISTS idx_reports_reporter ON reports(reporter_id);
CREATE INDEX IF NOT EXISTS idx_reports_priority ON reports(priority);
CREATE INDEX IF NOT EXISTS idx_reports_created_at ON reports(created_at DESC);

-- Add comments
COMMENT ON TABLE reports IS 'User and listing reports for moderation';
COMMENT ON COLUMN reports.reported_entity_type IS 'Type of entity: USER, LISTING';
COMMENT ON COLUMN reports.reason IS 'Report reason: SPAM, FRAUD, INAPPROPRIATE, FAKE, SCAM, OTHER';
COMMENT ON COLUMN reports.status IS 'Report status: PENDING, REVIEWING, RESOLVED, DISMISSED';
COMMENT ON COLUMN reports.priority IS 'Report priority: LOW, MEDIUM, HIGH, CRITICAL';
COMMENT ON COLUMN reports.action_taken IS 'Action taken: REMOVED, WARNING, SUSPENDED, BANNED, NO_ACTION';

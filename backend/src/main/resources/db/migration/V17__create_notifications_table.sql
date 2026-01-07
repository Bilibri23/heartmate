-- V17: Create notifications table for in-app notifications

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    reference_id UUID,
    reference_type VARCHAR(50),
    action_url VARCHAR(500),
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient querying
CREATE INDEX idx_notification_user ON notifications(user_id);
CREATE INDEX idx_notification_read ON notifications(user_id, is_read);
CREATE INDEX idx_notification_created ON notifications(created_at DESC);

-- Add comments
COMMENT ON TABLE notifications IS 'In-app notifications for users';
COMMENT ON COLUMN notifications.type IS 'Type of notification (APPLICATION_RECEIVED, PAYMENT_VERIFIED, etc.)';
COMMENT ON COLUMN notifications.reference_id IS 'ID of the related entity (application, payment, etc.)';
COMMENT ON COLUMN notifications.reference_type IS 'Type of the related entity';
COMMENT ON COLUMN notifications.action_url IS 'URL to navigate to when notification is clicked';

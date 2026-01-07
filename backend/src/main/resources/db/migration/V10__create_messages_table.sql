-- Create messages table
CREATE TABLE messages (
    id UUID PRIMARY KEY,
    sender_id UUID NOT NULL,
    receiver_id UUID NOT NULL,
    content TEXT NOT NULL,
    message_type VARCHAR(50) NOT NULL DEFAULT 'TEXT',
    shared_listing_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    is_deleted_by_sender BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted_by_receiver BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Foreign keys
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_message_receiver FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_message_listing FOREIGN KEY (shared_listing_id) REFERENCES property_listings(id) ON DELETE SET NULL
);

-- Create indexes for efficient queries
CREATE INDEX idx_messages_conversation ON messages(sender_id, receiver_id);
CREATE INDEX idx_messages_receiver_unread ON messages(receiver_id) WHERE is_read = FALSE;
CREATE INDEX idx_messages_created ON messages(created_at DESC);
CREATE INDEX idx_messages_sender_created ON messages(sender_id, created_at DESC);
CREATE INDEX idx_messages_receiver_created ON messages(receiver_id, created_at DESC);

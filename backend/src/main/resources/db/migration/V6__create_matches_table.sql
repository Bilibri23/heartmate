-- =====================================================
-- MATCHES TABLE
-- =====================================================
CREATE TABLE matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user1_id UUID NOT NULL,
    user2_id UUID NOT NULL,
    compatibility_score INTEGER NOT NULL CHECK (compatibility_score >= 0 AND compatibility_score <= 100),
    
    -- Compatibility breakdown scores
    budget_score INTEGER,
    lifestyle_score INTEGER,
    schedule_score INTEGER,
    location_score INTEGER,
    habits_score INTEGER,
    
    -- Match status
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED')),
    
    -- User actions
    user1_action VARCHAR(20), -- ACCEPT, REJECT, or NULL
    user2_action VARCHAR(20), -- ACCEPT, REJECT, or NULL
    
    -- Algorithm version for tracking
    algorithm_version VARCHAR(20) DEFAULT '1.0',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    
    CONSTRAINT fk_matches_user1 FOREIGN KEY (user1_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_matches_user2 FOREIGN KEY (user2_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_user_match UNIQUE (user1_id, user2_id),
    CONSTRAINT check_different_users CHECK (user1_id != user2_id)
);

-- Create indexes
CREATE INDEX idx_matches_user1_id ON matches(user1_id);
CREATE INDEX idx_matches_user2_id ON matches(user2_id);
CREATE INDEX idx_matches_status ON matches(status);
CREATE INDEX idx_matches_compatibility_score ON matches(compatibility_score);
CREATE INDEX idx_matches_created_at ON matches(created_at);

-- Comments
COMMENT ON TABLE matches IS 'Roommate matches with compatibility scores';
COMMENT ON COLUMN matches.compatibility_score IS 'Overall compatibility score (0-100)';
COMMENT ON COLUMN matches.budget_score IS 'Budget compatibility score (0-100)';
COMMENT ON COLUMN matches.lifestyle_score IS 'Lifestyle compatibility score (0-100)';
COMMENT ON COLUMN matches.schedule_score IS 'Schedule compatibility score (0-100)';
COMMENT ON COLUMN matches.location_score IS 'Location compatibility score (0-100)';
COMMENT ON COLUMN matches.habits_score IS 'Habits compatibility score (0-100)';
COMMENT ON COLUMN matches.status IS 'Match status: PENDING, ACCEPTED, REJECTED, EXPIRED';
COMMENT ON COLUMN matches.user1_action IS 'Action taken by user1: ACCEPT, REJECT, or NULL';
COMMENT ON COLUMN matches.user2_action IS 'Action taken by user2: ACCEPT, REJECT, or NULL';


CREATE TABLE IF NOT EXISTS listing_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    min_budget INTEGER,
    max_budget INTEGER,
    preferred_locations TEXT[],
    max_distance_from_campus NUMERIC(10, 2),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_listing_preferences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

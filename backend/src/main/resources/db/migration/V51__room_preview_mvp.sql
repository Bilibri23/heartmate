CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE property_listings
    ADD COLUMN IF NOT EXISTS is_unfurnished BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS room_preview_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS room_length_meters NUMERIC(6,2),
    ADD COLUMN IF NOT EXISTS room_width_meters NUMERIC(6,2),
    ADD COLUMN IF NOT EXISTS room_height_meters NUMERIC(6,2),
    ADD COLUMN IF NOT EXISTS room_preview_photo_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS room_preview_status VARCHAR(40) NOT NULL DEFAULT 'NOT_ENABLED';

CREATE TABLE IF NOT EXISTS room_preview_layout (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL REFERENCES property_listings(id) ON DELETE CASCADE,
    layout_name VARCHAR(120) NOT NULL,
    items_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_room_preview_layout_user_listing
    ON room_preview_layout(user_id, listing_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_room_preview_layout_listing
    ON room_preview_layout(listing_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_property_listings_room_preview
    ON property_listings(room_preview_enabled, room_preview_status);

COMMENT ON COLUMN property_listings.room_preview_enabled IS 'Enables tenant-facing Room Preview for an unfurnished/listing room photo';
COMMENT ON COLUMN property_listings.room_preview_photo_url IS 'Listing photo URL selected by the landlord for Room Preview; must not reference private verification media';
COMMENT ON TABLE room_preview_layout IS 'Saved tenant Room Preview furniture layouts using normalized coordinates over listing photos';

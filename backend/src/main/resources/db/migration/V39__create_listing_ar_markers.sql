CREATE TABLE IF NOT EXISTS listing_ar_markers (
    id UUID PRIMARY KEY,
    listing_id UUID NOT NULL REFERENCES property_listings(id) ON DELETE CASCADE,
    x_coord DOUBLE PRECISION NOT NULL,
    y_coord DOUBLE PRECISION NOT NULL,
    z_coord DOUBLE PRECISION NOT NULL,
    label VARCHAR(120),
    color VARCHAR(24),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_listing_ar_markers_listing_id
    ON listing_ar_markers(listing_id);

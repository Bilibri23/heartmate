CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS visits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    landlord_id UUID NOT NULL,
    application_id UUID,
    requested_datetime TIMESTAMP NOT NULL,
    visit_datetime TIMESTAMP,
    status VARCHAR(40) NOT NULL DEFAULT 'REQUESTED',
    tenant_message TEXT,
    landlord_response TEXT,
    reschedule_reason TEXT,
    cancellation_reason TEXT,
    landlord_confirmed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_visit_listing FOREIGN KEY (listing_id) REFERENCES property_listings(id) ON DELETE CASCADE,
    CONSTRAINT fk_visit_tenant FOREIGN KEY (tenant_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_visit_landlord FOREIGN KEY (landlord_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_visit_application FOREIGN KEY (application_id) REFERENCES room_applications(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_visits_tenant ON visits(tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_visits_landlord ON visits(landlord_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_visits_listing ON visits(listing_id);
CREATE INDEX IF NOT EXISTS idx_visits_status ON visits(status);
CREATE INDEX IF NOT EXISTS idx_visits_visit_datetime ON visits(visit_datetime);
CREATE INDEX IF NOT EXISTS idx_visits_application ON visits(application_id);

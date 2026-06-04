CREATE TABLE platform_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    maintenance_mode BOOLEAN NOT NULL DEFAULT false,
    allow_new_registrations BOOLEAN NOT NULL DEFAULT true,
    require_email_verification BOOLEAN NOT NULL DEFAULT true,
    require_tenant_verification BOOLEAN NOT NULL DEFAULT true,
    auto_approve_listings BOOLEAN NOT NULL DEFAULT false,
    max_listings_per_landlord INTEGER NOT NULL DEFAULT 10,
    platform_fee_percent INTEGER NOT NULL DEFAULT 5,
    support_email VARCHAR(255) NOT NULL DEFAULT 'support@roombay.cm',
    notify_on_new_listing BOOLEAN NOT NULL DEFAULT true,
    notify_on_new_verification BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert default single row
INSERT INTO platform_settings (
    maintenance_mode,
    allow_new_registrations,
    require_email_verification,
    require_tenant_verification,
    auto_approve_listings,
    max_listings_per_landlord,
    platform_fee_percent,
    support_email,
    notify_on_new_listing,
    notify_on_new_verification
) VALUES (
    false, true, true, true, false, 10, 5, 'support@roombay.cm', true, true
);

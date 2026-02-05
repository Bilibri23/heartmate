-- V28: Add co-application and co-tenant support

-- Create co_application_invitations table
CREATE TABLE IF NOT EXISTS co_application_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES property_listings(id) ON DELETE CASCADE,
    inviter_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invitee_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    responded_at TIMESTAMP,
    decline_reason TEXT,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_co_invitation_listing_users UNIQUE (listing_id, inviter_id, invitee_id)
);

-- Add indexes for co_application_invitations
CREATE INDEX IF NOT EXISTS idx_co_invitation_invitee ON co_application_invitations(invitee_id, status);
CREATE INDEX IF NOT EXISTS idx_co_invitation_inviter ON co_application_invitations(inviter_id);
CREATE INDEX IF NOT EXISTS idx_co_invitation_listing ON co_application_invitations(listing_id);

-- Add co-applicant columns to room_applications
ALTER TABLE room_applications 
ADD COLUMN IF NOT EXISTS co_applicant_id UUID REFERENCES users(id) ON DELETE SET NULL,
ADD COLUMN IF NOT EXISTS match_id UUID REFERENCES matches(id) ON DELETE SET NULL,
ADD COLUMN IF NOT EXISTS is_co_application BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS co_applicant_confirmed BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS co_applicant_confirmed_at TIMESTAMP;

-- Add index for co-applications
CREATE INDEX IF NOT EXISTS idx_room_applications_co_applicant ON room_applications(co_applicant_id) WHERE co_applicant_id IS NOT NULL;

-- Add co-tenant columns to leases
ALTER TABLE leases
ADD COLUMN IF NOT EXISTS co_tenant_id UUID REFERENCES users(id) ON DELETE SET NULL,
ADD COLUMN IF NOT EXISTS is_shared_lease BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS co_tenant_signature TEXT,
ADD COLUMN IF NOT EXISTS co_tenant_signature_type VARCHAR(20),
ADD COLUMN IF NOT EXISTS co_tenant_accepted_terms BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS co_tenant_accepted_at TIMESTAMP;

-- Add index for shared leases
CREATE INDEX IF NOT EXISTS idx_leases_co_tenant ON leases(co_tenant_id) WHERE co_tenant_id IS NOT NULL;

-- Add comment
COMMENT ON TABLE co_application_invitations IS 'Stores invitations between matched roommates to apply together for listings';
COMMENT ON COLUMN room_applications.co_applicant_id IS 'The matched roommate applying together (co-tenant)';
COMMENT ON COLUMN leases.co_tenant_id IS 'The co-tenant/roommate sharing the lease';

-- Phase 1: Household Management System
-- Create households table (auto-created when shared lease is signed)
CREATE TABLE households (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lease_id UUID REFERENCES leases(id) ON DELETE SET NULL,
    listing_id UUID REFERENCES property_listings(id) ON DELETE SET NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Household members
CREATE TABLE household_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER', -- ADMIN, MEMBER
    rent_share_percentage INTEGER DEFAULT 50, -- Percentage of rent this member pays
    joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
    left_at TIMESTAMP,
    invite_status VARCHAR(20) DEFAULT 'ACCEPTED', -- PENDING, ACCEPTED, DECLINED
    CONSTRAINT unique_household_member UNIQUE (household_id, user_id)
);

-- Shared expenses
CREATE TABLE household_expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    paid_by UUID NOT NULL REFERENCES users(id),
    amount INTEGER NOT NULL, -- in XAF
    category VARCHAR(30) NOT NULL, -- RENT, UTILITIES, GROCERIES, SUPPLIES, INTERNET, CLEANING, OTHER
    description VARCHAR(255),
    receipt_url VARCHAR(500),
    expense_date DATE NOT NULL DEFAULT CURRENT_DATE,
    split_type VARCHAR(20) NOT NULL DEFAULT 'EQUAL', -- EQUAL, PERCENTAGE, FIXED, CUSTOM
    is_recurring BOOLEAN DEFAULT FALSE,
    recurring_day INTEGER, -- Day of month for recurring expenses
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Expense splits (who owes what)
CREATE TABLE expense_splits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id UUID NOT NULL REFERENCES household_expenses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    amount INTEGER NOT NULL, -- Amount this user owes
    is_paid BOOLEAN DEFAULT FALSE,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_expense_split UNIQUE (expense_id, user_id)
);

-- Settlement transactions (when someone pays their share)
CREATE TABLE expense_settlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    from_user UUID NOT NULL REFERENCES users(id),
    to_user UUID NOT NULL REFERENCES users(id),
    amount INTEGER NOT NULL,
    note VARCHAR(255),
    settlement_method VARCHAR(30), -- CASH, MOBILE_MONEY, BANK_TRANSFER
    settled_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_household_lease ON households(lease_id);
CREATE INDEX idx_household_listing ON households(listing_id);
CREATE INDEX idx_household_members_household ON household_members(household_id);
CREATE INDEX idx_household_members_user ON household_members(user_id);
CREATE INDEX idx_household_expenses_household ON household_expenses(household_id);
CREATE INDEX idx_household_expenses_paid_by ON household_expenses(paid_by);
CREATE INDEX idx_expense_splits_expense ON expense_splits(expense_id);
CREATE INDEX idx_expense_splits_user ON expense_splits(user_id);
CREATE INDEX idx_expense_settlements_household ON expense_settlements(household_id);

-- Comments
COMMENT ON TABLE households IS 'Represents a shared living arrangement between roommates';
COMMENT ON TABLE household_members IS 'Members of a household with their roles and rent share';
COMMENT ON TABLE household_expenses IS 'Shared expenses that need to be split among household members';
COMMENT ON TABLE expense_splits IS 'Individual splits of an expense per member';
COMMENT ON TABLE expense_settlements IS 'Record of payments between members to settle balances';

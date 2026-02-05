-- Phase 2: House Rules and Bill Reminders

-- House rules agreement
CREATE TABLE house_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    
    -- Quiet hours
    quiet_hours_enabled BOOLEAN DEFAULT FALSE,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    
    -- Guest policy
    guest_policy VARCHAR(30) DEFAULT 'NOTIFY', -- ALWAYS_OK, NOTIFY, ASK_FIRST, NO_OVERNIGHT, NO_GUESTS
    
    -- Cleaning
    cleaning_schedule VARCHAR(30) DEFAULT 'ROTATING', -- ROTATING, ASSIGNED, FLEXIBLE, SHARED
    
    -- Smoking/Drinking
    smoking_allowed BOOLEAN DEFAULT FALSE,
    smoking_area VARCHAR(100),
    drinking_allowed BOOLEAN DEFAULT TRUE,
    
    -- Pets
    pets_allowed BOOLEAN DEFAULT FALSE,
    pet_rules VARCHAR(255),
    
    -- Shared spaces
    kitchen_rules TEXT,
    bathroom_rules TEXT,
    living_room_rules TEXT,
    
    -- Custom rules
    custom_rules JSONB DEFAULT '[]'::jsonb,
    
    -- Agreement tracking
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    last_updated_by UUID REFERENCES users(id),
    
    CONSTRAINT unique_household_rules UNIQUE (household_id)
);

-- Rule agreements (each member must agree)
CREATE TABLE rule_agreements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    house_rules_id UUID NOT NULL REFERENCES house_rules(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    agreed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version INTEGER DEFAULT 1, -- Which version of rules they agreed to
    CONSTRAINT unique_rule_agreement UNIQUE (house_rules_id, user_id)
);

-- Bill reminders
CREATE TABLE bill_reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES users(id),
    
    name VARCHAR(100) NOT NULL, -- e.g., "Electricity Bill", "Internet"
    category VARCHAR(30) NOT NULL, -- RENT, UTILITIES, INTERNET, WATER, GAS, OTHER
    amount INTEGER, -- Expected amount (optional)
    
    -- Recurrence
    is_recurring BOOLEAN DEFAULT TRUE,
    due_day INTEGER, -- Day of month (1-31)
    reminder_days_before INTEGER DEFAULT 3, -- Send reminder X days before
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Bill reminder history
CREATE TABLE bill_reminder_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bill_reminder_id UUID NOT NULL REFERENCES bill_reminders(id) ON DELETE CASCADE,
    due_date DATE NOT NULL,
    is_paid BOOLEAN DEFAULT FALSE,
    paid_by UUID REFERENCES users(id),
    paid_at TIMESTAMP,
    amount_paid INTEGER,
    expense_id UUID REFERENCES household_expenses(id), -- Link to actual expense record
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_house_rules_household ON house_rules(household_id);
CREATE INDEX idx_rule_agreements_rules ON rule_agreements(house_rules_id);
CREATE INDEX idx_rule_agreements_user ON rule_agreements(user_id);
CREATE INDEX idx_bill_reminders_household ON bill_reminders(household_id);
CREATE INDEX idx_bill_reminder_history_reminder ON bill_reminder_history(bill_reminder_id);
CREATE INDEX idx_bill_reminder_history_due_date ON bill_reminder_history(due_date);

-- Comments
COMMENT ON TABLE house_rules IS 'Agreed-upon rules for the household';
COMMENT ON TABLE rule_agreements IS 'Track which members have agreed to the rules';
COMMENT ON TABLE bill_reminders IS 'Recurring bill reminders for the household';
COMMENT ON TABLE bill_reminder_history IS 'History of bill payments linked to reminders';

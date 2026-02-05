-- Phase 3: Task Management and Issue Reporting

-- Household tasks
CREATE TABLE household_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES users(id),
    
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(30) NOT NULL, -- CLEANING, SHOPPING, MAINTENANCE, COOKING, OTHER
    
    -- Assignment
    assigned_to UUID REFERENCES users(id),
    assignment_type VARCHAR(20) DEFAULT 'VOLUNTARY', -- VOLUNTARY, ROTATING, ASSIGNED
    
    -- Scheduling
    due_date DATE,
    due_time TIME,
    is_recurring BOOLEAN DEFAULT FALSE,
    recurrence_pattern VARCHAR(30), -- DAILY, WEEKLY, BIWEEKLY, MONTHLY
    recurrence_day INTEGER, -- Day of week (0-6) or day of month (1-31)
    
    -- Status
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, IN_PROGRESS, COMPLETED, OVERDUE, CANCELLED
    completed_by UUID REFERENCES users(id),
    completed_at TIMESTAMP,
    
    -- Priority
    priority VARCHAR(10) DEFAULT 'NORMAL', -- LOW, NORMAL, HIGH, URGENT
    
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Task rotation schedule (for rotating chores)
CREATE TABLE task_rotations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES household_tasks(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    rotation_order INTEGER NOT NULL,
    last_completed_at TIMESTAMP,
    CONSTRAINT unique_task_rotation UNIQUE (task_id, user_id)
);

-- Household issues / conflict reports
CREATE TABLE household_issues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
    reported_by UUID NOT NULL REFERENCES users(id),
    reported_against UUID REFERENCES users(id), -- Optional, might be general issue
    
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(30) NOT NULL, -- NOISE, CLEANLINESS, BILLS, GUESTS, BEHAVIOR, PROPERTY, OTHER
    severity VARCHAR(20) DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, CRITICAL
    
    -- Status tracking
    status VARCHAR(30) DEFAULT 'OPEN', -- OPEN, ACKNOWLEDGED, IN_DISCUSSION, RESOLVED, ESCALATED, CLOSED
    
    -- Resolution
    resolution TEXT,
    resolved_at TIMESTAMP,
    resolved_by UUID REFERENCES users(id),
    
    -- Escalation
    is_escalated BOOLEAN DEFAULT FALSE,
    escalated_at TIMESTAMP,
    escalated_to VARCHAR(50), -- PLATFORM_SUPPORT, LANDLORD
    
    -- Privacy
    is_anonymous BOOLEAN DEFAULT FALSE,
    
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Issue comments / discussion thread
CREATE TABLE issue_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL REFERENCES household_issues(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    is_resolution_proposal BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Issue votes (for resolution proposals)
CREATE TABLE issue_votes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comment_id UUID NOT NULL REFERENCES issue_comments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    vote VARCHAR(10) NOT NULL, -- AGREE, DISAGREE, ABSTAIN
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_issue_vote UNIQUE (comment_id, user_id)
);

-- Indexes
CREATE INDEX idx_household_tasks_household ON household_tasks(household_id);
CREATE INDEX idx_household_tasks_assigned ON household_tasks(assigned_to);
CREATE INDEX idx_household_tasks_status ON household_tasks(status);
CREATE INDEX idx_household_tasks_due_date ON household_tasks(due_date);
CREATE INDEX idx_task_rotations_task ON task_rotations(task_id);
CREATE INDEX idx_household_issues_household ON household_issues(household_id);
CREATE INDEX idx_household_issues_status ON household_issues(status);
CREATE INDEX idx_household_issues_reported_by ON household_issues(reported_by);
CREATE INDEX idx_issue_comments_issue ON issue_comments(issue_id);

-- Comments
COMMENT ON TABLE household_tasks IS 'Tasks and chores for the household';
COMMENT ON TABLE task_rotations IS 'Rotation schedule for recurring chores';
COMMENT ON TABLE household_issues IS 'Reported issues and conflicts within the household';
COMMENT ON TABLE issue_comments IS 'Discussion thread for issue resolution';
COMMENT ON TABLE issue_votes IS 'Votes on resolution proposals';

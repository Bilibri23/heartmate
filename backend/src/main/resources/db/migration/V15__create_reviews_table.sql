-- =====================================================
-- REVIEWS TABLE
-- Reviews and ratings for landlords, listings, and students
-- =====================================================

CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reviewer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reviewee_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lease_id UUID NOT NULL REFERENCES leases(id) ON DELETE CASCADE,
    listing_id UUID REFERENCES property_listings(id) ON DELETE SET NULL,
    
    -- Review type
    review_type VARCHAR(30) NOT NULL CHECK (review_type IN (
        'STUDENT_TO_LANDLORD', 'STUDENT_TO_LISTING', 
        'LANDLORD_TO_STUDENT', 'ROOMMATE_TO_ROOMMATE'
    )),
    
    -- Ratings (1-5 stars)
    overall_rating INTEGER NOT NULL CHECK (overall_rating >= 1 AND overall_rating <= 5),
    communication_rating INTEGER CHECK (communication_rating >= 1 AND communication_rating <= 5),
    accuracy_rating INTEGER CHECK (accuracy_rating >= 1 AND accuracy_rating <= 5),
    cleanliness_rating INTEGER CHECK (cleanliness_rating >= 1 AND cleanliness_rating <= 5),
    value_rating INTEGER CHECK (value_rating >= 1 AND value_rating <= 5),
    location_rating INTEGER CHECK (location_rating >= 1 AND location_rating <= 5),
    
    -- Review content
    comment TEXT,
    pros TEXT,
    cons TEXT,
    would_recommend BOOLEAN DEFAULT TRUE,
    
    -- Moderation
    visible BOOLEAN DEFAULT TRUE,
    flagged BOOLEAN DEFAULT FALSE,
    flag_reason VARCHAR(255),
    moderated_by UUID REFERENCES users(id),
    moderated_at TIMESTAMP,
    
    -- Response from reviewee
    response TEXT,
    response_at TIMESTAMP,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Unique constraint: one review per type per lease
    UNIQUE(reviewer_id, lease_id, review_type)
);

-- Indexes
CREATE INDEX idx_reviews_reviewer ON reviews(reviewer_id);
CREATE INDEX idx_reviews_reviewee ON reviews(reviewee_id);
CREATE INDEX idx_reviews_lease ON reviews(lease_id);
CREATE INDEX idx_reviews_listing ON reviews(listing_id);
CREATE INDEX idx_reviews_type ON reviews(review_type);
CREATE INDEX idx_reviews_visible ON reviews(visible) WHERE visible = TRUE;
CREATE INDEX idx_reviews_flagged ON reviews(flagged) WHERE flagged = TRUE;
CREATE INDEX idx_reviews_rating ON reviews(overall_rating);

-- Comments
COMMENT ON TABLE reviews IS 'Reviews and ratings after lease completion';
COMMENT ON COLUMN reviews.review_type IS 'Who is reviewing whom: student->landlord, student->listing, landlord->student';
COMMENT ON COLUMN reviews.visible IS 'FALSE if hidden by moderation';

export interface NeighborhoodAssessment {
  city: string;
  neighborhood: string;
  avgSafety?: number | null;
  avgAmenities?: number | null;
  avgTransport?: number | null;
  avgNoise?: number | null;
  overallScore?: number | null;
  reviewCount: number;
}

export interface NeighborhoodReview {
  id: string;
  reviewerFirstName?: string;
  city: string;
  neighborhood: string;
  safetyRating: number;
  amenitiesRating: number;
  transportRating: number;
  noiseRating: number;
  comment?: string;
  createdAt: string;
}

/** Payload for POST /api/neighborhoods/reviews. */
export interface NeighborhoodReviewForm {
  city: string;
  neighborhood: string;
  safetyRating: number;
  amenitiesRating: number;
  transportRating: number;
  noiseRating: number;
  comment?: string;
}

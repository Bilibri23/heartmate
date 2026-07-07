import api from "../lib/api";
import type { NeighborhoodAssessment, NeighborhoodReview, NeighborhoodReviewForm } from "../types/neighborhood-review";

interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const neighborhoodService = {
  getAssessment: async (city: string, neighborhood: string): Promise<NeighborhoodAssessment> => {
    const response = await api.get<NeighborhoodAssessment>("/neighborhoods/assessment", {
      params: { city, neighborhood },
    });
    return response.data;
  },

  getReviews: async (city: string, neighborhood: string, page = 0, size = 20) => {
    const response = await api.get<Page<NeighborhoodReview>>("/neighborhoods/reviews", {
      params: { city, neighborhood, page, size },
    });
    return response.data;
  },

  submitReview: async (form: NeighborhoodReviewForm): Promise<NeighborhoodReview> => {
    const response = await api.post<NeighborhoodReview>("/neighborhoods/reviews", form);
    return response.data;
  },
};

import api from '../lib/api';

export interface Listing {
  id: string;
  title: string;
  description: string;
  price: number; // frontend alias
  rentAmount?: number; // backend field name
  address: string;
  city: string;
  propertyType: string;
  bedrooms: number;
  bathrooms: number;
  area: number;
  availableFrom: string;
  images: string[];
  primaryPhotoUrl?: string;
  landlordId: string;
  createdAt: string;
  status: 'ACTIVE' | 'RENTED' | 'UNAVAILABLE';
  amenities: string[];
  isFavorite?: boolean;
}

export interface ListingRequest {
  title: string;
  description: string;
  price: number;
  address: string;
  city: string;
  propertyType: string;
  bedrooms: number;
  bathrooms: number;
  area: number;
  availableFrom: string;
  amenities: string[];
}

export const listingService = {
  // Landlord operations
  createListing: async (landlordId: string, data: ListingRequest) => {
    const response = await api.post<Listing>(`/listings?landlordId=${landlordId}`, data);
    return response.data;
  },

  getLandlordListings: async (landlordId: string) => {
    const response = await api.get<Listing[]>(`/listings/landlord/${landlordId}`);
    return response.data;
  },

  getLandlordStatistics: async (landlordId: string) => {
    const response = await api.get<any>(`/listings/landlord/${landlordId}/statistics`);
    return response.data;
  },

  // General operations
  searchListings: async (params: any) => {
    const response = await api.get<any>('/search', { params });
    return response.data;
  },

  getListing: async (id: string) => {
    const response = await api.get<Listing>(`/listings/${id}`);
    return response.data;
  },

  getFeaturedListings: async () => {
    const response = await api.get<Listing[]>('/listings/featured');
    return response.data;
  },

  // Tenant operations
  getFavorites: async (userId: string) => {
    const response = await api.get<Listing[]>(`/listings/favorites/${userId}`);
    return response.data;
  },

  toggleFavorite: async (listingId: string, userId: string) => {
    const response = await api.post(`/listings/${listingId}/favorite?userId=${userId}`);
    return response.data;
  }
};

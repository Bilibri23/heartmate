import api from '../lib/api';

export interface Profile {
  userId: string;
  bio?: string;
  profilePhotoUrl?: string;
  languages?: string[];
  whatsappNumber?: string;
  visibility: 'PUBLIC' | 'PRIVATE';
  // Add other profile fields
}

export const profileService = {
  getProfile: async (userId: string) => {
    const response = await api.get<Profile>(`/profiles/${userId}`);
    return response.data;
  },

  updateProfile: async (userId: string, data: any) => {
    const response = await api.put<Profile>(`/profiles/${userId}`, data);
    return response.data;
  },
  
  uploadProfilePhoto: async (userId: string, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.put<Profile>(`/profiles/${userId}`, formData);
    return response.data;
  }
};

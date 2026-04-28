import api from '../lib/api';
import { LoginRequest, RegisterRequest, AuthResponse, AuthMeResponse, UpdateMeRequest } from '@/types/auth';

export const authService = {
  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const response = await api.post<AuthResponse>('/auth/login', data);
    return response.data;
  },

  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const response = await api.post<AuthResponse>('/auth/register', data);
    return response.data;
  },

  getMe: async (): Promise<AuthMeResponse> => {
    const response = await api.get<AuthMeResponse>('/auth/me');
    return response.data;
  },

  updateMe: async (data: UpdateMeRequest): Promise<AuthMeResponse> => {
    const response = await api.patch<AuthMeResponse>('/auth/me', data);
    return response.data;
  },

  resendVerificationEmail: async (): Promise<void> => {
    await api.post('/auth/resend-verification-email');
  },

  sendPhoneVerificationOtp: async (phoneNumber: string): Promise<void> => {
    await api.post('/phone-verification/send', { phoneNumber });
  },

  verifyPhoneOtp: async (otpCode: string): Promise<void> => {
    await api.post('/phone-verification/verify', { otpCode });
  },

  resendPhoneVerificationOtp: async (): Promise<void> => {
    await api.post('/phone-verification/resend');
  },

  logout: () => {
    if (typeof window !== 'undefined') {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        api.post('/auth/logout', {}, { headers: { Authorization: `Bearer ${refreshToken}` } }).catch(() => undefined);
      }
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    }
  },

  getCurrentUser: () => {
    if (typeof window !== 'undefined') {
      const userStr = localStorage.getItem('user');
      return userStr ? JSON.parse(userStr) : null;
    }
    return null;
  },
  
  isAuthenticated: () => {
    if (typeof window !== 'undefined') {
      return !!localStorage.getItem('token');
    }
    return false;
  }
};

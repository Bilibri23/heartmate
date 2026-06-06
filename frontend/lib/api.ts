import axios from 'axios';

// Dev: same-origin `/api` so ngrok/mobile testers hit Next rewrites → localhost backend.
// Prod: full backend URL.
const API_URL =
  process.env.NODE_ENV === 'production'
    ? `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8082'}/api`
    : '/api';

// Uploads go through the same-origin Next proxy. This avoids browser/proxy HTTP2
// failures on cross-origin multipart requests while still reaching Spring.
const UPLOAD_API_URL = '/api';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// API instance for multipart uploads.
export const uploadApi = axios.create({
  baseURL: UPLOAD_API_URL,
});

type RetriableRequest = {
  _retry?: boolean;
  headers?: Record<string, string>;
};

const clearAuthStorage = () => {
  if (typeof window === 'undefined') return;
  localStorage.removeItem('token');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
};

const attachToken = (config: any) => {
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
};

// Add token interceptor to uploadApi as well
uploadApi.interceptors.request.use(
  (config) => attachToken(config),
  (error) => {
    return Promise.reject(error);
  }
);

// Add a request interceptor to attach the token
api.interceptors.request.use(
  (config) => attachToken(config),
  (error) => {
    return Promise.reject(error);
  }
);

// Add a response interceptor to handle auth errors
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = (error.config || {}) as RetriableRequest;
    if (error.response?.status === 401 && !originalRequest._retry) {
      const refreshToken = typeof window !== 'undefined' ? localStorage.getItem('refreshToken') : null;
      if (!refreshToken) {
        clearAuthStorage();
        return Promise.reject(error);
      }

      originalRequest._retry = true;
      try {
        const refreshResponse = await axios.post(
          `${API_URL}/auth/refresh`,
          {},
          { headers: { Authorization: `Bearer ${refreshToken}` } }
        );
        const { accessToken, refreshToken: rotatedRefreshToken } = refreshResponse.data;
        if (typeof window !== 'undefined') {
          localStorage.setItem('token', accessToken);
          localStorage.setItem('refreshToken', rotatedRefreshToken);
        }
        originalRequest.headers = originalRequest.headers || {};
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        clearAuthStorage();
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

export default api;

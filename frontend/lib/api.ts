import axios from 'axios';

// Use /api for development (proxied), direct URL for production
const API_URL = process.env.NODE_ENV === 'production' 
  ? `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8082'}/api`
  : '/api';
// Direct backend URL for file uploads and when bypassing proxy
const BACKEND_URL = `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8082'}/api`;

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Direct API instance for file uploads (bypasses Next.js proxy)
export const uploadApi = axios.create({
  baseURL: BACKEND_URL,
});

// Add token interceptor to uploadApi as well
uploadApi.interceptors.request.use(
  (config) => {
    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add a request interceptor to attach the token
api.interceptors.request.use(
  (config) => {
    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add a response interceptor to handle auth errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Handle unauthorized access (e.g., redirect to login)
      if (typeof window !== 'undefined') {
        localStorage.removeItem('token');
        // Optional: Redirect to login page
        // window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;

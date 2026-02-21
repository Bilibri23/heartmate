"use client";

import React, { createContext, useContext, useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { authService } from '../services/auth.service';
import { User, LoginRequest, RegisterRequest, AuthResponse } from '../types/auth';

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    const initAuth = () => {
      const currentUser = authService.getCurrentUser();
      setUser(currentUser);
      setIsLoading(false);
    };
    initAuth();
  }, []);

  const login = async (data: LoginRequest) => {
    try {
      const response = await authService.login(data);
      if (response.accessToken) {
        const user: User = {
          id: response.userId,
          firstName: response.firstName,
          lastName: response.lastName,
          role: response.role,
        };
        localStorage.setItem('token', response.accessToken);
        localStorage.setItem('refreshToken', response.refreshToken);
        localStorage.setItem('user', JSON.stringify(user));
        setUser(user);

        // Role-based redirect after login
        if (response.role === 'ADMIN') {
          router.push('/admin');
        } else if (response.role === 'LANDLORD') {
          router.push('/landlord');
        } else {
          router.push('/for-you');
        }
      }
    } catch (error) {
      console.error('Login failed:', error);
      throw error;
    }
  };

  const register = async (data: RegisterRequest) => {
    try {
      const response = await authService.register(data);
      if (response.accessToken) {
        const user: User = {
          id: response.userId,
          firstName: response.firstName,
          lastName: response.lastName,
          role: response.role,
        };
        localStorage.setItem('token', response.accessToken);
        localStorage.setItem('refreshToken', response.refreshToken);
        localStorage.setItem('user', JSON.stringify(user));
        setUser(user);

        // Role-based redirect after registration
        if (response.role === 'LANDLORD') {
          router.push('/landlord');
        } else {
          router.push('/onboarding');
        }
      } else {
        router.push('/login?registered=true');
      }
    } catch (error) {
      console.error('Registration failed:', error);
      throw error;
    }
  };

  const logout = () => {
    authService.logout();
    setUser(null);
    router.push('/login');
  };

  // Protect routes
  useEffect(() => {
    const publicRoutes = ['/login', '/register', '/', '/forgot-password'];
    if (!isLoading && !user && !publicRoutes.includes(pathname)) {
      router.push('/login');
    }
  }, [user, isLoading, pathname, router]);

  return (
    <AuthContext.Provider value={{ user, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

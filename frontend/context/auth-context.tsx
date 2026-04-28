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
  /** Reload user flags (e.g. emailVerified) from GET /auth/me and sync localStorage. */
  refreshUser: () => Promise<void>;
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
          emailVerified: response.emailVerified,
          phoneVerified: response.phoneVerified,
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
      const ax = error as { response?: { status?: number; data?: unknown } };
      if (ax.response) {
        console.error('Login failed:', ax.response.status, ax.response.data);
      } else {
        console.error('Login failed:', error);
      }
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
          emailVerified: response.emailVerified,
          phoneVerified: response.phoneVerified,
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

  const refreshUser = async () => {
    if (typeof globalThis.window === "undefined") return;
    const token = globalThis.window.localStorage.getItem("token");
    if (!token) return;
    try {
      const me = await authService.getMe();
      const prev = authService.getCurrentUser();
      if (!prev) return;
      const merged: User = {
        ...prev,
        firstName: me.firstName ?? prev.firstName,
        lastName: me.lastName ?? prev.lastName,
        emailVerified: me.emailVerified,
        phoneVerified: me.phoneVerified,
        email: me.email ?? prev.email,
        phone: me.phone ?? prev.phone,
      };
      globalThis.window.localStorage.setItem("user", JSON.stringify(merged));
      setUser(merged);
    } catch {
      /* ignore */
    }
  };

  // Protect routes — allow browsing listings/search without an account (housing-first)
  useEffect(() => {
    const isPublicRoute = (path: string) => {
      const exact = ['/login', '/register', '/', '/forgot-password', '/auth/oauth-callback'];
      if (exact.includes(path)) return true;
      if (path === '/search') return true;
      if (path === '/listings' || path.startsWith('/listings/')) return true;
      return false;
    };
    if (!isLoading && !user && !isPublicRoute(pathname)) {
      router.push('/login');
    }
  }, [user, isLoading, pathname, router]);

  return (
    <AuthContext.Provider value={{ user, isLoading, login, register, logout, refreshUser }}>
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

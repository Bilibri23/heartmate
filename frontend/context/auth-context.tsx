"use client";

import React, { createContext, useContext, useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { authService } from '../services/auth.service';
import { User, LoginRequest, RegisterRequest } from '../types/auth';

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  mounted: boolean;
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
  /** Reload user flags (e.g. emailVerified) from GET /auth/me and sync localStorage. */
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

function mergeUserFromMe(previous: User | null, me: Awaited<ReturnType<typeof authService.getMe>>): User {
  return {
    id: me.userId || previous?.id || '',
    firstName: me.firstName ?? previous?.firstName ?? '',
    lastName: me.lastName ?? previous?.lastName ?? '',
    role: me.role ?? previous?.role ?? '',
    email: me.email ?? previous?.email,
    phone: me.phone ?? previous?.phone,
    verificationStatus: previous?.verificationStatus,
    emailVerified: me.emailVerified ?? previous?.emailVerified,
    phoneVerified: me.phoneVerified ?? previous?.phoneVerified,
  };
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [mounted, setMounted] = useState(false);
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    let cancelled = false;
    setMounted(true);

    const initAuth = async () => {
      const currentUser = authService.getCurrentUser();
      const token = typeof window !== 'undefined' ? window.localStorage.getItem('token') : null;

      if (!token) {
        if (!cancelled) {
          setUser(null);
          setIsLoading(false);
        }
        return;
      }

      if (currentUser && !cancelled) {
        setUser(currentUser);
      }

      try {
        const me = await authService.getMe();
        if (cancelled) return;
        const merged = mergeUserFromMe(currentUser, me);
        window.localStorage.setItem('user', JSON.stringify(merged));
        setUser(merged);
      } catch {
        if (!cancelled && !currentUser) {
          setUser(null);
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    };

    void initAuth();
    return () => {
      cancelled = true;
    };
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
          router.replace('/admin');
        } else if (response.role === 'LANDLORD') {
          router.replace('/landlord');
        } else if (response.role === 'REALTOR') {
          router.replace('/realtor');
        } else {
          router.replace('/for-you');
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
          router.replace('/landlord');
        } else if (response.role === 'REALTOR') {
          router.replace('/realtor/onboarding');
        } else {
          router.replace('/onboarding');
        }
      } else {
        router.replace('/login?registered=true');
      }
    } catch (error) {
      console.error('Registration failed:', error);
      throw error;
    }
  };

  const logout = () => {
    authService.logout();
    setUser(null);
    router.replace('/login');
  };

  const refreshUser = async () => {
    if (typeof globalThis.window === "undefined") return;
    const token = globalThis.window.localStorage.getItem("token");
    if (!token) return;
    try {
      const me = await authService.getMe();
      const prev = authService.getCurrentUser();
      const merged = mergeUserFromMe(prev, me);
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
    // Avoid redirecting during SSR/first render until client is mounted and auth state is resolved
    if (!mounted || isLoading) return;
    if (!user && !isPublicRoute(pathname)) {
      router.replace('/login');
    }
  }, [user, isLoading, pathname, router, mounted]);

  return (
    <AuthContext.Provider value={{ user, isLoading, mounted, login, register, logout, refreshUser }}>
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

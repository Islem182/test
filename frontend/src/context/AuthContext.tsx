import React, { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import type { UserProfile, JwtResponse } from '../types';
import { authService } from '../services/auth.service';
import { userService } from '../services/user.service';

interface AuthContextType {
  user: UserProfile | null;
  token: string | null;
  isAuthenticated: boolean;
  loading: boolean;
  isAdmin: boolean;
  login: (usernameOrEmail: string, password: string) => Promise<JwtResponse>;
  register: (data: Record<string, any>) => Promise<JwtResponse>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser]   = useState<UserProfile | null>(() => {
    const saved = localStorage.getItem('user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('accessToken'));
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const init = async () => {
      if (token) {
        try {
          const profile = await userService.getProfile();
          setUser(profile);
          localStorage.setItem('user', JSON.stringify(profile));
        } catch {
          setUser(null);
          setToken(null);
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          localStorage.removeItem('user');
        }
      } else {
        setUser(null);
      }
      setLoading(false);
    };
    init();
  }, [token]);

  const login = async (usernameOrEmail: string, password: string): Promise<JwtResponse> => {
    const data = await authService.login(usernameOrEmail, password);
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    setToken(data.accessToken);
    try {
      const profile = await userService.getProfile();
      setUser(profile);
      localStorage.setItem('user', JSON.stringify(profile));
    } catch {
      const fallback: UserProfile = {
        id: data.id, username: data.username, email: data.email,
        enabled: true, isAdmin: data.isAdmin,
      };
      setUser(fallback);
      localStorage.setItem('user', JSON.stringify(fallback));
    }
    return data;
  };

  const register = async (formData: Record<string, any>): Promise<JwtResponse> => {
    const data = await authService.register(formData);
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    setToken(data.accessToken);
    try {
      const profile = await userService.getProfile();
      setUser(profile);
      localStorage.setItem('user', JSON.stringify(profile));
    } catch {
      const fallback: UserProfile = {
        id: data.id, username: data.username, email: data.email,
        enabled: true, isAdmin: false,
      };
      setUser(fallback);
      localStorage.setItem('user', JSON.stringify(fallback));
    }
    return data;
  };

  const logout = async () => {
    await authService.logout();
    setUser(null);
    setToken(null);
  };

  const refreshProfile = async () => {
    if (token) {
      const profile = await userService.getProfile();
      setUser(profile);
      localStorage.setItem('user', JSON.stringify(profile));
    }
  };

  const isAdmin = user?.isAdmin === true;

  return (
    <AuthContext.Provider
      value={{ user, token, isAuthenticated: !!token && !!user, loading, isAdmin,
               login, register, logout, refreshProfile }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
};

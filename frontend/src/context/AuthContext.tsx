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
  login: (usernameOrEmail: string, password: string) => Promise<JwtResponse>;
  register: (data: Record<string, any>) => Promise<JwtResponse>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
  hasRole: (role: string) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserProfile | null>(() => {
    const saved = localStorage.getItem('user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('accessToken'));
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    const initAuth = async () => {
      if (token) {
        try {
          const profile = await userService.getProfile();
          setUser(profile);
          localStorage.setItem('user', JSON.stringify(profile));
        } catch (err) {
          console.error('Failed to fetch user profile:', err);
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

    initAuth();
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
      const fallbackUser: UserProfile = {
        id: data.id,
        username: data.username,
        email: data.email,
        enabled: true,
        roles: data.roles,
      };
      setUser(fallbackUser);
      localStorage.setItem('user', JSON.stringify(fallbackUser));
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
      const fallbackUser: UserProfile = {
        id: data.id,
        username: data.username,
        email: data.email,
        enabled: true,
        roles: data.roles,
      };
      setUser(fallbackUser);
      localStorage.setItem('user', JSON.stringify(fallbackUser));
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

  const hasRole = (role: string): boolean => {
    if (!user || !user.roles) return false;
    const formattedRole = role.startsWith('ROLE_') ? role : `ROLE_${role}`;
    return user.roles.some((r) => r === role || r === formattedRole);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token && !!user,
        loading,
        login,
        register,
        logout,
        refreshProfile,
        hasRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

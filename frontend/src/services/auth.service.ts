import { apiClient } from '../api/axios';
import type { ApiResponse, JwtResponse } from '../types';

export const authService = {
  async register(data: Record<string, any>): Promise<JwtResponse> {
    const res = await apiClient.post<ApiResponse<JwtResponse>>('/api/auth/register', data);
    return res.data.data;
  },

  async login(usernameOrEmail: string, password: string): Promise<JwtResponse> {
    const res = await apiClient.post<ApiResponse<JwtResponse>>('/api/auth/login', {
      usernameOrEmail,
      password,
    });
    return res.data.data;
  },

  async logout(): Promise<void> {
    try {
      await apiClient.post('/api/auth/logout');
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    }
  },

  async forgotPassword(email: string): Promise<string> {
    const res = await apiClient.post<ApiResponse<string>>('/api/auth/forgot-password', { email });
    return res.data.message;
  },

  async resetPassword(token: string, newPassword: string): Promise<string> {
    const res = await apiClient.post<ApiResponse<string>>('/api/auth/reset-password', {
      token,
      newPassword,
    });
    return res.data.message;
  },
};

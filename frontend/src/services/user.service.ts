import { apiClient } from '../api/axios';
import type { ApiResponse, UserProfile } from '../types';

export const userService = {
  async getProfile(): Promise<UserProfile> {
    const res = await apiClient.get<ApiResponse<UserProfile>>('/api/users/profile');
    return res.data.data;
  },

  async updateProfile(data: Partial<UserProfile>): Promise<UserProfile> {
    const res = await apiClient.put<ApiResponse<UserProfile>>('/api/users/profile', data);
    return res.data.data;
  },

  async changePassword(currentPassword: string, newPassword: string): Promise<string> {
    const res = await apiClient.put<ApiResponse<string>>('/api/users/password', {
      currentPassword,
      newPassword,
    });
    return res.data.message;
  },

  async uploadProfileImage(file: File): Promise<UserProfile> {
    const formData = new FormData();
    formData.append('file', file);
    const res = await apiClient.put<ApiResponse<UserProfile>>('/api/users/profile/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data.data;
  },

  async getAllUsers(): Promise<UserProfile[]> {
    const res = await apiClient.get<ApiResponse<UserProfile[]>>('/api/users');
    return res.data.data;
  },
};

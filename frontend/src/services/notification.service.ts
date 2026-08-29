import { apiClient } from '../api/axios';
import type { ApiResponse, NotificationItem } from '../types';

export const notificationService = {
  async getNotifications(): Promise<NotificationItem[]> {
    const res = await apiClient.get<ApiResponse<NotificationItem[]>>('/api/notifications');
    return res.data.data;
  },

  async markAsRead(id: number): Promise<void> {
    await apiClient.put(`/api/notifications/${id}/read`);
  },

  async markAllAsRead(): Promise<void> {
    await apiClient.put('/api/notifications/read-all');
  },

  async getUnreadCount(): Promise<number> {
    const res = await apiClient.get<ApiResponse<number>>('/api/notifications/unread-count');
    return res.data.data;
  },
};

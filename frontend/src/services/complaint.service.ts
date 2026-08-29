import { apiClient } from '../api/axios';
import type { ApiResponse, Complaint } from '../types';

export const complaintService = {
  async createComplaint(subject: string, message: string): Promise<Complaint> {
    const res = await apiClient.post<ApiResponse<Complaint>>('/api/complaints', { subject, message });
    return res.data.data;
  },

  async getComplaints(): Promise<Complaint[]> {
    const res = await apiClient.get<ApiResponse<Complaint[]>>('/api/complaints');
    return res.data.data;
  },

  async respondToComplaint(id: number, response: string): Promise<Complaint> {
    const res = await apiClient.put<ApiResponse<Complaint>>(`/api/complaints/${id}/respond`, { response });
    return res.data.data;
  },
};

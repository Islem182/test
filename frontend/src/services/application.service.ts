import { apiClient } from '../api/axios';
import type { ApiResponse, TaxApplication, ApplicationStatus } from '../types';

export const applicationService = {
  async createApplication(payload: { activities: any[]; documents: any[] }): Promise<TaxApplication> {
    const res = await apiClient.post<ApiResponse<TaxApplication>>('/api/applications', payload);
    return res.data.data;
  },

  async getApplicationById(id: number): Promise<TaxApplication> {
    const res = await apiClient.get<ApiResponse<TaxApplication>>(`/api/applications/${id}`);
    return res.data.data;
  },

  async getApplicationsStatus(statusFilter?: ApplicationStatus): Promise<TaxApplication[]> {
    const params = statusFilter ? { filterStatus: statusFilter } : {};
    const res = await apiClient.get<ApiResponse<TaxApplication[]>>('/api/applications/status', { params });
    return res.data.data;
  },

  async getAllApplications(): Promise<TaxApplication[]> {
    const res = await apiClient.get<ApiResponse<TaxApplication[]>>('/api/applications');
    return res.data.data;
  },

  async updateApplicationStatus(id: number, status: ApplicationStatus, notes?: string): Promise<TaxApplication> {
    const res = await apiClient.put<ApiResponse<TaxApplication>>(`/api/applications/${id}/status`, {
      status,
      notes,
    });
    return res.data.data;
  },

  async uploadDocument(applicationId: number, file: File, type: string): Promise<any> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', type);
    const res = await apiClient.post(`/api/applications/${applicationId}/documents`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data.data;
  },
};

import { apiClient } from '../api/axios';
import type { ApiResponse, OcrVerificationReport } from '../types';

export interface VerifyDocumentsPayload {
  cinData:       Record<string, string>;
  passportData:  Record<string, string>;
  certificatData: Record<string, string>;
}

export const comparisonService = {
  /**
   * POST /api/ocr/verify-documents
   *
   * Sends the three extractedData maps already received in Step 3
   * and returns the full structured verification report.
   */
  async verifyDocuments(payload: VerifyDocumentsPayload): Promise<OcrVerificationReport> {
    const res = await apiClient.post<ApiResponse<OcrVerificationReport>>(
      '/api/ocr/verify-documents',
      payload
    );
    return res.data.data;
  },
};

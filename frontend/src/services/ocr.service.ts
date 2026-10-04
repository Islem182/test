import { apiClient } from '../api/axios';
import type { ApiResponse, OcrVerificationResponse, OcrVerificationReport } from '../types';

export const ocrService = {
  /**
   * Upload a CIN, Passport, or Certificat image to the backend OCR engine.
   * The file is sent as multipart/form-data and processed in-memory
   * by Tesseract — never stored on the server.
   *
   * @param file         JPG / JPEG / PNG image, max 10 MB
   * @param documentType "CIN" | "PASSPORT" | "CERTIFICAT_IMMATRICULATION"
   * @returns structured OCR result with extracted identity fields
   */
  async uploadAndExtract(
    file: File,
    documentType: 'CIN' | 'PASSPORT' | 'CERTIFICAT_IMMATRICULATION',
    onUploadProgress?: (percent: number) => void
  ): Promise<OcrVerificationResponse> {
    const formData = new FormData();
    formData.append('file', file);

    const res = await apiClient.post<ApiResponse<OcrVerificationResponse>>(
      `/api/ocr/extract-document?documentType=${documentType}`,
      formData,
      {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (progressEvent: any) => {
          if (onUploadProgress && progressEvent.total) {
            const percent = Math.round(
              (progressEvent.loaded * 100) / progressEvent.total
            );
            onUploadProgress(percent);
          }
        },
      }
    );

    return res.data.data;
  },

  /**
   * Send extracted document maps and user-entered form data to the backend verification engine.
   */
  async verifyDocuments(payload: {
    cinData: Record<string, string>;
    passportData: Record<string, string>;
    certificatData: Record<string, string>;
    userEnteredData: Record<string, string>;
  }): Promise<OcrVerificationReport> {
    const res = await apiClient.post<ApiResponse<OcrVerificationReport>>(
      '/api/ocr/verify-documents',
      payload
    );
    return res.data.data;
  },
};

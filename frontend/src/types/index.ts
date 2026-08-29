export type UserRole = 'ROLE_TAXPAYER' | 'ROLE_AGENT' | 'ROLE_ADMIN' | 'TAXPAYER' | 'AGENT' | 'ADMIN';

export type ApplicationStatus = 'PENDING' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED';

export type DocumentType = 
  | 'CIN_COPY' 
  | 'PASSPORT_COPY' 
  | 'PATENTE' 
  | 'COMMERCIAL_REGISTER' 
  | 'TAX_RETURN' 
  | 'OTHER';

export type VerificationStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';

export type ComplaintStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';

export interface User {
  id: number;
  username: string;
  email: string;
  phone?: string;
  enabled: boolean;
  createdAt?: string;
  roles: string[];
}

export interface TaxpayerProfile {
  taxpayerId?: number;
  firstName?: string;
  lastName?: string;
  cin?: string;
  passport?: string;
  birthDate?: string;
  address?: string;
  profilePicture?: string;
  fiscalNumber?: string;
}

export interface UserProfile extends User, TaxpayerProfile {}

export interface JwtResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  id: number;
  username: string;
  email: string;
  roles: string[];
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface Activity {
  id?: number;
  businessName: string;
  businessType?: string;
  sector?: string;
  address?: string;
  startDate?: string;
}

export interface DocumentItem {
  id?: number;
  type: DocumentType;
  filePath: string;
  fileName?: string;
  fileSize?: string;
  uploadDate?: string;
  verificationStatus?: VerificationStatus;
}

export interface TaxApplication {
  id: number;
  taxpayerId: number;
  taxpayerName: string;
  status: ApplicationStatus;
  createdDate: string;
  updatedDate: string;
  activities: Activity[];
  documents: DocumentItem[];
}

export interface Complaint {
  id: number;
  taxpayerId: number;
  taxpayerName: string;
  subject: string;
  message: string;
  status: ComplaintStatus;
  response?: string;
  createdAt: string;
}

export interface NotificationItem {
  id: number;
  title: string;
  message: string;
  readStatus: boolean;
  createdAt: string;
}

export interface ChatMessage {
  id?: number;
  sender: 'user' | 'bot';
  text: string;
  timestamp: string;
}

export interface OcrResult {
  cinNumber?: string;
  fullName?: string;
  birthDate?: string;
  address?: string;
  confidenceScore: number;
  documentType: string;
}

export interface FaceVerificationResult {
  matched: boolean;
  similarityScore: number;
  livenessVerified: boolean;
}

// ── Auth ──────────────────────────────────────────────────────────────────────

export type ApplicationStatus = 'PENDING' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED';
export type VerificationStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';
export type ComplaintStatus    = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';

export type DocumentType =
  | 'CIN_COPY'
  | 'PASSPORT_COPY'
  | 'CERTIFICAT_IMMATRICULATION'
  | 'PATENTE'
  | 'COMMERCIAL_REGISTER'
  | 'TAX_RETURN'
  | 'OTHER';

/** Flat user — no separate Taxpayer entity, no role array. */
export interface UserProfile {
  id: number;
  username: string;
  email: string;
  phone?: string;
  enabled: boolean;
  isAdmin: boolean;
  createdAt?: string;
  // Identity / taxpayer fields
  firstName?: string;
  lastName?: string;
  cin?: string;
  passport?: string;
  birthDate?: string;
  address?: string;
  profilePicture?: string;
  fiscalNumber?: string;
}

export interface JwtResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  id: number;
  username: string;
  email: string;
  isAdmin: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

// ── Domain ────────────────────────────────────────────────────────────────────

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
  userId: number;
  taxpayerName: string;
  status: ApplicationStatus;
  createdDate: string;
  updatedDate: string;
  activities: Activity[];
  documents: DocumentItem[];
}

export interface Complaint {
  id: number;
  userId: number;
  userName: string;
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

// ── OCR ───────────────────────────────────────────────────────────────────────

/**
 * Mirrors backend OcrVerificationResponse.
 * extractedData keys:
 *   CIN      → cinNumber, firstName, lastName, birthDate, address
 *   PASSPORT → passportNumber, firstName, lastName, birthDate, nationality
 *   CERTIFICAT_IMMATRICULATION → companyName, fiscalNumber, businessType,
 *                                 registrationNumber, registrationDate, address
 */
export interface OcrVerificationResponse {
  verification: 'SUCCESS' | 'PARTIAL' | 'FAILED';
  confidenceScore: number;
  documentType: string;
  extractedData: Record<string, string>;
  differences: string[];
  statusMessage: string;
}

// ── OCR Verification Report (Step 4) ─────────────────────────────────────────

/** Status for a single extracted field. */
export type OcrFieldStatus = 'MATCH' | 'MISMATCH' | 'NOT_FOUND' | 'VALID' | 'MISSING' | 'INVALID';

/** Match status between two documents. */
export type IdentityMatchStatus = 'MATCH' | 'POSSIBLE_MATCH' | 'MISMATCH' | 'UNABLE_TO_COMPARE';

/** Overall document/report status. */
export type OverallStatus = 'VALID' | 'INVALID' | 'PARTIAL';

/** Single field result inside a document section. */
export interface OcrFieldResult {
  fieldName?: string;
  extractedValue?: string | null;
  enteredValue?: string | null;
  value?: string | null;
  status: OcrFieldStatus;
  message?: string;
  confidence?: number;
}

/** CIN document verification section. */
export interface CinVerification {
  numeroCin: OcrFieldResult;
  nom: OcrFieldResult;
  prenom: OcrFieldResult;
  adresse?: OcrFieldResult;
  documentStatus: OverallStatus | string;
}

/** Passport document verification section. */
export interface PassportVerification {
  numeroPassport: OcrFieldResult;
  nom: OcrFieldResult;
  prenom: OcrFieldResult;
  identityCompatibility: IdentityMatchStatus | string;
  compatibilityScore: number;
  documentStatus: OverallStatus | string;
}

/** Certificat d'immatriculation verification section. */
export interface CertificatVerification {
  dateDebutActivite: OcrFieldResult;
  denominationSociale: OcrFieldResult;
  formeJuridique: OcrFieldResult;
  siegeSocial: OcrFieldResult;
  secteurActivite: OcrFieldResult;
  documentStatus: OverallStatus | string;
}

/** Full structured verification report returned by POST /api/ocr/verify-documents. */
export interface OcrVerificationReport {
  cin: CinVerification;
  passport: PassportVerification;
  commercialRegistration: CertificatVerification;
  overallStatus: OverallStatus | string;
  verified?: boolean;
  identityConsistency: IdentityMatchStatus | string;
  summary: string;
}

// ── Cross-document comparison (legacy — used by NewApplication comparison table) ──

export type ComparisonStatus = 'MATCH' | 'MISMATCH' | 'PENDING' | 'MISSING';

export interface FieldComparison {
  label: string;
  sourceA: string | undefined;
  sourceALabel: string;
  sourceB: string | undefined;
  sourceBLabel: string;
  status: ComparisonStatus;
}

export interface CrossDocumentComparison {
  fields: FieldComparison[];
  allMatch: boolean;
}

// ── Misc ──────────────────────────────────────────────────────────────────────

export interface FaceVerificationResult {
  matched: boolean;
  similarityScore: number;
  livenessVerified: boolean;
}

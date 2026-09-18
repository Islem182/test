import React, { useState } from 'react';
import {
  Box,
  Button,
  Typography,
  LinearProgress,
  Paper,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  IconButton,
  Alert,
  useTheme,
} from '@mui/material';
import {
  CloudUpload as UploadIcon,
  Delete as DeleteIcon,
  CheckCircle as SuccessIcon,
} from '@mui/icons-material';
import type { DocumentType, DocumentItem, OcrVerificationResponse } from '../types';
import { ocrService } from '../services/ocr.service';

interface FileUploaderProps {
  /** Called after a successful upload + OCR extraction. */
  onFileUpload: (document: DocumentItem, ocrResult?: OcrVerificationResponse) => void;
  /** Restrict the document type selector to these values. Default: all types. */
  allowedTypes?: DocumentType[];
  /**
   * When true the component will POST the image to the OCR backend and
   * return the extraction result via onFileUpload. Default: true.
   */
  enableOcr?: boolean;
}

const ACCEPTED_MIME = 'image/jpeg,image/jpg,image/png';

/** Human-readable French labels for every document type. */
const DOC_TYPE_LABELS: Record<string, string> = {
  CIN_COPY:                   'Carte d\'Identité Nationale (CIN)',
  PASSPORT_COPY:              'Passeport',
  CERTIFICAT_IMMATRICULATION: 'Certificat d\'immatriculation',
  PATENTE:                    'Patente',
  COMMERCIAL_REGISTER:        'Registre de commerce',
  TAX_RETURN:                 'Déclaration fiscale',
  OTHER:                      'Autre document',
};

/** Maps frontend DocumentType to the backend OCR documentType string. */
const OCR_DOC_TYPE_MAP: Partial<Record<string, 'CIN' | 'PASSPORT' | 'CERTIFICAT_IMMATRICULATION'>> = {
  CIN_COPY:                   'CIN',
  PASSPORT_COPY:              'PASSPORT',
  CERTIFICAT_IMMATRICULATION: 'CERTIFICAT_IMMATRICULATION',
};

export const FileUploader: React.FC<FileUploaderProps> = ({
  onFileUpload,
  allowedTypes = ['CIN_COPY', 'PASSPORT_COPY', 'CERTIFICAT_IMMATRICULATION', 'PATENTE', 'COMMERCIAL_REGISTER', 'TAX_RETURN', 'OTHER'],
  enableOcr = true,
}) => {
  const theme = useTheme();
  const [selectedType, setSelectedType] = useState<DocumentType>('CIN_COPY');
  const [uploadProgress, setUploadProgress] = useState<number>(0);
  const [phase, setPhase] = useState<'idle' | 'uploading' | 'processing' | 'done' | 'error'>('idle');
  const [uploadedFile, setUploadedFile] = useState<DocumentItem | null>(null);
  const [errorMessage, setErrorMessage] = useState<string>('');

  /** Returns the backend OCR type for this DocumentType, or null if OCR is not applicable. */
  const toOcrDocType = (dt: DocumentType) => OCR_DOC_TYPE_MAP[dt] ?? null;

  const handleFileSelect = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    // Reset state
    setErrorMessage('');
    setUploadProgress(0);
    setPhase('uploading');

    const doc: DocumentItem = {
      type: selectedType,
      filePath: `ocr://${Date.now()}_${file.name}`, // logical reference — not a real path
      fileName: file.name,
      fileSize: (file.size / 1024 / 1024).toFixed(2) + ' MB',
      uploadDate: new Date().toISOString(),
      verificationStatus: 'PENDING',
    };

    const ocrDocType = toOcrDocType(selectedType);
    const shouldRunOcr = enableOcr && ocrDocType !== null;

    if (!shouldRunOcr) {
      // Non-identity document: simulate upload progress, no OCR call
      simulateFakeProgress(() => {
        setUploadedFile(doc);
        setPhase('done');
        onFileUpload(doc, undefined);
      });
      return;
    }

    try {
      // Real upload + OCR
      setUploadProgress(0);
      const ocrResult = await ocrService.uploadAndExtract(
        file,
        ocrDocType as 'CIN' | 'PASSPORT' | 'CERTIFICAT_IMMATRICULATION',
        (pct) => {
          setUploadProgress(pct);
          if (pct === 100) setPhase('processing');
        }
      );

      setUploadedFile(doc);
      setPhase('done');
      onFileUpload(doc, ocrResult);
    } catch (err: any) {
      setPhase('error');
      setErrorMessage(
        err?.response?.data?.message ||
        'OCR processing failed. Please upload a clearer image (JPG/PNG, max 10 MB).'
      );
    }

    // Reset the input so the same file can be re-selected after an error
    event.target.value = '';
  };

  const handleRemove = () => {
    setUploadedFile(null);
    setPhase('idle');
    setUploadProgress(0);
    setErrorMessage('');
  };

  return (
    <Paper
      variant="outlined"
      sx={{
        p: 3,
        borderRadius: 3,
        borderStyle: 'dashed',
        borderWidth: 2,
        borderColor:
          phase === 'uploading' || phase === 'processing'
            ? theme.palette.primary.main
            : phase === 'error'
            ? theme.palette.error.main
            : theme.palette.divider,
        backgroundColor: theme.palette.background.default,
        transition: 'border-color 0.2s',
      }}
    >
      {phase === 'done' && uploadedFile ? (
        /* ── Uploaded state ── */
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <SuccessIcon color="success" fontSize="large" />
            <Box>
              <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                {uploadedFile.fileName}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {uploadedFile.type} • {uploadedFile.fileSize}
              </Typography>
            </Box>
          </Box>
          <IconButton color="error" onClick={handleRemove} aria-label="Remove uploaded file">
            <DeleteIcon />
          </IconButton>
        </Box>
      ) : (
        /* ── Upload / processing state ── */
        <Box sx={{ textAlign: 'center' }}>
          <FormControl size="small" sx={{ mb: 2, minWidth: 220 }}>
            <InputLabel id="doc-type-label">Document Type</InputLabel>
            <Select
              labelId="doc-type-label"
              value={selectedType}
              label="Document Type"
              onChange={(e) => setSelectedType(e.target.value as DocumentType)}
              disabled={phase === 'uploading' || phase === 'processing'}
            >
              {allowedTypes.map((type) => (
                <MenuItem key={type} value={type}>
                  {DOC_TYPE_LABELS[type] ?? type.replace(/_/g, ' ')}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <Box sx={{ mb: 2 }}>
            <UploadIcon sx={{ fontSize: 48, color: theme.palette.primary.main }} />
            <Typography variant="subtitle1" sx={{ fontWeight: 600, mt: 1 }}>
              {phase === 'processing' ? 'Analyse OCR en cours…' : 'Glissez ou parcourez votre document'}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {enableOcr && toOcrDocType(selectedType)
                ? 'JPG / PNG · max 10 Mo · extraction automatique des champs'
                : 'JPG, PNG · max 10 Mo'}
            </Typography>
          </Box>

          <Button
            variant="contained"
            component="label"
            disabled={phase === 'uploading' || phase === 'processing'}
            aria-label="Choose file to upload"
          >
            Choose File
            <input
              type="file"
              hidden
              accept={ACCEPTED_MIME}
              onChange={handleFileSelect}
            />
          </Button>

          {(phase === 'uploading' || phase === 'processing') && (
            <Box sx={{ width: '100%', mt: 3 }}>
              <LinearProgress
                variant={phase === 'processing' ? 'indeterminate' : 'determinate'}
                value={uploadProgress}
              />
              <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
                {phase === 'processing'
                  ? 'Tesseract analyse le document…'
                  : `Envoi en cours… ${uploadProgress}%`}
              </Typography>
            </Box>
          )}

          {phase === 'error' && errorMessage && (
            <Alert severity="error" sx={{ mt: 2, borderRadius: 2, textAlign: 'left' }}>
              {errorMessage}
            </Alert>
          )}
        </Box>
      )}
    </Paper>
  );
};

// ─── helpers ──────────────────────────────────────────────────────────────────

/** Fake progress animation for non-OCR uploads (no actual API call). */
function simulateFakeProgress(onComplete: () => void) {
  let progress = 0;
  const interval = setInterval(() => {
    progress += 25;
    if (progress >= 100) {
      clearInterval(interval);
      onComplete();
    }
  }, 200);
}

import React from 'react';
import {
  Card,
  CardContent,
  Typography,
  Box,
  Divider,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Alert,
  LinearProgress,
  Skeleton,
} from '@mui/material';
import {
  CheckCircle as MatchIcon,
  Error as MismatchIcon,
  HelpOutlined as UnknownIcon,
  Badge as CinIcon,
  MenuBook as PassportIcon,
  Business as CertificatIcon,
} from '@mui/icons-material';
import type { OcrVerificationResponse } from '../types';

// ── Types ──────────────────────────────────────────────────────────────────

interface UserEnteredData {
  cin?: string;
  passport?: string;
  firstName?: string;
  lastName?: string;
  birthDate?: string;
  address?: string;
  // Certificat d'immatriculation fields
  companyName?: string;
  businessType?: string;
  businessAddress?: string; // Siège social
  sector?: string;          // Secteur d'activité
  startDate?: string;       // Date de début d'activité (from Step 2)
}

interface OcrResultsViewerProps {
  /** Real OCR result from the backend. Null while loading. */
  ocrResult: OcrVerificationResponse | null;
  /** Whether the OCR call is currently in-flight. */
  loading?: boolean;
  /** User-entered form data to compare against extracted values. */
  userEnteredData?: UserEnteredData;
  /** Optional title override shown in the card header. */
  docTitle?: string;
}

interface FieldRow {
  label: string;
  extractedKey: string;
  userValue?: string;
}

// ── Document type helpers ──────────────────────────────────────────────────

function getDocumentMeta(docType: string): {
  icon: React.ReactNode;
  color: string;
  defaultTitle: string;
  fieldRows: (ud: UserEnteredData) => FieldRow[];
} {
  const upper = (docType ?? '').toUpperCase();

  // ── Passeport ──────────────────────────────────────────────────────────
  // Champs vérifiés : N° Passeport, Nom, Prénom
  // (Nom + Prénom doivent correspondre à ceux de la CIN)
  if (upper.includes('PASSPORT')) {
    return {
      icon: <PassportIcon fontSize="small" />,
      color: '#1565c0',
      defaultTitle: 'Passeport',
      fieldRows: (ud) => [
        { label: 'N° Passeport', extractedKey: 'passportNumber', userValue: ud.passport },
        { label: 'Nom',          extractedKey: 'lastName',       userValue: ud.lastName },
        { label: 'Prénom',       extractedKey: 'firstName',      userValue: ud.firstName },
      ],
    };
  }

  // ── Certificat d'immatriculation ──────────────────────────────────────
  // Champs vérifiés : Dénomination sociale, Forme juridique, Siège social,
  //                   Secteur d'activité, Date de début d'activité
  if (upper.includes('IMMATRICULATION') || upper === 'PATENTE' || upper === 'COMMERCIAL_REGISTER') {
    return {
      icon: <CertificatIcon fontSize="small" />,
      color: '#2e7d32',
      defaultTitle: 'Certificat d\'immatriculation',
      fieldRows: (ud) => [
        { label: 'Dénomination sociale',     extractedKey: 'companyName',      userValue: ud.companyName },
        { label: 'Forme juridique',           extractedKey: 'businessType',     userValue: ud.businessType },
        { label: 'Siège social',              extractedKey: 'address',          userValue: ud.businessAddress },
        { label: 'Secteur d\'activité',       extractedKey: 'sector',           userValue: ud.sector },
        { label: 'Date de début d\'activité', extractedKey: 'registrationDate', userValue: ud.startDate },
      ],
    };
  }

  // ── Carte d'Identité Nationale (CIN) — défaut ─────────────────────────
  // Champs vérifiés : N° CIN, Nom, Prénom, Adresse
  return {
    icon: <CinIcon fontSize="small" />,
    color: '#b71c1c',
    defaultTitle: 'Carte d\'Identité Nationale',
    fieldRows: (ud) => [
      { label: 'N° CIN', extractedKey: 'cinNumber', userValue: ud.cin },
      { label: 'Nom',    extractedKey: 'lastName',  userValue: ud.lastName },
      { label: 'Prénom', extractedKey: 'firstName', userValue: ud.firstName },
      { label: 'Adresse',extractedKey: 'address',   userValue: ud.address },
    ],
  };
}

// ── Component ──────────────────────────────────────────────────────────────

export const OcrResultsViewer: React.FC<OcrResultsViewerProps> = ({
  ocrResult,
  loading = false,
  userEnteredData = {},
  docTitle,
}) => {

  // ── Loading ────────────────────────────────────────────────────────────
  if (loading) {
    return (
      <Card variant="outlined" sx={{ borderRadius: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Box sx={{ flex: 1 }}>
              <Skeleton variant="text" width="55%" height={28} />
              <Skeleton variant="text" width="35%" height={18} />
            </Box>
            <Skeleton variant="rounded" width={140} height={32} />
          </Box>
          <Divider sx={{ mb: 2 }} />
          <LinearProgress />
          <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
            Tesseract analyse le document…
          </Typography>
        </CardContent>
      </Card>
    );
  }

  // ── No result ──────────────────────────────────────────────────────────
  if (!ocrResult) {
    return (
      <Card variant="outlined" sx={{ borderRadius: 3 }}>
        <CardContent>
          <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 1 }}>
            {docTitle ?? 'OCR — En attente du document'}
          </Typography>
          <Alert severity="info" sx={{ borderRadius: 2 }}>
            Téléchargez le document à l'étape 3 pour lancer l'extraction automatique.
          </Alert>
        </CardContent>
      </Card>
    );
  }

  // ── Resolved document metadata ─────────────────────────────────────────
  const meta       = getDocumentMeta(ocrResult.documentType);
  const title      = docTitle ?? meta.defaultTitle;
  const fieldRows  = meta.fieldRows(userEnteredData);
  const extracted  = ocrResult.extractedData ?? {};
  const confidence = ocrResult.confidenceScore ?? 0;
  const pct        = Math.round(confidence * 100);

  const overallMatch =
    ocrResult.verification === 'SUCCESS' ||
    (ocrResult.differences?.length === 0 && ocrResult.verification !== 'FAILED');

  const chipColor  = ocrResult.verification === 'SUCCESS' ? 'success'
                   : ocrResult.verification === 'PARTIAL'  ? 'warning'
                   : 'error';
  const chipLabel  = ocrResult.verification === 'SUCCESS' ? 'OCR RÉUSSI'
                   : ocrResult.verification === 'PARTIAL'  ? 'EXTRACTION PARTIELLE'
                   : 'INCOHÉRENCE';

  return (
    <Card variant="outlined" sx={{ borderRadius: 3, borderColor: meta.color + '40' }}>
      <CardContent>

        {/* ── Header ── */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start',
                   mb: 2, gap: 1, flexWrap: 'wrap' }}>
          <Box>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
              <Box sx={{ color: meta.color }}>{meta.icon}</Box>
              <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                {title}
              </Typography>
            </Box>
            <Typography variant="caption" color="text.secondary">
              Confiance OCR&nbsp;: {pct}%
            </Typography>
            <LinearProgress
              variant="determinate"
              value={pct}
              color={pct >= 70 ? 'success' : pct >= 40 ? 'warning' : 'error'}
              sx={{ mt: 0.5, height: 5, borderRadius: 3, minWidth: 140 }}
            />
          </Box>
          <Chip label={chipLabel} color={chipColor} size="small" sx={{ fontWeight: 700 }} />
        </Box>

        <Divider sx={{ mb: 1.5 }} />

        {/* ── Field table ── */}
        <TableContainer component={Paper} elevation={0} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow sx={{ bgcolor: 'action.hover' }}>
                <TableCell sx={{ fontWeight: 700, fontSize: '0.75rem' }}>Champ</TableCell>
                <TableCell sx={{ fontWeight: 700, fontSize: '0.75rem' }}>Valeur OCR</TableCell>
                <TableCell sx={{ fontWeight: 700, fontSize: '0.75rem' }}>Votre saisie</TableCell>
                <TableCell sx={{ fontWeight: 700, fontSize: '0.75rem', textAlign: 'center' }}>✓</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {fieldRows.map((row) => {
                const ocrVal  = extracted[row.extractedKey];
                const userVal = row.userValue;

                let matchIcon: React.ReactNode;
                if (!ocrVal) {
                  matchIcon = <UnknownIcon color="disabled" fontSize="small" />;
                } else if (!userVal) {
                  matchIcon = <UnknownIcon color="disabled" fontSize="small" />;
                } else if (ocrVal.toLowerCase() === userVal.toLowerCase()) {
                  matchIcon = <MatchIcon color="success" fontSize="small" />;
                } else {
                  matchIcon = <MismatchIcon color="error" fontSize="small" />;
                }

                // Skip rows where OCR extracted nothing AND there's no user value
                if (!ocrVal && !userVal) return null;

                return (
                  <TableRow key={row.extractedKey}>
                    <TableCell sx={{ fontWeight: 600, fontSize: '0.78rem', whiteSpace: 'nowrap' }}>
                      {row.label}
                    </TableCell>
                    <TableCell sx={{ color: ocrVal ? 'text.primary' : 'text.disabled', fontSize: '0.78rem' }}>
                      {ocrVal || '—'}
                    </TableCell>
                    <TableCell sx={{ color: userVal ? 'text.primary' : 'text.disabled', fontSize: '0.78rem' }}>
                      {userVal || '—'}
                    </TableCell>
                    <TableCell sx={{ textAlign: 'center' }}>{matchIcon}</TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>

        {/* ── Status message ── */}
        {ocrResult.statusMessage && (
          <Alert
            severity={overallMatch ? 'success' : ocrResult.verification === 'PARTIAL' ? 'warning' : 'error'}
            sx={{ mt: 2, borderRadius: 2, fontSize: '0.8rem' }}
          >
            {ocrResult.statusMessage}
          </Alert>
        )}

        {/* ── Differences ── */}
        {ocrResult.differences?.length > 0 && (
          <Alert severity="warning" sx={{ mt: 1.5, borderRadius: 2, fontSize: '0.8rem' }}>
            <Typography variant="body2" sx={{ fontWeight: 700, mb: 0.5 }}>
              Incohérences détectées — révision agent requise :
            </Typography>
            <ul style={{ margin: 0, paddingLeft: 18 }}>
              {ocrResult.differences.map((d, i) => (
                <li key={i}><Typography variant="body2">{d}</Typography></li>
              ))}
            </ul>
          </Alert>
        )}

      </CardContent>
    </Card>
  );
};

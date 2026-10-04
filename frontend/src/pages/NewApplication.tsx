import React, { useState } from 'react';
import {
  Box,
  Typography,
  Stepper,
  Step,
  StepLabel,
  Card,
  CardContent,
  Button,
  Grid,
  TextField,
  MenuItem,
  Alert,
  Divider,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  useTheme,
} from '@mui/material';
import {
  NavigateNext as NextIcon,
  NavigateBefore as PrevIcon,
  CheckCircle as SubmitIcon,
  CheckCircle as CheckIcon,
  Error as ErrorIcon,
  HelpOutlined as PendingIcon,
  VerifiedUser as BioIcon,
  Badge as CinIcon,
  MenuBook as PassportIcon,
  Business as CertificatIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { FileUploader } from '../components/FileUploader';
import { OcrResultsViewer } from '../components/OcrResultsViewer';
import { FaceVerificationModal } from '../components/FaceVerificationModal';
import { applicationService } from '../services/application.service';
import { compareNumber, compareText, compareDate } from '../utils/ocrCompare';
import type { DocumentItem, OcrVerificationResponse, CrossDocumentComparison, FieldComparison } from '../types';

// ── Step labels ────────────────────────────────────────────────────────────

const STEPS = [
  'Informations personnelles',
  'Activité commerciale',
  'Téléchargement des documents',
  'Vérification OCR',
  'Révision & Soumission',
];

// ── Required document slots ────────────────────────────────────────────────

interface DocSlot {
  key: 'cin' | 'passport' | 'certificat';
  type: 'CIN_COPY' | 'PASSPORT_COPY' | 'CERTIFICAT_IMMATRICULATION';
  label: string;
  icon: React.ReactNode;
  color: string;
  description: string;
}

const DOC_SLOTS: DocSlot[] = [
  {
    key: 'cin',
    type: 'CIN_COPY',
    label: 'Carte d\'Identité Nationale (CIN)',
    icon: <CinIcon />,
    color: '#b71c1c',
    description: 'Recto de la CIN tunisienne (JPG/PNG)',
  },
  {
    key: 'passport',
    type: 'PASSPORT_COPY',
    label: 'Passeport',
    icon: <PassportIcon />,
    color: '#1565c0',
    description: 'Page d\'identité du passeport (JPG/PNG)',
  },
  {
    key: 'certificat',
    type: 'CERTIFICAT_IMMATRICULATION',
    label: 'Certificat d\'immatriculation',
    icon: <CertificatIcon />,
    color: '#2e7d32',
    description: 'Certificat d\'immatriculation au registre du commerce (JPG/PNG)',
  },
];

// ── Cross-document comparison helper ──────────────────────────────────────
// The values entered in Step 1 (Informations personnelles) and Step 2
// (Activité commerciale) are the REFERENCE ("Valeur saisie"). Every OCR
// field extracted from the 3 uploaded documents is checked against that
// reference. Nom/Prénom on the passport are additionally cross-checked
// against the CIN OCR for identity consistency.
//
//   CIN          → N° CIN, Nom, Prénom, Adresse            vs saisie
//   Passeport    → N° Passeport, Nom, Prénom                vs saisie
//                  + Nom/Prénom OCR Passeport vs OCR CIN (cohérence identité)
//   Certificat   → Dénomination sociale, Forme juridique,
//                  Siège social, Secteur d'activité,
//                  Date de début d'activité                vs saisie

interface CrossComparisonFormData {
  firstName: string;
  lastName: string;
  cin: string;
  passport: string;
  businessName: string;
  businessType: string;
  businessAddress: string;
  sector: string;
  startDate: string;
}

function pushField(
  fields: FieldComparison[],
  label: string,
  sourceALabel: string,
  sourceA: string | undefined,
  sourceBLabel: string,
  sourceB: string | undefined,
  status: FieldComparison['status'],
) {
  fields.push({ label, sourceA, sourceALabel, sourceB, sourceBLabel, status });
}

function buildCrossComparison(
  ocrCin: OcrVerificationResponse | null,
  ocrPassport: OcrVerificationResponse | null,
  ocrCertificat: OcrVerificationResponse | null,
  formData: CrossComparisonFormData,
): CrossDocumentComparison {
  const fields: FieldComparison[] = [];
  const SAISIE = 'Saisie formulaire';

  const cinExt       = ocrCin?.extractedData;
  const passportExt  = ocrPassport?.extractedData;
  const certExt      = ocrCertificat?.extractedData;

  // ── CIN vs saisie ──────────────────────────────────────────────────────
  const cinNumberCmp = compareNumber(formData.cin, cinExt?.cinNumber);
  pushField(fields, 'N° CIN', SAISIE, formData.cin, 'OCR CIN', cinExt?.cinNumber, cinNumberCmp.status);

  const cinNomCmp = compareText(formData.lastName, cinExt?.lastName);
  pushField(fields, 'Nom (CIN)', SAISIE, formData.lastName, 'OCR CIN', cinExt?.lastName, cinNomCmp.status);

  const cinPrenomCmp = compareText(formData.firstName, cinExt?.firstName);
  pushField(fields, 'Prénom (CIN)', SAISIE, formData.firstName, 'OCR CIN', cinExt?.firstName, cinPrenomCmp.status);

  // ── Passeport vs saisie ────────────────────────────────────────────────
  const passportNumberCmp = compareNumber(formData.passport, passportExt?.passportNumber);
  pushField(fields, 'N° Passeport', SAISIE, formData.passport, 'OCR Passeport', passportExt?.passportNumber, passportNumberCmp.status);

  const passportNomCmp = compareText(formData.lastName, passportExt?.lastName);
  pushField(fields, 'Nom (Passeport)', SAISIE, formData.lastName, 'OCR Passeport', passportExt?.lastName, passportNomCmp.status);

  const passportPrenomCmp = compareText(formData.firstName, passportExt?.firstName);
  pushField(fields, 'Prénom (Passeport)', SAISIE, formData.firstName, 'OCR Passeport', passportExt?.firstName, passportPrenomCmp.status);

  // ── Cohérence d'identité : Nom/Prénom OCR CIN vs OCR Passeport ─────────
  const identityNomCmp = compareText(cinExt?.lastName, passportExt?.lastName);
  pushField(fields, 'Cohérence Nom (CIN ↔ Passeport)', 'OCR CIN', cinExt?.lastName, 'OCR Passeport', passportExt?.lastName, identityNomCmp.status);

  const identityPrenomCmp = compareText(cinExt?.firstName, passportExt?.firstName);
  pushField(fields, 'Cohérence Prénom (CIN ↔ Passeport)', 'OCR CIN', cinExt?.firstName, 'OCR Passeport', passportExt?.firstName, identityPrenomCmp.status);

  // ── Certificat / Activité commerciale vs saisie ────────────────────────
  const denomCmp = compareText(formData.businessName, certExt?.companyName);
  pushField(fields, 'Dénomination sociale', SAISIE, formData.businessName, 'OCR Certificat', certExt?.companyName, denomCmp.status);

  const formeCmp = compareText(formData.businessType, certExt?.businessType);
  pushField(fields, 'Forme juridique', SAISIE, formData.businessType, 'OCR Certificat', certExt?.businessType, formeCmp.status);

  const siegeCmp = compareText(formData.businessAddress, certExt?.address, 70);
  pushField(fields, 'Siège social', SAISIE, formData.businessAddress, 'OCR Certificat', certExt?.address, siegeCmp.status);

  const secteurCmp = compareText(formData.sector, certExt?.sector);
  pushField(fields, 'Secteur d\'activité', SAISIE, formData.sector, 'OCR Certificat', certExt?.sector, secteurCmp.status);

  const dateCmp = compareDate(formData.startDate, certExt?.registrationDate);
  pushField(fields, 'Date de début d\'activité', SAISIE, formData.startDate, 'OCR Certificat', certExt?.registrationDate, dateCmp.status);

  const allMatch = fields.every(f => f.status === 'MATCH');
  return { fields, allMatch };
}

// ── Status chip helper ─────────────────────────────────────────────────────

function ComparisonStatusChip({ status }: { status: string }) {
  const props = {
    MATCH:     { label: 'MATCH',     color: 'success' as const, icon: <CheckIcon fontSize="small" /> },
    MISMATCH:  { label: 'MISMATCH',  color: 'error'   as const, icon: <ErrorIcon fontSize="small" /> },
    NOT_FOUND: { label: 'NOT_FOUND', color: 'warning' as const, icon: <PendingIcon fontSize="small" /> },
  }[status] ?? { label: status, color: 'default' as const, icon: null };

  return <Chip label={props.label} color={props.color} size="small" icon={props.icon} sx={{ fontWeight: 700 }} />;
}

// =========================================================================
// Component
// =========================================================================

export const NewApplication: React.FC = () => {
  const theme  = useTheme();
  const navigate = useNavigate();
  const [activeStep, setActiveStep] = useState(0);
  const [error, setError] = useState('');

  // ── Form data ────────────────────────────────────────────────────────────
  const [formData, setFormData] = useState({
    firstName: '', lastName: '',
    cin: '', passport: '',
    phone: '', address: '',
    businessName: '', businessType: 'SARL',
    sector: '', businessAddress: '', startDate: '',
  });

  // ── Document state — one slot per required document ──────────────────────
  const [docItems, setDocItems] = useState<Record<string, DocumentItem | null>>({
    cin: null, passport: null, certificat: null,
  });

  // ── OCR results — one per document ───────────────────────────────────────
  const [ocrResults, setOcrResults] = useState<Record<string, OcrVerificationResponse | null>>({
    cin: null, passport: null, certificat: null,
  });

  const [ocrLoading, setOcrLoading] = useState<Record<string, boolean>>({
    cin: false, passport: false, certificat: false,
  });

  // ── Other state ───────────────────────────────────────────────────────────
  const [faceVerified, setFaceVerified]   = useState(false);
  const [faceModalOpen, setFaceModalOpen] = useState(false);
  const [loading, setLoading]             = useState(false);

  // ── Computed: all 3 docs uploaded ─────────────────────────────────────────
  const allDocsUploaded =
    docItems.cin !== null &&
    docItems.passport !== null &&
    docItems.certificat !== null;

  // ── Cross-document comparison (computed from OCR results + form) ──────────
  const crossComparison: CrossDocumentComparison = buildCrossComparison(
    ocrResults.cin,
    ocrResults.passport,
    ocrResults.certificat,
    formData,
  );

  // ── Handlers ─────────────────────────────────────────────────────────────

  const handleNext = () => {
    if (activeStep === 2 && !allDocsUploaded) {
      const missing = DOC_SLOTS
        .filter(s => !docItems[s.key])
        .map(s => s.label)
        .join(', ');
      setError(`Documents obligatoires manquants : ${missing}`);
      return;
    }
    setError('');
    setActiveStep(p => p + 1);
  };

  const handleBack = () => {
    setError('');
    setActiveStep(p => p - 1);
  };

  const handleFileUpload =
    (slotKey: string) =>
    (doc: DocumentItem, result?: OcrVerificationResponse) => {
      setDocItems(prev  => ({ ...prev, [slotKey]: doc }));
      setOcrResults(prev => ({ ...prev, [slotKey]: result ?? null }));

      // Auto-fill form from OCR extracted data
      if (result?.extractedData) {
        const ext = result.extractedData;
        setFormData(prev => ({
          ...prev,
          firstName:  ext.firstName        || prev.firstName,
          lastName:   ext.lastName         || prev.lastName,
          cin:        ext.cinNumber        || prev.cin,
          passport:   ext.passportNumber   || prev.passport,
          address:    ext.address          || prev.address,
          // Certificat — pre-fill business info
          businessName: ext.companyName    || prev.businessName,
          businessType: ext.businessType   || prev.businessType,
          startDate:    ext.registrationDate ? toInputDate(ext.registrationDate) : prev.startDate,
        }));
      }
    };

  const handleSubmit = async () => {
    setLoading(true);
    setError('');
    try {
      await applicationService.createApplication({
        activities: [{
          businessName: formData.businessName,
          businessType: formData.businessType,
          sector:       formData.sector,
          address:      formData.businessAddress,
          startDate:    formData.startDate,
        }],
        documents: Object.values(docItems)
          .filter(Boolean)
          .map(d => ({ type: d!.type, filePath: d!.filePath })),
      });
      navigate('/applications');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Échec de la soumission. Vérifiez vos données.');
    } finally {
      setLoading(false);
    }
  };

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <Box sx={{ maxWidth: '1200px', mx: 'auto' }}>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 1 }}>
        Assistant d'immatriculation fiscale en ligne
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Déposez votre dossier d'immatriculation à la Direction Générale des Impôts en 5 étapes
      </Typography>

      <Stepper activeStep={activeStep} alternativeLabel sx={{ mb: 4 }}>
        {STEPS.map(label => (
          <Step key={label}><StepLabel>{label}</StepLabel></Step>
        ))}
      </Stepper>

      {error && (
        <Alert severity="error" sx={{ mb: 3, borderRadius: 2 }}>{error}</Alert>
      )}

      <Card variant="outlined" sx={{ borderRadius: 3, p: { xs: 2, sm: 3 } }}>
        <CardContent>

          {/* ════════════════════════════════════════════
              STEP 1 — Personal Information
          ════════════════════════════════════════════ */}
          {activeStep === 0 && (
            <Box>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Étape 1 : Informations personnelles
              </Typography>
              <Grid container spacing={2}>
                {[
                  { label: 'Prénom *',           field: 'firstName' },
                  { label: 'Nom *',              field: 'lastName' },
                  { label: 'N° CIN *',           field: 'cin' },
                  { label: 'N° Passeport',       field: 'passport' },
                  { label: 'Téléphone',          field: 'phone' },
                  { label: 'Adresse domicile',   field: 'address' },
                ].map(({ label, field }) => (
                  <Grid key={field} size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth label={label}
                      value={(formData as any)[field]}
                      onChange={e => setFormData({ ...formData, [field]: e.target.value })}
                    />
                  </Grid>
                ))}
              </Grid>
            </Box>
          )}

          {/* ════════════════════════════════════════════
              STEP 2 — Business Details
          ════════════════════════════════════════════ */}
          {activeStep === 1 && (
            <Box>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Étape 2 : Activité commerciale
              </Typography>
              <Grid container spacing={2}>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField fullWidth label="Dénomination sociale *"
                    value={formData.businessName}
                    onChange={e => setFormData({ ...formData, businessName: e.target.value })} />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField fullWidth select label="Forme juridique *"
                    value={formData.businessType}
                    onChange={e => setFormData({ ...formData, businessType: e.target.value })}>
                    <MenuItem value="SARL">SARL</MenuItem>
                    <MenuItem value="SUARL">SUARL</MenuItem>
                    <MenuItem value="SA">SA</MenuItem>
                    <MenuItem value="PATENTE_PERSONNE_PHYSIQUE">Personne Physique</MenuItem>
                  </TextField>
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField fullWidth label="Secteur d'activité"
                    value={formData.sector}
                    onChange={e => setFormData({ ...formData, sector: e.target.value })} />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField fullWidth type="date"
                    label="Date de début d'activité *"
                    slotProps={{ inputLabel: { shrink: true } }}
                    value={formData.startDate}
                    onChange={e => setFormData({ ...formData, startDate: e.target.value })} />
                </Grid>
                <Grid size={{ xs: 12 }}>
                  <TextField fullWidth label="Adresse du siège social"
                    value={formData.businessAddress}
                    onChange={e => setFormData({ ...formData, businessAddress: e.target.value })} />
                </Grid>
              </Grid>
            </Box>
          )}

          {/* ════════════════════════════════════════════
              STEP 3 — Document Upload (3 mandatory)
          ════════════════════════════════════════════ */}
          {activeStep === 2 && (
            <Box>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>
                Étape 3 : Téléchargement des documents obligatoires
              </Typography>
              <Alert severity="info" sx={{ mb: 3, borderRadius: 2 }}>
                Les <strong>3 documents suivants sont obligatoires</strong>. L'OCR sera lancé
                automatiquement sur chaque document pour en extraire les informations.
              </Alert>

              <Grid container spacing={3}>
                {DOC_SLOTS.map(slot => {
                  const uploaded = docItems[slot.key];
                  const ocr      = ocrResults[slot.key];

                  return (
                    <Grid key={slot.key} size={{ xs: 12 }}>
                      <Card
                        variant="outlined"
                        sx={{
                          borderRadius: 3,
                          borderColor: uploaded
                            ? slot.color + '80'
                            : theme.palette.divider,
                          borderWidth: uploaded ? 2 : 1,
                          transition: 'border-color 0.2s',
                        }}
                      >
                        <CardContent sx={{ pb: '16px !important' }}>
                          {/* Slot header */}
                          <Box sx={{ display: 'flex', alignItems: 'center',
                                     justifyContent: 'space-between', mb: 2 }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                              <Box sx={{ color: slot.color }}>{slot.icon}</Box>
                              <Box>
                                <Typography variant="subtitle1" sx={{ fontWeight: 700, lineHeight: 1.2 }}>
                                  {slot.label}
                                </Typography>
                                <Typography variant="caption" color="text.secondary">
                                  {slot.description}
                                </Typography>
                              </Box>
                            </Box>
                            {uploaded
                              ? <Chip icon={<CheckIcon />} label="Téléchargé" color="success" size="small" sx={{ fontWeight: 700 }} />
                              : <Chip label="Obligatoire" color="error" variant="outlined" size="small" sx={{ fontWeight: 700 }} />
                            }
                          </Box>

                          {/* FileUploader — locked to this slot's type */}
                          <FileUploader
                            allowedTypes={[slot.type]}
                            enableOcr
                            onFileUpload={handleFileUpload(slot.key)}
                          />

                          {/* Inline OCR status */}
                          {ocr && (
                            <Alert
                              severity={ocr.verification === 'SUCCESS' ? 'success'
                                       : ocr.verification === 'PARTIAL' ? 'warning' : 'error'}
                              sx={{ mt: 2, borderRadius: 2, fontSize: '0.8rem' }}
                            >
                              {ocr.statusMessage}
                            </Alert>
                          )}
                        </CardContent>
                      </Card>
                    </Grid>
                  );
                })}
              </Grid>

              {/* Progress summary */}
              <Box sx={{ mt: 3, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                {DOC_SLOTS.map(slot => (
                  <Chip
                    key={slot.key}
                    icon={docItems[slot.key] ? <CheckIcon /> : <PendingIcon />}
                    label={slot.label.split('(')[0].trim()}
                    color={docItems[slot.key] ? 'success' : 'default'}
                    variant={docItems[slot.key] ? 'filled' : 'outlined'}
                    size="small"
                  />
                ))}
              </Box>

              {!allDocsUploaded && (
                <Alert severity="warning" sx={{ mt: 2, borderRadius: 2 }}>
                  {DOC_SLOTS.filter(s => !docItems[s.key]).length} document(s) manquant(s). 
                  Vous ne pouvez pas passer à l'étape suivante.
                </Alert>
              )}
            </Box>
          )}

          {/* ════════════════════════════════════════════
              STEP 4 — OCR Verification + Cross-comparison
          ════════════════════════════════════════════ */}
          {activeStep === 3 && (
            <Box>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>
                Étape 4 : Vérification OCR & Cohérence des documents
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                Résultats de l'extraction OCR sur vos 3 documents et comparaison croisée.
              </Typography>

              {/* ── 3 OCR result cards ── */}
              <Grid container spacing={2} sx={{ mb: 3 }}>
                {DOC_SLOTS.map(slot => (
                  <Grid key={slot.key} size={{ xs: 12, lg: 4 }}>
                    <OcrResultsViewer
                      docTitle={slot.label}
                      ocrResult={ocrResults[slot.key]}
                      loading={ocrLoading[slot.key]}
                      userEnteredData={{
                        cin:             formData.cin,
                        passport:        formData.passport,
                        firstName:       formData.firstName,
                        lastName:        formData.lastName,
                        address:         formData.address,
                        businessType:    formData.businessType,
                        startDate:       formData.startDate,
                        companyName:     formData.businessName,
                      }}
                    />
                  </Grid>
                ))}
              </Grid>

              <Divider sx={{ my: 3 }} />

              {/* ── Cross-document comparison panel ── */}
              <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 2 }}>
                Comparaison croisée des documents
              </Typography>

              <Card variant="outlined" sx={{ borderRadius: 3, mb: 3 }}>
                <CardContent>
                  <TableContainer component={Paper} elevation={0} variant="outlined">
                    <Table size="small">
                      <TableHead>
                        <TableRow sx={{ bgcolor: 'action.hover' }}>
                          <TableCell sx={{ fontWeight: 700 }}>Champ comparé</TableCell>
                          <TableCell sx={{ fontWeight: 700 }}>Source A</TableCell>
                          <TableCell sx={{ fontWeight: 700 }}>Valeur A</TableCell>
                          <TableCell sx={{ fontWeight: 700 }}>Source B</TableCell>
                          <TableCell sx={{ fontWeight: 700 }}>Valeur B</TableCell>
                          <TableCell sx={{ fontWeight: 700, textAlign: 'center' }}>Statut</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {crossComparison.fields.map((f, i) => (
                          <TableRow key={i}>
                            <TableCell sx={{ fontWeight: 600 }}>{f.label}</TableCell>
                            <TableCell>
                              <Typography variant="caption" color="text.secondary">
                                {f.sourceALabel}
                              </Typography>
                            </TableCell>
                            <TableCell sx={{
                              color: f.status === 'MISMATCH' ? 'error.main' : 'text.primary',
                              fontWeight: f.status === 'MISMATCH' ? 700 : 400,
                            }}>
                              {f.sourceA || '—'}
                            </TableCell>
                            <TableCell>
                              <Typography variant="caption" color="text.secondary">
                                {f.sourceBLabel}
                              </Typography>
                            </TableCell>
                            <TableCell sx={{
                              color: f.status === 'MISMATCH' ? 'error.main' : 'text.primary',
                              fontWeight: f.status === 'MISMATCH' ? 700 : 400,
                            }}>
                              {f.sourceB || '—'}
                            </TableCell>
                            <TableCell sx={{ textAlign: 'center' }}>
                              <ComparisonStatusChip status={f.status} />
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>

                  {/* Global verdict */}
                  <Alert
                    severity={crossComparison.allMatch ? 'success'
                              : crossComparison.fields.some(f => f.status === 'MISMATCH')
                                ? 'error' : 'warning'}
                    sx={{ mt: 2, borderRadius: 2 }}
                  >
                    {crossComparison.allMatch
                      ? 'Tous les champs comparés sont cohérents entre vos documents.'
                      : crossComparison.fields.some(f => f.status === 'MISMATCH')
                        ? 'Des incohérences ont été détectées. Un agent DGI devra valider votre dossier.'
                        : 'Certains champs n\'ont pas pu être comparés (données manquantes dans l\'OCR).'}
                  </Alert>
                </CardContent>
              </Card>

              {/* ── Biometric check ── */}
              <Card variant="outlined" sx={{ borderRadius: 3, p: 2, textAlign: 'center' }}>
                <BioIcon sx={{ fontSize: 48, color: faceVerified ? 'success.main' : 'primary.main', mb: 1 }} />
                <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                  Vérification biométrique (optionnel)
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                  Comparez votre selfie en direct avec la photo de votre CIN.
                </Typography>
                <Button
                  variant={faceVerified ? 'outlined' : 'contained'}
                  color={faceVerified ? 'success' : 'primary'}
                  onClick={() => setFaceModalOpen(true)}
                >
                  {faceVerified ? 'Identité vérifiée ✓' : 'Lancer la vérification faciale'}
                </Button>
              </Card>

              <FaceVerificationModal
                open={faceModalOpen}
                onClose={() => setFaceModalOpen(false)}
                onVerified={v => setFaceVerified(v)}
              />
            </Box>
          )}

          {/* ════════════════════════════════════════════
              STEP 5 — Review & Submit
          ════════════════════════════════════════════ */}
          {activeStep === 4 && (
            <Box>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Étape 5 : Révision & Soumission
              </Typography>
              <Divider sx={{ mb: 3 }} />

              <Grid container spacing={3}>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
                    Contribuable
                  </Typography>
                  <Typography variant="body1" sx={{ fontWeight: 600 }}>
                    {formData.firstName} {formData.lastName}
                  </Typography>
                  <Typography variant="body2">CIN : {formData.cin || '—'}</Typography>
                  <Typography variant="body2">Passeport : {formData.passport || '—'}</Typography>
                  <Typography variant="body2">Téléphone : {formData.phone || '—'}</Typography>
                </Grid>

                <Grid size={{ xs: 12, sm: 6 }}>
                  <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
                    Entreprise
                  </Typography>
                  <Typography variant="body1" sx={{ fontWeight: 600 }}>
                    {formData.businessName || '—'} ({formData.businessType})
                  </Typography>
                  <Typography variant="body2">Secteur : {formData.sector || '—'}</Typography>
                  <Typography variant="body2">
                    Date de début : {formData.startDate || '—'}
                  </Typography>
                  <Typography variant="body2">Adresse : {formData.businessAddress || '—'}</Typography>
                </Grid>

                <Grid size={{ xs: 12 }}>
                  <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
                    Documents téléchargés
                  </Typography>
                  <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                    {DOC_SLOTS.map(slot => (
                      <Chip
                        key={slot.key}
                        icon={<CheckIcon />}
                        label={slot.label.split('(')[0].trim()}
                        color="success"
                        size="small"
                      />
                    ))}
                  </Box>
                </Grid>

                {crossComparison.fields.some(f => f.status === 'MISMATCH') && (
                  <Grid size={{ xs: 12 }}>
                    <Alert severity="warning" sx={{ borderRadius: 2 }}>
                      Des incohérences ont été détectées lors de la comparaison croisée des documents.
                      Votre dossier sera soumis à la révision d'un agent DGI avant approbation.
                    </Alert>
                  </Grid>
                )}
              </Grid>

              <Divider sx={{ my: 3 }} />
              <Alert severity="info" sx={{ borderRadius: 2 }}>
                Après soumission, votre dossier sera transmis à la file de révision des agents DGI.
                Vous recevrez des notifications en temps réel et votre matricule fiscal officiel
                dès approbation.
              </Alert>
            </Box>
          )}

          {/* ── Navigation ── */}
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 4, pt: 2,
                     borderTop: `1px solid ${theme.palette.divider}` }}>
            <Button disabled={activeStep === 0} onClick={handleBack} startIcon={<PrevIcon />}>
              Retour
            </Button>
            {activeStep < STEPS.length - 1 ? (
              <Button
                variant="contained"
                onClick={handleNext}
                endIcon={<NextIcon />}
                disabled={activeStep === 2 && !allDocsUploaded}
              >
                Étape suivante
              </Button>
            ) : (
              <Button
                variant="contained" color="success"
                onClick={handleSubmit}
                disabled={loading}
                startIcon={<SubmitIcon />}
                sx={{ px: 4, fontWeight: 700 }}
              >
                {loading ? 'Soumission…' : 'Soumettre le dossier'}
              </Button>
            )}
          </Box>

        </CardContent>
      </Card>
    </Box>
  );
};

// ── Helpers ──────────────────────────────────────────────────────────────────

/** Convert OCR date string (DD/MM/YYYY) to HTML date input format (YYYY-MM-DD) */
function toInputDate(raw: string): string {
  if (!raw) return '';
  const m = raw.match(/^(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{4})$/);
  if (m) return `${m[3]}-${m[2].padStart(2,'0')}-${m[1].padStart(2,'0')}`;
  return raw;
}
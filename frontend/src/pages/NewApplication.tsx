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
  List,
  ListItem,
  ListItemText,
  useTheme,
} from '@mui/material';
import {
  NavigateNext as NextIcon,
  NavigateBefore as PrevIcon,
  CheckCircle as SubmitIcon,
  VerifiedUser as BioIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { FileUploader } from '../components/FileUploader';
import { OcrResultsViewer } from '../components/OcrResultsViewer';
import { FaceVerificationModal } from '../components/FaceVerificationModal';
import { applicationService } from '../services/application.service';
import type { DocumentItem } from '../types';

const steps = [
  'Personal Information',
  'Business Details',
  'Document Upload',
  'Biometric & OCR Check',
  'Review & Submit',
];

export const NewApplication: React.FC = () => {
  const theme = useTheme();
  const navigate = useNavigate();
  const [activeStep, setActiveStep] = useState(0);

  const [formData, setFormData] = useState({
    firstName: 'Mohamed',
    lastName: 'Ben Ali',
    cin: '08923415',
    passport: '',
    phone: '+216 98 123 456',
    address: 'Avenue Habib Bourguiba, Tunis',

    businessName: 'DGI Tech Solutions SARL',
    businessType: 'SARL',
    sector: 'Technologies de l\'Information',
    businessAddress: 'Cyberparc El Ghazala, Ariana',
    startDate: '2026-08-01',
  });

  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [faceVerified, setFaceVerified] = useState(false);
  const [faceModalOpen, setFaceModalOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleNext = () => {
    if (activeStep === 2 && documents.length === 0) {
      setError('Please upload at least one mandatory document (CIN or Commercial Register).');
      return;
    }
    setError('');
    setActiveStep((prev) => prev + 1);
  };

  const handleBack = () => {
    setError('');
    setActiveStep((prev) => prev - 1);
  };

  const handleFileUpload = (doc: DocumentItem) => {
    setDocuments((prev) => [...prev, doc]);
  };

  const handleSubmit = async () => {
    setLoading(true);
    setError('');

    try {
      await applicationService.createApplication({
        activities: [
          {
            businessName: formData.businessName,
            businessType: formData.businessType,
            sector: formData.sector,
            address: formData.businessAddress,
            startDate: formData.startDate,
          },
        ],
        documents: documents.map((d) => ({
          type: d.type,
          filePath: d.filePath,
        })),
      });

      navigate('/applications');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Submission failed. Please check your data and retry.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ maxWidth: '1200px', mx: 'auto' }}>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 1 }}>
        Online Taxpayer Registration Wizard
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Complete the 5-step digital immatriculation request for the Direction Générale des Impôts
      </Typography>

      <Stepper activeStep={activeStep} alternativeLabel sx={{ mb: 4 }}>
        {steps.map((label) => (
          <Step key={label}>
            <StepLabel>{label}</StepLabel>
          </Step>
        ))}
      </Stepper>

      {error && (
        <Alert severity="error" sx={{ mb: 3, borderRadius: 2 }}>
          {error}
        </Alert>
      )}

      <Card variant="outlined" sx={{ borderRadius: 3, p: { xs: 2, sm: 3 } }}>
        <CardContent>
          {/* Step 1: Personal Info */}
          {activeStep === 0 && (
            <Box component="form">
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Step 1: Personal & Identity Information
              </Typography>
              <Grid container spacing={2}>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    label="First Name *"
                    value={formData.firstName}
                    onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    label="Last Name *"
                    value={formData.lastName}
                    onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    label="CIN Card Number *"
                    value={formData.cin}
                    onChange={(e) => setFormData({ ...formData, cin: e.target.value })}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    label="Passport Number"
                    value={formData.passport}
                    onChange={(e) => setFormData({ ...formData, passport: e.target.value })}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    label="Phone Number"
                    value={formData.phone}
                    onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    label="Home Address"
                    value={formData.address}
                    onChange={(e) => setFormData({ ...formData, address: e.target.value })}
                  />
                </Grid>
              </Grid>
            </Box>
          )}

          {/* Step 2: Business Info */}
          {activeStep === 1 && (
            <Box component="form">
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Step 2: Business & Activity Details
              </Typography>
              <Grid container spacing={2}>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    label="Business / Corporate Name *"
                    value={formData.businessName}
                    onChange={(e) => setFormData({ ...formData, businessName: e.target.value })}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    select
                    label="Legal Structure *"
                    value={formData.businessType}
                    onChange={(e) => setFormData({ ...formData, businessType: e.target.value })}
                  >
                    <MenuItem value="SARL">SARL (Société à Responsabilité Limitée)</MenuItem>
                    <MenuItem value="SUARL">SUARL (Société Unipersonnelle)</MenuItem>
                    <MenuItem value="SA">SA (Société Anonyme)</MenuItem>
                    <MenuItem value="PATENTE_PERSONNE_PHYSIQUE">Personne Physique</MenuItem>
                  </TextField>
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    label="Economic Sector"
                    value={formData.sector}
                    onChange={(e) => setFormData({ ...formData, sector: e.target.value })}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    type="date"
                    label="Activity Start Date"
                    slotProps={{ inputLabel: { shrink: true } }}
                    value={formData.startDate}
                    onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                  />
                </Grid>
                <Grid size={{ xs: 12 }}>
                  <TextField
                    fullWidth
                    label="Business Headquarter Address"
                    value={formData.businessAddress}
                    onChange={(e) => setFormData({ ...formData, businessAddress: e.target.value })}
                  />
                </Grid>
              </Grid>
            </Box>
          )}

          {/* Step 3: Document Upload */}
          {activeStep === 2 && (
            <Box>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Step 3: Document Upload & Verification
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                Please attach scanned copies of your CIN identity card and business registration.
              </Typography>

              <FileUploader onFileUpload={handleFileUpload} />

              {documents.length > 0 && (
                <Box sx={{ mt: 3 }}>
                  <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                    Uploaded Documents ({documents.length})
                  </Typography>
                  <List disablePadding>
                    {documents.map((d, i) => (
                      <ListItem key={i} divider>
                        <ListItemText
                          primary={d.fileName}
                          secondary={`Type: ${d.type} • Uploaded: ${new Date(d.uploadDate!).toLocaleTimeString()}`}
                        />
                      </ListItem>
                    ))}
                  </List>
                </Box>
              )}
            </Box>
          )}

          {/* Step 4: Biometric & OCR Check */}
          {activeStep === 3 && (
            <Box>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Step 4: AI Biometric & OCR Document Audit
              </Typography>

              <Grid container spacing={3}>
                <Grid size={{ xs: 12, md: 7 }}>
                  <OcrResultsViewer
                    userEnteredData={{
                      cin: formData.cin,
                      fullName: `${formData.firstName} ${formData.lastName}`,
                      address: formData.address,
                    }}
                  />
                </Grid>
                <Grid size={{ xs: 12, md: 5 }}>
                  <Card variant="outlined" sx={{ borderRadius: 3, p: 2, textAlign: 'center' }}>
                    <BioIcon sx={{ fontSize: 56, color: faceVerified ? 'success.main' : 'primary.main', mb: 1 }} />
                    <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                      Biometric Face Check
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                      Match live webcam selfie against your uploaded CIN identity document photo.
                    </Typography>

                    <Button
                      variant={faceVerified ? 'outlined' : 'contained'}
                      color={faceVerified ? 'success' : 'primary'}
                      onClick={() => setFaceModalOpen(true)}
                    >
                      {faceVerified ? 'Identity Verified ✓' : 'Run Face Verification'}
                    </Button>
                  </Card>
                </Grid>
              </Grid>

              <FaceVerificationModal
                open={faceModalOpen}
                onClose={() => setFaceModalOpen(false)}
                onVerified={(v) => setFaceVerified(v)}
              />
            </Box>
          )}

          {/* Step 5: Review & Submit */}
          {activeStep === 4 && (
            <Box>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Step 5: Review & Submit Application
              </Typography>
              <Divider sx={{ mb: 3 }} />

              <Grid container spacing={3}>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <Typography variant="subtitle2" color="text.secondary">
                    Taxpayer Information
                  </Typography>
                  <Typography variant="body1" sx={{ fontWeight: 600 }}>
                    {formData.firstName} {formData.lastName}
                  </Typography>
                  <Typography variant="body2">CIN: {formData.cin}</Typography>
                  <Typography variant="body2">Phone: {formData.phone}</Typography>
                </Grid>

                <Grid size={{ xs: 12, sm: 6 }}>
                  <Typography variant="subtitle2" color="text.secondary">
                    Business Enterprise
                  </Typography>
                  <Typography variant="body1" sx={{ fontWeight: 600 }}>
                    {formData.businessName} ({formData.businessType})
                  </Typography>
                  <Typography variant="body2">Sector: {formData.sector}</Typography>
                  <Typography variant="body2">Address: {formData.businessAddress}</Typography>
                </Grid>
              </Grid>

              <Divider sx={{ my: 3 }} />

              <Alert severity="info" sx={{ borderRadius: 2 }}>
                Upon submission, your application will be dispatched to the DGI Tax Agent review queue. You will receive real-time notifications and your official Fiscal Number upon approval.
              </Alert>
            </Box>
          )}

          {/* Stepper Navigation Buttons */}
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 4, pt: 2, borderTop: `1px solid ${theme.palette.divider}` }}>
            <Button disabled={activeStep === 0} onClick={handleBack} startIcon={<PrevIcon />}>
              Back
            </Button>
            {activeStep < steps.length - 1 ? (
              <Button variant="contained" onClick={handleNext} endIcon={<NextIcon />}>
                Next Step
              </Button>
            ) : (
              <Button
                variant="contained"
                color="success"
                onClick={handleSubmit}
                disabled={loading}
                startIcon={<SubmitIcon />}
                sx={{ px: 4, fontWeight: 700 }}
              >
                {loading ? 'Submitting...' : 'Submit Application'}
              </Button>
            )}
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
};

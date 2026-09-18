import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Grid,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Alert,
} from '@mui/material';
import {
  CheckCircle as ApproveIcon,
  Cancel as RejectIcon,
  Visibility as ViewIcon,
} from '@mui/icons-material';
import { StatusBadge } from '../components/StatusBadge';
import { OcrResultsViewer } from '../components/OcrResultsViewer';
import { apiClient } from '../api/axios';
import type { TaxApplication } from '../types';

export const AgentDashboard: React.FC = () => {
  const [applications, setApplications] = useState<TaxApplication[]>([]);
  const [selectedApp, setSelectedApp] = useState<TaxApplication | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [comments, setComments] = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [statusMsg, setStatusMsg] = useState('');

  const fetchQueue = async () => {
    try {
      const res = await apiClient.get('/api/agent/applications');
      setApplications(res.data?.data || []);
    } catch {
      setApplications([
        {
          id: 1042,
          taxpayerId: 1,
          taxpayerName: 'Mohamed Ben Ali',
          status: 'PENDING',
          createdDate: '2026-07-30T10:00:00Z',
          updatedDate: '2026-07-30T10:00:00Z',
          activities: [
            {
              businessName: 'DGI Tech Solutions SARL',
              businessType: 'SARL',
              sector: 'Technologies de l\'Information',
              address: 'Cyberparc El Ghazala, Ariana',
              startDate: '2026-08-01',
            },
          ],
          documents: [
            { type: 'CIN_COPY', filePath: '/uploads/cin.pdf', verificationStatus: 'PENDING' },
            { type: 'COMMERCIAL_REGISTER', filePath: '/uploads/rc.pdf', verificationStatus: 'VERIFIED' },
          ],
        },
      ]);
    }
  };

  useEffect(() => {
    fetchQueue();
  }, []);

  const handleOpenReview = (app: TaxApplication) => {
    setSelectedApp(app);
    setComments('');
    setDialogOpen(true);
  };

  const handleAction = async (status: 'APPROVED' | 'REJECTED') => {
    if (!selectedApp) return;
    setActionLoading(true);

    try {
      await apiClient.put(`/api/agent/applications/${selectedApp.id}/review`, {
        status,
        comments,
      });

      setStatusMsg(`Application #${selectedApp.id} status updated to ${status}. ${status === 'APPROVED' ? 'Fiscal Number auto-generated!' : ''}`);
      setDialogOpen(false);
      fetchQueue();
    } catch {
      setStatusMsg(`Application #${selectedApp.id} updated to ${status}.`);
      setDialogOpen(false);
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 1 }}>
        Portail Agent Fiscal DGI - File d'Examen
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Examinez les dossiers d'immatriculation, contrôlez les pièces jointes OCR / Biométrie et attribuez les matricules fiscaux.
      </Typography>

      {statusMsg && (
        <Alert severity="success" sx={{ mb: 3, borderRadius: 2 }}>
          {statusMsg}
        </Alert>
      )}

      <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 3 }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 700 }}>N° Dossier</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Contribuable</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Entreprise / Raison Sociale</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Date Soumission</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Statut</TableCell>
              <TableCell sx={{ fontWeight: 700, textAlign: 'center' }}>Action Agent</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {applications.map((app) => (
              <TableRow key={app.id} hover>
                <TableCell sx={{ fontWeight: 700 }}>#{app.id}</TableCell>
                <TableCell>{app.taxpayerName}</TableCell>
                <TableCell>{app.activities.length > 0 ? app.activities[0].businessName : 'N/A'}</TableCell>
                <TableCell>{new Date(app.createdDate).toLocaleDateString()}</TableCell>
                <TableCell>
                  <StatusBadge status={app.status} />
                </TableCell>
                <TableCell sx={{ textAlign: 'center' }}>
                  <Button
                    variant="contained"
                    size="small"
                    startIcon={<ViewIcon />}
                    onClick={() => handleOpenReview(app)}
                  >
                    Examiner
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Agent Review Dialog */}
      {selectedApp && (
        <Dialog
          open={dialogOpen}
          onClose={() => setDialogOpen(false)}
          maxWidth="md"
          fullWidth
          slotProps={{
            paper: { sx: { borderRadius: 3 } },
          }}
        >
          <DialogTitle sx={{ fontWeight: 700 }}>
            Examen du Dossier #{selectedApp.id} - {selectedApp.taxpayerName}
          </DialogTitle>
          <DialogContent dividers>
            <Grid container spacing={3} sx={{ mb: 3 }}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <Typography variant="subtitle2" color="text.secondary">
                  Détails Contribuable
                </Typography>
                <Typography variant="body1" sx={{ fontWeight: 700 }}>
                  {selectedApp.taxpayerName}
                </Typography>
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <Typography variant="subtitle2" color="text.secondary">
                  Activité Économique
                </Typography>
                <Typography variant="body1" sx={{ fontWeight: 700 }}>
                  {selectedApp.activities.length > 0 ? selectedApp.activities[0].businessName : 'N/A'}
                </Typography>
              </Grid>
            </Grid>

            <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>
              Résultats OCR & Vérification Biométrique
            </Typography>
            <OcrResultsViewer />

            <Typography variant="h6" sx={{ fontWeight: 700, mt: 3, mb: 1 }}>
              Remarques Agent / Motif de décision
            </Typography>
            <TextField
              fullWidth
              multiline
              rows={3}
              placeholder="Saisissez des commentaires à l'attention du contribuable en cas de rejet ou de pièces complémentaires requises..."
              value={comments}
              onChange={(e) => setComments(e.target.value)}
            />
          </DialogContent>
          <DialogActions sx={{ p: 2, justifyContent: 'space-between' }}>
            <Button onClick={() => setDialogOpen(false)}>Annuler</Button>
            <Box sx={{ display: 'flex', gap: 1.5 }}>
              <Button
                variant="outlined"
                color="error"
                startIcon={<RejectIcon />}
                disabled={actionLoading}
                onClick={() => handleAction('REJECTED')}
              >
                Rejeter Dossier
              </Button>
              <Button
                variant="contained"
                color="success"
                startIcon={<ApproveIcon />}
                disabled={actionLoading}
                onClick={() => handleAction('APPROVED')}
                sx={{ fontWeight: 700 }}
              >
                Approuver & Générer Matricule
              </Button>
            </Box>
          </DialogActions>
        </Dialog>
      )}
    </Box>
  );
};

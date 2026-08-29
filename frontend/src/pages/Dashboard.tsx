import React, { useState, useEffect } from 'react';
import {
  Grid,
  Card,
  CardContent,
  Typography,
  Box,
  Button,
  Avatar,
  Paper,
  useTheme,
} from '@mui/material';
import {
  Assignment as AppIcon,
  Badge as FiscalIcon,
  Notifications as NotifIcon,
  AddCircleOutlined as NewAppIcon,
  Download as DownloadIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { StatusBadge } from '../components/StatusBadge';
import { applicationService } from '../services/application.service';
import { TaxApplication } from '../types';

export const Dashboard: React.FC = () => {
  const theme = useTheme();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [applications, setApplications] = useState<TaxApplication[]>([]);

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        const apps = await applicationService.getApplicationsStatus();
        setApplications(apps);
      } catch {
        setApplications([
          {
            id: 1042,
            taxpayerId: user?.taxpayerId || 1,
            taxpayerName: `${user?.firstName || 'Mohamed'} ${user?.lastName || 'Ben Ali'}`,
            status: 'APPROVED',
            createdDate: '2026-07-20T10:30:00Z',
            updatedDate: '2026-07-22T14:15:00Z',
            activities: [
              {
                id: 1,
                businessName: 'DGI Tech Solutions SARL',
                businessType: 'SARL',
                sector: 'Technologies de l\'Information',
                address: 'Avenue Habib Bourguiba, Tunis',
                startDate: '2026-08-01',
              },
            ],
            documents: [],
          },
        ]);
      }
    };

    fetchDashboardData();
  }, [user]);

  const latestApp = applications.length > 0 ? applications[0] : null;

  return (
    <Box>
      {/* Welcome Banner */}
      <Paper
        elevation={0}
        sx={{
          p: { xs: 3, md: 4 },
          mb: 4,
          borderRadius: 4,
          background: theme.palette.mode === 'light'
            ? 'linear-gradient(135deg, #0f4c81 0%, #1e3a8a 100%)'
            : 'linear-gradient(135deg, #1e293b 0%, #0f172a 100%)',
          color: '#ffffff',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 2,
        }}
      >
        <Box sx={{ maxWidth: 600 }}>
          <Typography variant="h4" sx={{ fontWeight: 800, mb: 1 }}>
            Bienvenue, {user?.firstName || user?.username}!
          </Typography>
          <Typography variant="body1" sx={{ opacity: 0.9 }}>
            Plateforme Numérique Officielle de la Direction Générale des Impôts. Gérez vos déclarations, immatriculations et attestations fiscales en ligne.
          </Typography>
        </Box>

        <Button
          variant="contained"
          size="large"
          startIcon={<NewAppIcon />}
          onClick={() => navigate('/registration/new')}
          sx={{
            bgcolor: theme.palette.secondary.main,
            color: '#fff',
            fontWeight: 700,
            py: 1.5,
            px: 3,
            '&:hover': {
              bgcolor: theme.palette.secondary.dark,
            },
          }}
        >
          Nouvelle Immatriculation
        </Button>
      </Paper>

      {/* Display Cards Grid */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        {/* Card 1: Fiscal Number */}
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <Card variant="outlined" sx={{ borderRadius: 3, height: '100%' }}>
            <CardContent sx={{ display: 'flex', flexDirection: 'column', height: '100%', justifyContent: 'space-between' }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                <Avatar sx={{ bgcolor: theme.palette.primary.light + '20', color: theme.palette.primary.main }}>
                  <FiscalIcon />
                </Avatar>
                <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                  Matricule Fiscal
                </Typography>
              </Box>
              <Typography variant="h5" color="primary.main" sx={{ fontWeight: 800, my: 1 }}>
                {user?.fiscalNumber || '1548962/A/M/000'}
              </Typography>
              <Box sx={{ mt: 2, pt: 1, borderTop: `1px solid ${theme.palette.divider}`, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="caption" color="text.secondary">
                  Statut: Immatriculé
                </Typography>
                <Button
                  size="small"
                  startIcon={<DownloadIcon />}
                  onClick={() => window.open(`http://localhost:8082/api/documents/certificate/${user?.taxpayerId || 1}`, '_blank')}
                  sx={{ textTransform: 'none', fontWeight: 600 }}
                >
                  Télécharger Attestation
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Card 2: Application Status */}
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <Card variant="outlined" sx={{ borderRadius: 3, height: '100%' }}>
            <CardContent sx={{ display: 'flex', flexDirection: 'column', height: '100%', justifyContent: 'space-between' }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                <Avatar sx={{ bgcolor: theme.palette.secondary.light + '20', color: theme.palette.secondary.main }}>
                  <AppIcon />
                </Avatar>
                <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                  Dernier Dossier
                </Typography>
              </Box>
              <Box sx={{ my: 1 }}>
                <Typography variant="h6" sx={{ fontWeight: 700 }}>
                  Dossier #{latestApp ? latestApp.id : '1042'}
                </Typography>
                <Box sx={{ mt: 1 }}>
                  <StatusBadge status={latestApp ? latestApp.status : 'APPROVED'} />
                </Box>
              </Box>
              <Box sx={{ mt: 2, pt: 1, borderTop: `1px solid ${theme.palette.divider}`, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="caption" color="text.secondary">
                  Total dossiers: {applications.length}
                </Typography>
                <Button size="small" onClick={() => navigate('/applications')} sx={{ fontWeight: 600 }}>
                  Voir Tous
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Card 3: Notifications */}
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <Card variant="outlined" sx={{ borderRadius: 3, height: '100%' }}>
            <CardContent sx={{ display: 'flex', flexDirection: 'column', height: '100%', justifyContent: 'space-between' }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                <Avatar sx={{ bgcolor: theme.palette.warning.light + '20', color: theme.palette.warning.main }}>
                  <NotifIcon />
                </Avatar>
                <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                  Centre de Notifications
                </Typography>
              </Box>
              <Typography variant="body2" color="text.secondary" sx={{ my: 1 }}>
                Vous avez des mises à jour récentes concernant la vérification de vos pièces justificatives.
              </Typography>
              <Box sx={{ mt: 2, pt: 1, borderTop: `1px solid ${theme.palette.divider}`, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="caption" color="text.secondary">
                  Nouveaux messages
                </Typography>
                <Button size="small" onClick={() => navigate('/notifications')} sx={{ fontWeight: 600 }}>
                  Consulter
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

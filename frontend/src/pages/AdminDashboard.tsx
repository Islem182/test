import React, { useState } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Avatar,
  Tab,
  Tabs,
  LinearProgress,
} from '@mui/material';
import {
  People as UsersIcon,
  SupportAgent as AgentIcon,
  Analytics as StatsIcon,
  Security as SecurityIcon,
} from '@mui/icons-material';

export const AdminDashboard: React.FC = () => {
  const [tabValue, setTabValue] = useState(0);

  const usersMock = [
    { id: 1, username: 'mbenali', email: 'mohamed.benali@gmail.com', role: 'TAXPAYER', status: 'ACTIVE', created: '2026-07-20' },
    { id: 2, username: 'agent_imane', email: 'imane.trabelsi@dgi.tn', role: 'AGENT', status: 'ACTIVE', created: '2026-07-15' },
    { id: 3, username: 'admin_sys', email: 'admin@dgi.tn', role: 'ADMIN', status: 'ACTIVE', created: '2026-07-01' },
    { id: 4, username: 'sami_khelil', email: 'sami.khelil@gmail.com', role: 'TAXPAYER', status: 'ACTIVE', created: '2026-07-28' },
  ];

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 1 }}>
        Administration & Gouvernance Système DGI
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Gestion des utilisateurs, affectation des agents fiscaux et supervision des métriques système
      </Typography>

      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card variant="outlined" sx={{ borderRadius: 3 }}>
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Avatar sx={{ bgcolor: 'primary.light' + '20', color: 'primary.main' }}>
                <UsersIcon />
              </Avatar>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Total Contribuables
                </Typography>
                <Typography variant="h5" sx={{ fontWeight: 800 }}>
                  14,820
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card variant="outlined" sx={{ borderRadius: 3 }}>
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Avatar sx={{ bgcolor: 'secondary.light' + '20', color: 'secondary.main' }}>
                <AgentIcon />
              </Avatar>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Agents Fiscaux Actifs
                </Typography>
                <Typography variant="h5" sx={{ fontWeight: 800 }}>
                  245
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card variant="outlined" sx={{ borderRadius: 3 }}>
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Avatar sx={{ bgcolor: 'warning.light' + '20', color: 'warning.main' }}>
                <StatsIcon />
              </Avatar>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Immatriculations / Jour
                </Typography>
                <Typography variant="h5" sx={{ fontWeight: 800 }}>
                  384
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card variant="outlined" sx={{ borderRadius: 3 }}>
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Avatar sx={{ bgcolor: 'error.light' + '20', color: 'error.main' }}>
                <SecurityIcon />
              </Avatar>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Taux de Détection OCR
                </Typography>
                <Typography variant="h5" sx={{ fontWeight: 800 }}>
                  98.6%
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Paper variant="outlined" sx={{ borderRadius: 3, mb: 3 }}>
        <Tabs value={tabValue} onChange={(_, v) => setTabValue(v)} sx={{ px: 2, pt: 1 }}>
          <Tab label="Gestion des Utilisateurs" />
          <Tab label="Effectif Agents Fiscaux" />
          <Tab label="Statistiques & Performance" />
        </Tabs>
      </Paper>

      {tabValue === 0 && (
        <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 3 }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell sx={{ fontWeight: 700 }}>ID</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Nom d'utilisateur</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Email Officiel</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Rôle Système</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Statut</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Créé le</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {usersMock.map((u) => (
                <TableRow key={u.id} hover>
                  <TableCell sx={{ fontWeight: 700 }}>#{u.id}</TableCell>
                  <TableCell>{u.username}</TableCell>
                  <TableCell>{u.email}</TableCell>
                  <TableCell>
                    <Chip
                      label={u.role}
                      color={u.role === 'ADMIN' ? 'error' : u.role === 'AGENT' ? 'secondary' : 'primary'}
                      size="small"
                      sx={{ fontWeight: 700 }}
                    />
                  </TableCell>
                  <TableCell>
                    <Chip label={u.status} color="success" size="small" variant="outlined" />
                  </TableCell>
                  <TableCell>{u.created}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {tabValue === 2 && (
        <Card variant="outlined" sx={{ borderRadius: 3, p: 3 }}>
          <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>
            Supervision de la Charge Système & Serveur PostgreSQL
          </Typography>

          <Box sx={{ mb: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
              <Typography variant="subtitle2">Charge CPU Serveur Backend (Spring Boot 3)</Typography>
              <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>28%</Typography>
            </Box>
            <LinearProgress variant="determinate" value={28} sx={{ height: 8, borderRadius: 4 }} />
          </Box>

          <Box sx={{ mb: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
              <Typography variant="subtitle2">Utilisation Mémoire Heap JVM (Java 17)</Typography>
              <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>42%</Typography>
            </Box>
            <LinearProgress variant="determinate" value={42} color="secondary" sx={{ height: 8, borderRadius: 4 }} />
          </Box>

          <Box sx={{ mb: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
              <Typography variant="subtitle2">Pool de Connexions PostgreSQL 16</Typography>
              <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>18 / 100 Active</Typography>
            </Box>
            <LinearProgress variant="determinate" value={18} color="warning" sx={{ height: 8, borderRadius: 4 }} />
          </Box>
        </Card>
      )}
    </Box>
  );
};

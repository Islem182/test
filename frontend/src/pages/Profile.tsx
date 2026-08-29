import React, { useState } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  TextField,
  Button,
  Alert,
  Avatar,
  Divider,
  Tab,
  Tabs,
  Paper,
} from '@mui/material';
import { PhotoCamera as CameraIcon, Save as SaveIcon, Lock as LockIcon } from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';
import { userService } from '../services/user.service';

export const Profile: React.FC = () => {
  const { user, refreshProfile } = useAuth();
  const [tabValue, setTabValue] = useState(0);

  const [profileData, setProfileData] = useState({
    firstName: user?.firstName || '',
    lastName: user?.lastName || '',
    phone: user?.phone || '',
    cin: user?.cin || '',
    passport: user?.passport || '',
    address: user?.address || '',
  });

  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleProfileSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMessage('');
    setError('');
    setLoading(true);

    try {
      await userService.updateProfile(profileData);
      await refreshProfile();
      setMessage('Votre profil a été mis à jour avec succès !');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Échec de la mise à jour du profil.');
    } finally {
      setLoading(false);
    }
  };

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMessage('');
    setError('');

    if (passwordData.newPassword !== passwordData.confirmPassword) {
      setError('Le nouveau mot de passe et sa confirmation ne correspondent pas.');
      return;
    }

    setLoading(true);
    try {
      const msg = await userService.changePassword(
        passwordData.currentPassword,
        passwordData.newPassword
      );
      setMessage(msg || 'Mot de passe modifié avec succès !');
      setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err: any) {
      setError(err.response?.data?.message || 'Le mot de passe actuel est incorrect.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 3 }}>
        Profil & Paramètres du Compte
      </Typography>

      {message && (
        <Alert severity="success" sx={{ mb: 3, borderRadius: 2 }}>
          {message}
        </Alert>
      )}
      {error && (
        <Alert severity="error" sx={{ mb: 3, borderRadius: 2 }}>
          {error}
        </Alert>
      )}

      <Paper variant="outlined" sx={{ borderRadius: 3, mb: 3 }}>
        <Tabs value={tabValue} onChange={(_, val) => setTabValue(val)} sx={{ px: 2, pt: 1 }}>
          <Tab label="Informations Personnelles" />
          <Tab label="Changer le Mot de Passe" />
        </Tabs>
      </Paper>

      {tabValue === 0 && (
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 4 }}>
            <Card variant="outlined" sx={{ borderRadius: 3, textAlign: 'center', p: 3 }}>
              <Box sx={{ position: 'relative', display: 'inline-block', mb: 2 }}>
                <Avatar
                  sx={{
                    width: 120,
                    height: 120,
                    fontSize: '2.5rem',
                    bgcolor: 'primary.main',
                    boxShadow: '0 8px 24px rgba(0,0,0,0.15)',
                  }}
                >
                  {user?.firstName ? user.firstName.charAt(0).toUpperCase() : user?.username?.charAt(0).toUpperCase() || 'U'}
                </Avatar>
                <Button
                  variant="contained"
                  size="small"
                  sx={{
                    position: 'absolute',
                    bottom: 0,
                    right: 0,
                    minWidth: 'auto',
                    borderRadius: '50%',
                    p: 1,
                  }}
                  component="label"
                >
                  <CameraIcon fontSize="small" />
                  <input type="file" hidden accept="image/*" />
                </Button>
              </Box>
              <Typography variant="h6" sx={{ fontWeight: 700 }}>
                {user?.firstName} {user?.lastName}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {user?.email}
              </Typography>
              <Typography variant="caption" color="primary.main" sx={{ fontWeight: 700, display: 'block', mt: 1 }}>
                Matricule: {user?.fiscalNumber || '1548962/A/M/000'}
              </Typography>
            </Card>
          </Grid>

          <Grid size={{ xs: 12, md: 8 }}>
            <Card variant="outlined" sx={{ borderRadius: 3 }}>
              <CardContent component="form" onSubmit={handleProfileSubmit}>
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                  Modifier vos Informations
                </Typography>
                <Divider sx={{ mb: 3 }} />

                <Grid container spacing={2}>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth
                      label="Prénom"
                      value={profileData.firstName}
                      onChange={(e) => setProfileData({ ...profileData, firstName: e.target.value })}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth
                      label="Nom"
                      value={profileData.lastName}
                      onChange={(e) => setProfileData({ ...profileData, lastName: e.target.value })}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth
                      label="Téléphone"
                      value={profileData.phone}
                      onChange={(e) => setProfileData({ ...profileData, phone: e.target.value })}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth
                      label="Carte CIN"
                      value={profileData.cin}
                      onChange={(e) => setProfileData({ ...profileData, cin: e.target.value })}
                    />
                  </Grid>
                  <Grid size={{ xs: 12 }}>
                    <TextField
                      fullWidth
                      label="Adresse de Domiciliation"
                      multiline
                      rows={3}
                      value={profileData.address}
                      onChange={(e) => setProfileData({ ...profileData, address: e.target.value })}
                    />
                  </Grid>
                </Grid>

                <Button
                  type="submit"
                  variant="contained"
                  startIcon={<SaveIcon />}
                  disabled={loading}
                  sx={{ mt: 3, fontWeight: 700 }}
                >
                  Sauvegarder les Modifications
                </Button>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {tabValue === 1 && (
        <Card variant="outlined" sx={{ borderRadius: 3, maxWidth: 600 }}>
          <CardContent component="form" onSubmit={handlePasswordSubmit}>
            <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
              Modifier le Mot de Passe
            </Typography>
            <Divider sx={{ mb: 3 }} />

            <TextField
              fullWidth
              label="Mot de passe actuel"
              type="password"
              margin="normal"
              value={passwordData.currentPassword}
              onChange={(e) => setPasswordData({ ...passwordData, currentPassword: e.target.value })}
              required
            />
            <TextField
              fullWidth
              label="Nouveau mot de passe"
              type="password"
              margin="normal"
              value={passwordData.newPassword}
              onChange={(e) => setPasswordData({ ...passwordData, newPassword: e.target.value })}
              required
            />
            <TextField
              fullWidth
              label="Confirmer le nouveau mot de passe"
              type="password"
              margin="normal"
              value={passwordData.confirmPassword}
              onChange={(e) => setPasswordData({ ...passwordData, confirmPassword: e.target.value })}
              required
            />

            <Button
              type="submit"
              variant="contained"
              startIcon={<LockIcon />}
              disabled={loading}
              sx={{ mt: 3, fontWeight: 700 }}
            >
              Changer le Mot de Passe
            </Button>
          </CardContent>
        </Card>
      )}
    </Box>
  );
};

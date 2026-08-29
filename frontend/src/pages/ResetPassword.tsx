import React, { useState } from 'react';
import { Typography, TextField, Button, Box, Alert, Link } from '@mui/material';
import { useNavigate, useSearchParams, Link as RouterLink } from 'react-router-dom';
import { authService } from '../services/auth.service';

export const ResetPassword: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const tokenParam = searchParams.get('token') || '';

  const [token, setToken] = useState(tokenParam);
  const [newPassword, setNewPassword] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);

    try {
      const msg = await authService.resetPassword(token, newPassword);
      setMessage(msg || 'Votre mot de passe a été réinitialisé avec succès !');
      setTimeout(() => navigate('/login'), 2000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Code de réinitialisation invalide ou expiré.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box component="form" onSubmit={handleSubmit}>
      <Typography variant="h5" align="center" sx={{ fontWeight: 700, mb: 1 }}>
        Réinitialisation du Mot de Passe
      </Typography>
      <Typography variant="body2" align="center" color="text.secondary" sx={{ mb: 3 }}>
        Saisissez le jeton de sécurité et votre nouveau mot de passe
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

      <TextField
        fullWidth
        label="Jeton de réinitialisation (Token)"
        variant="outlined"
        margin="normal"
        value={token}
        onChange={(e) => setToken(e.target.value)}
        required
      />

      <TextField
        fullWidth
        label="Nouveau mot de passe"
        type="password"
        variant="outlined"
        margin="normal"
        value={newPassword}
        onChange={(e) => setNewPassword(e.target.value)}
        required
      />

      <Button
        type="submit"
        fullWidth
        variant="contained"
        size="large"
        disabled={loading}
        sx={{ mt: 3, mb: 2, py: 1.2, fontWeight: 700 }}
      >
        {loading ? 'Réinitialisation...' : 'Réinitialiser le mot de passe'}
      </Button>

      <Typography variant="body2" align="center" color="text.secondary">
        Retour à la{' '}
        <Link component={RouterLink} to="/login" sx={{ fontWeight: 700 }}>
          Connexion
        </Link>
      </Typography>
    </Box>
  );
};

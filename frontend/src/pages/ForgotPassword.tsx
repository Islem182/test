import React, { useState } from 'react';
import { Typography, TextField, Button, Box, Alert, Link } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { authService } from '../services/auth.service';

export const ForgotPassword: React.FC = () => {
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);

    try {
      const msg = await authService.forgotPassword(email);
      setMessage(msg || 'Un lien de réinitialisation vous a été envoyé par email.');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Erreur lors de la demande. Veuillez vérifier votre adresse email.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box component="form" onSubmit={handleSubmit}>
      <Typography variant="h5" align="center" sx={{ fontWeight: 700, mb: 1 }}>
        Mot de Passe Oublié
      </Typography>
      <Typography variant="body2" align="center" color="text.secondary" sx={{ mb: 3 }}>
        Saisissez votre email pour recevoir les instructions de réinitialisation
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
        label="Adresse Email"
        type="email"
        variant="outlined"
        margin="normal"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
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
        {loading ? 'Envoi...' : 'Envoyer le lien'}
      </Button>

      <Typography variant="body2" align="center" color="text.secondary">
        Retour à la{' '}
        <Link component={RouterLink} to="/login" sx={{ fontWeight: 700 }}>
          page de connexion
        </Link>
      </Typography>
    </Box>
  );
};

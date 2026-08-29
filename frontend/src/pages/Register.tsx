import React, { useState } from 'react';
import {
  Typography,
  TextField,
  Button,
  Box,
  Alert,
  Link,
  Grid,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
} from '@mui/material';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export const Register: React.FC = () => {
  const navigate = useNavigate();
  const { register: registerUser } = useAuth();
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    phone: '',
    firstName: '',
    lastName: '',
    cin: '',
    passport: '',
    address: '',
    role: 'TAXPAYER',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | { name?: string; value: unknown }>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name as string]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await registerUser(formData);
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Registration failed. Username or email may already be in use.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box component="form" onSubmit={handleSubmit}>
      <Typography variant="h5" align="center" sx={{ fontWeight: 700, mb: 1 }}>
        Inscription en Ligne
      </Typography>
      <Typography variant="body2" align="center" color="text.secondary" sx={{ mb: 3 }}>
        Créez votre compte Contribuable ou Agent DGI
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 3, borderRadius: 2 }}>
          {error}
        </Alert>
      )}

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            fullWidth
            label="Prénom *"
            name="firstName"
            value={formData.firstName}
            onChange={handleChange}
            required
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            fullWidth
            label="Nom *"
            name="lastName"
            value={formData.lastName}
            onChange={handleChange}
            required
          />
        </Grid>

        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            fullWidth
            label="Nom d'utilisateur *"
            name="username"
            value={formData.username}
            onChange={handleChange}
            required
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            fullWidth
            label="Email *"
            type="email"
            name="email"
            value={formData.email}
            onChange={handleChange}
            required
          />
        </Grid>

        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            fullWidth
            label="Mot de passe *"
            type="password"
            name="password"
            value={formData.password}
            onChange={handleChange}
            required
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            fullWidth
            label="Téléphone"
            name="phone"
            value={formData.phone}
            onChange={handleChange}
          />
        </Grid>

        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            fullWidth
            label="Numéro CIN"
            name="cin"
            value={formData.cin}
            onChange={handleChange}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            fullWidth
            label="Passport"
            name="passport"
            value={formData.passport}
            onChange={handleChange}
          />
        </Grid>

        <Grid size={{ xs: 12 }}>
          <TextField
            fullWidth
            label="Adresse de Résidence"
            name="address"
            value={formData.address}
            onChange={handleChange}
            multiline
            rows={2}
          />
        </Grid>

        <Grid size={{ xs: 12 }}>
          <FormControl fullWidth size="small">
            <InputLabel>Rôle du Compte</InputLabel>
            <Select
              name="role"
              value={formData.role}
              label="Rôle du Compte"
              onChange={handleChange as any}
            >
              <MenuItem value="TAXPAYER">Contribuable (Taxpayer)</MenuItem>
              <MenuItem value="AGENT">Agent Fiscal DGI</MenuItem>
              <MenuItem value="ADMIN">Administrateur Système</MenuItem>
            </Select>
          </FormControl>
        </Grid>
      </Grid>

      <Button
        type="submit"
        fullWidth
        variant="contained"
        size="large"
        disabled={loading}
        sx={{ mt: 3, mb: 2, py: 1.2, fontWeight: 700 }}
      >
        {loading ? 'Inscription...' : "S'inscrire"}
      </Button>

      <Typography variant="body2" align="center" color="text.secondary">
        Vous avez déjà un compte ?{' '}
        <Link component={RouterLink} to="/login" sx={{ fontWeight: 700 }}>
          Se connecter
        </Link>
      </Typography>
    </Box>
  );
};

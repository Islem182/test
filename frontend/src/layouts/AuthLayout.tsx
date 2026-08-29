import React from 'react';
import { Box, Container, Paper, Typography, useTheme } from '@mui/material';
import { Outlet } from 'react-router-dom';
import { useColorMode } from '../context/ThemeContext';

export const AuthLayout: React.FC = () => {
  const theme = useTheme();
  const { mode } = useColorMode();

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        background: mode === 'light'
          ? 'linear-gradient(135deg, #0f4c81 0%, #1e293b 100%)'
          : 'linear-gradient(135deg, #0f172a 0%, #1e1b4b 100%)',
        p: 2,
      }}
    >
      <Container maxWidth="sm">
        <Box sx={{ textAlign: 'center', mb: 3 }}>
          <Box
            sx={{
              width: 56,
              height: 56,
              mx: 'auto',
              mb: 1.5,
              borderRadius: 3,
              backgroundColor: '#ffffff',
              color: theme.palette.primary.main,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 800,
              fontSize: '1.5rem',
              boxShadow: '0 8px 24px rgba(0,0,0,0.3)',
            }}
          >
            DGI
          </Box>
          <Typography variant="h5" color="#ffffff" sx={{ fontWeight: 700 }}>
            Plateforme DGI en Ligne
          </Typography>
          <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.7)', mt: 0.5 }}>
            Ministère des Finances • Direction Générale des Impôts
          </Typography>
        </Box>

        <Paper
          elevation={6}
          sx={{
            p: { xs: 3, sm: 4 },
            borderRadius: 4,
            backgroundColor: theme.palette.background.paper,
            boxShadow: '0 12px 40px rgba(0,0,0,0.25)',
          }}
        >
          <Outlet />
        </Paper>

        <Typography variant="caption" sx={{ display: 'block', textAlign: 'center', color: 'rgba(255,255,255,0.6)', mt: 3 }}>
          © 2026 Direction Générale des Impôts - République Tunisienne. Tous droits réservés.
        </Typography>
      </Container>
    </Box>
  );
};

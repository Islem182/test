import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { CircularProgress, Box } from '@mui/material';
import { useAuth } from '../context/AuthContext';

interface ProtectedRouteProps {
  /** If "ADMIN", only the admin account can access. Omit for any authenticated user. */
  requiredRole?: 'ADMIN' | 'TAXPAYER' | 'AGENT';
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ requiredRole }) => {
  const { isAuthenticated, loading, isAdmin } = useAuth();

  if (loading) {
    return (
      <Box sx={{ display: 'flex', height: '100vh', justifyContent: 'center', alignItems: 'center' }}>
        <CircularProgress size={48} />
      </Box>
    );
  }

  if (!isAuthenticated) return <Navigate to="/login" replace />;

  if (requiredRole === 'ADMIN' && !isAdmin) return <Navigate to="/dashboard" replace />;
  if (requiredRole === 'TAXPAYER' && isAdmin) return <Navigate to="/admin/dashboard" replace />;

  return <Outlet />;
};

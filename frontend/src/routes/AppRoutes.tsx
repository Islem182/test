import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthLayout } from '../layouts/AuthLayout';
import { DashboardLayout } from '../layouts/DashboardLayout';
import { ProtectedRoute } from './ProtectedRoute';

// Auth Pages
import { Login } from '../pages/Login';
import { Register } from '../pages/Register';
import { ForgotPassword } from '../pages/ForgotPassword';
import { ResetPassword } from '../pages/ResetPassword';

// Taxpayer Pages
import { Dashboard } from '../pages/Dashboard';
import { Profile } from '../pages/Profile';
import { NewApplication } from '../pages/NewApplication';
import { Applications } from '../pages/Applications';
import { Documents } from '../pages/Documents';
import { Complaints } from '../pages/Complaints';
import { Notifications } from '../pages/Notifications';

// Agent & Admin Pages
import { AgentDashboard } from '../pages/AgentDashboard';
import { AdminDashboard } from '../pages/AdminDashboard';

export const AppRoutes: React.FC = () => {
  return (
    <Routes>
      {/* Public Auth Routes */}
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
      </Route>

      {/* Protected Main Dashboard Routes */}
      <Route element={<ProtectedRoute />}>
        <Route element={<DashboardLayout />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/registration/new" element={<NewApplication />} />
          <Route path="/applications" element={<Applications />} />
          <Route path="/documents" element={<Documents />} />
          <Route path="/complaints" element={<Complaints />} />
          <Route path="/notifications" element={<Notifications />} />

          {/* Agent Protected Routes */}
          <Route element={<ProtectedRoute requiredRole="AGENT" />}>
            <Route path="/agent/dashboard" element={<AgentDashboard />} />
            <Route path="/agent/applications" element={<AgentDashboard />} />
          </Route>

          {/* Admin Protected Routes */}
          <Route element={<ProtectedRoute requiredRole="ADMIN" />}>
            <Route path="/admin/dashboard" element={<AdminDashboard />} />
            <Route path="/admin/users" element={<AdminDashboard />} />
            <Route path="/admin/agents" element={<AdminDashboard />} />
            <Route path="/admin/statistics" element={<AdminDashboard />} />
          </Route>
        </Route>
      </Route>

      {/* Catch-all redirect */}
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
};

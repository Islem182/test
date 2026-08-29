import React, { useEffect, useState } from 'react';
import {
  Box, Card, CardContent, Typography, Grid, Skeleton,
} from '@mui/material';
import {
  People, Assignment, CheckCircle, Cancel, PendingActions, TrendingUp,
} from '@mui/icons-material';
import { applicationService } from '../../services/application.service';
import { userService } from '../../services/user.service';
import type { TaxApplication } from '../../types';

const StatWidget: React.FC<{ label: string; value: number; color: string; icon: React.ReactNode }> = ({ label, value, color, icon }) => (
  <Card>
    <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2, p: 2.5 }}>
      <Box sx={{ width: 48, height: 48, borderRadius: 2, bgcolor: color, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff' }}>
        {icon}
      </Box>
      <Box>
        <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, textTransform: 'uppercase', fontSize: '0.7rem' }}>{label}</Typography>
        <Typography variant="h5" sx={{ fontWeight: 700 }}>{value}</Typography>
      </Box>
    </CardContent>
  </Card>
);

const AdminStatistics: React.FC = () => {
  const [stats, setStats] = useState({
    totalUsers: 0, totalApps: 0, pending: 0, inReview: 0, approved: 0, rejected: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetch = async () => {
      try {
        const [users, apps] = await Promise.all([
          userService.getAllUsers().catch(() => []),
          applicationService.getAllApplications().catch(() => [] as TaxApplication[]),
        ]);
        const appList = apps as TaxApplication[];
        setStats({
          totalUsers: users.length,
          totalApps: appList.length,
          pending: appList.filter((a) => a.status === 'PENDING').length,
          inReview: appList.filter((a) => a.status === 'IN_REVIEW').length,
          approved: appList.filter((a) => a.status === 'APPROVED').length,
          rejected: appList.filter((a) => a.status === 'REJECTED').length,
        });
      } catch { /* ignore */ }
      finally { setLoading(false); }
    };
    fetch();
  }, []);

  const widgets = [
    { label: 'Total Users', value: stats.totalUsers, color: '#0f4c81', icon: <People /> },
    { label: 'Total Applications', value: stats.totalApps, color: '#3b82f6', icon: <Assignment /> },
    { label: 'Pending', value: stats.pending, color: '#f59e0b', icon: <PendingActions /> },
    { label: 'In Review', value: stats.inReview, color: '#8b5cf6', icon: <TrendingUp /> },
    { label: 'Approved', value: stats.approved, color: '#059669', icon: <CheckCircle /> },
    { label: 'Rejected', value: stats.rejected, color: '#dc2626', icon: <Cancel /> },
  ];

  return (
    <Box>
      <Typography variant="h4" sx={{ fontWeight: 700, mb: 3 }}>System Statistics</Typography>

      {loading ? (
        <Grid container spacing={3}>
          {[1,2,3,4,5,6].map((i) => (
            <Grid size={{ xs: 12, sm: 6, md: 4 }} key={i}><Skeleton variant="rounded" height={90} /></Grid>
          ))}
        </Grid>
      ) : (
        <Grid container spacing={3}>
          {widgets.map((w) => (
            <Grid size={{ xs: 12, sm: 6, md: 4 }} key={w.label}>
              <StatWidget label={w.label} value={w.value} color={w.color} icon={w.icon} />
            </Grid>
          ))}
        </Grid>
      )}
    </Box>
  );
};

export default AdminStatistics;

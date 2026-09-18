import React, { useEffect, useState } from 'react';
import {
  Box, Card, CardContent, Typography, Grid, Avatar, Skeleton,
} from '@mui/material';
import { People, SupportAgent, Assignment, TrendingUp } from '@mui/icons-material';
import { userService } from '../../services/user.service';
import { applicationService } from '../../services/application.service';
import type { TaxApplication } from '../../types';

const AdminDashboard: React.FC = () => {
  const [stats, setStats] = useState({ users: 0, agents: 0, applications: 0, approved: 0 });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetch = async () => {
      try {
        const [users, apps] = await Promise.all([
          userService.getAllUsers().catch(() => []),
          applicationService.getAllApplications().catch(() => [] as TaxApplication[]),
        ]);
        const agents = users.filter((u) => u.isAdmin).length;
        const approved = (apps as TaxApplication[]).filter((a) => a.status === 'APPROVED').length;
        setStats({ users: users.length, agents, applications: (apps as TaxApplication[]).length, approved });
      } catch { /* ignore */ }
      finally { setLoading(false); }
    };
    fetch();
  }, []);

  const cards = [
    { title: 'Total Users', value: stats.users, icon: <People />, color: '#0f4c81' },
    { title: 'Agents', value: stats.agents, icon: <SupportAgent />, color: '#059669' },
    { title: 'Applications', value: stats.applications, icon: <Assignment />, color: '#f59e0b' },
    { title: 'Approved', value: stats.approved, icon: <TrendingUp />, color: '#3b82f6' },
  ];

  return (
    <Box>
      <Typography variant="h4" sx={{ fontWeight: 700, mb: 3 }}>Admin Dashboard</Typography>
      <Grid container spacing={3}>
        {cards.map((card) => (
          <Grid size={{ xs: 12, sm: 6, md: 3 }} key={card.title}>
            {loading ? <Skeleton variant="rounded" height={100} /> : (
              <Card>
                <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2, p: 2.5 }}>
                  <Avatar sx={{ bgcolor: card.color, width: 52, height: 52, borderRadius: 2 }}>{card.icon}</Avatar>
                  <Box>
                    <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, textTransform: 'uppercase', fontSize: '0.7rem' }}>
                      {card.title}
                    </Typography>
                    <Typography variant="h4" sx={{ fontWeight: 700 }}>{card.value}</Typography>
                  </Box>
                </CardContent>
              </Card>
            )}
          </Grid>
        ))}
      </Grid>
    </Box>
  );
};

export default AdminDashboard;

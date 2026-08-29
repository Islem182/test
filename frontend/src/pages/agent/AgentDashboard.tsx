import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box, Card, CardContent, Typography, Grid, Avatar, Chip, Table,
  TableBody, TableCell, TableContainer, TableHead, TableRow,
  Skeleton, Button, Tooltip, IconButton,
} from '@mui/material';
import {
  Assignment, PendingActions, CheckCircle, Cancel, Visibility, TrendingUp,
} from '@mui/icons-material';
import { applicationService } from '../../services/application.service';
import { StatusBadge } from '../../components/StatusBadge';
import { formatDate } from '../../utils/helpers';
import type { TaxApplication } from '../../types';

const AgentDashboard: React.FC = () => {
  const navigate = useNavigate();
  const [applications, setApplications] = useState<TaxApplication[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetch = async () => {
      try {
        const data = await applicationService.getAllApplications();
        setApplications(data || []);
      } catch { setApplications([]); }
      finally { setLoading(false); }
    };
    fetch();
  }, []);

  const total = applications.length;
  const pending = applications.filter((a) => a.status === 'PENDING').length;
  const inReview = applications.filter((a) => a.status === 'IN_REVIEW').length;
  const approved = applications.filter((a) => a.status === 'APPROVED').length;
  const rejected = applications.filter((a) => a.status === 'REJECTED').length;
  const queue = applications.filter((a) => a.status === 'PENDING' || a.status === 'IN_REVIEW').slice(0, 10);

  return (
    <Box>
      <Typography variant="h4" sx={{ fontWeight: 700, mb: 3 }}>Agent Dashboard</Typography>

      <Grid container spacing={2.5} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
          <Card>
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2, p: 2 }}>
              <Avatar sx={{ bgcolor: '#0f4c81', width: 44, height: 44, borderRadius: 2 }}><Assignment /></Avatar>
              <Box>
                <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, textTransform: 'uppercase', fontSize: '0.7rem' }}>Total</Typography>
                <Typography variant="h5" sx={{ fontWeight: 700 }}>{total}</Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
          <Card>
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2, p: 2 }}>
              <Avatar sx={{ bgcolor: '#f59e0b', width: 44, height: 44, borderRadius: 2 }}><PendingActions /></Avatar>
              <Box>
                <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, textTransform: 'uppercase', fontSize: '0.7rem' }}>Pending</Typography>
                <Typography variant="h5" sx={{ fontWeight: 700 }}>{pending}</Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
          <Card>
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2, p: 2 }}>
              <Avatar sx={{ bgcolor: '#3b82f6', width: 44, height: 44, borderRadius: 2 }}><TrendingUp /></Avatar>
              <Box>
                <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, textTransform: 'uppercase', fontSize: '0.7rem' }}>In Review</Typography>
                <Typography variant="h5" sx={{ fontWeight: 700 }}>{inReview}</Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
          <Card>
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2, p: 2 }}>
              <Avatar sx={{ bgcolor: '#059669', width: 44, height: 44, borderRadius: 2 }}><CheckCircle /></Avatar>
              <Box>
                <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, textTransform: 'uppercase', fontSize: '0.7rem' }}>Approved</Typography>
                <Typography variant="h5" sx={{ fontWeight: 700 }}>{approved}</Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
          <Card>
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2, p: 2 }}>
              <Avatar sx={{ bgcolor: '#dc2626', width: 44, height: 44, borderRadius: 2 }}><Cancel /></Avatar>
              <Box>
                <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, textTransform: 'uppercase', fontSize: '0.7rem' }}>Rejected</Typography>
                <Typography variant="h5" sx={{ fontWeight: 700 }}>{rejected}</Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Card>
        <CardContent sx={{ p: 0 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', p: 2.5, pb: 1 }}>
            <Typography variant="h6">Application Queue</Typography>
            <Button size="small" onClick={() => navigate('/agent/applications')}>View All</Button>
          </Box>
          <TableContainer>
            {loading ? (
              <Box sx={{ p: 2 }}>{[1,2,3,4].map((i) => <Skeleton key={i} variant="rounded" height={48} sx={{ mb: 1 }} />)}</Box>
            ) : (
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 700 }}>ID</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Taxpayer</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Status</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Submitted</TableCell>
                    <TableCell sx={{ fontWeight: 700 }} align="right">Action</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {queue.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={5} sx={{ textAlign: 'center', py: 4 }}>
                        <Typography color="text.secondary">No pending applications</Typography>
                      </TableCell>
                    </TableRow>
                  ) : queue.map((app) => (
                    <TableRow key={app.id} hover>
                      <TableCell>#{app.id}</TableCell>
                      <TableCell>{app.taxpayerName || '—'}</TableCell>
                      <TableCell><StatusBadge status={app.status} size="small" /></TableCell>
                      <TableCell>{formatDate(app.createdDate)}</TableCell>
                      <TableCell align="right">
                        <Tooltip title="Review">
                          <IconButton color="primary" size="small"
                            onClick={() => navigate(`/agent/applications/${app.id}`)}>
                            <Visibility fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </TableContainer>
        </CardContent>
      </Card>
    </Box>
  );
};

export default AgentDashboard;

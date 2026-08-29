import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box, Card, CardContent, Typography, Grid, Button, Alert, CircularProgress,
  Divider, Paper, Chip, Table, TableBody, TableCell, TableRow, TextField,
} from '@mui/material';
import {
  CheckCircle, Cancel, Description, Face, TextFields, ArrowBack,
} from '@mui/icons-material';
import { applicationService } from '../../services/application.service';
import { StatusBadge } from '../../components/StatusBadge';
import { formatDate } from '../../utils/helpers';
import { getErrorMessage } from '../../utils/helpers';
import type { TaxApplication, ApplicationStatus } from '../../types';

const ApplicationDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [app, setApp] = useState<TaxApplication | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notes, setNotes] = useState('');
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    const fetch = async () => {
      if (!id) return;
      try {
        const data = await applicationService.getApplicationById(Number(id));
        setApp(data);
      } catch (err) {
        setError(getErrorMessage(err));
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, [id]);

  const handleAction = async (status: ApplicationStatus) => {
    if (!id) return;
    setActionLoading(true);
    setError(null);
    try {
      const updated = await applicationService.updateApplicationStatus(Number(id), status, notes);
      setApp(updated);
      setSuccess(`Application ${status.toLowerCase()} successfully.`);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return <Box sx={{ textAlign: 'center', py: 8 }}><CircularProgress /></Box>;
  }

  if (!app) {
    return <Alert severity="error">Application not found.</Alert>;
  }

  return (
    <Box>
      <Button startIcon={<ArrowBack />} onClick={() => navigate(-1)} sx={{ mb: 2 }}>Back</Button>

      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3, flexWrap: 'wrap', gap: 1 }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>Application #{app.id}</Typography>
        <StatusBadge status={app.status} />
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}

      <Grid container spacing={3}>
        {/* Application Info */}
        <Grid size={{ xs: 12, md: 7 }}>
          <Card sx={{ mb: 3 }}>
            <CardContent sx={{ p: 3 }}>
              <Typography variant="h6" sx={{ mb: 2 }}>Application Details</Typography>
              <Table>
                <TableBody>
                  <TableRow><TableCell sx={{ fontWeight: 600, width: '40%' }}>Taxpayer</TableCell><TableCell>{app.taxpayerName || '—'}</TableCell></TableRow>
                  <TableRow><TableCell sx={{ fontWeight: 600 }}>Application ID</TableCell><TableCell>#{app.id}</TableCell></TableRow>
                  <TableRow><TableCell sx={{ fontWeight: 600 }}>Status</TableCell><TableCell><StatusBadge status={app.status} size="small" /></TableCell></TableRow>
                  <TableRow><TableCell sx={{ fontWeight: 600 }}>Submitted</TableCell><TableCell>{formatDate(app.createdDate)}</TableCell></TableRow>
                  <TableRow><TableCell sx={{ fontWeight: 600 }}>Last Updated</TableCell><TableCell>{formatDate(app.updatedDate)}</TableCell></TableRow>
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          {/* Activities */}
          <Card sx={{ mb: 3 }}>
            <CardContent sx={{ p: 3 }}>
              <Typography variant="h6" sx={{ mb: 2 }}>Business Activities</Typography>
              {app.activities?.length ? app.activities.map((act, i) => (
                <Paper key={i} variant="outlined" sx={{ p: 2, mb: i < app.activities.length - 1 ? 1 : 0 }}>
                  <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>{act.businessName}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {act.businessType && `Type: ${act.businessType}`}
                    {act.sector && ` • Sector: ${act.sector}`}
                  </Typography>
                  {act.address && <Typography variant="body2" color="text.secondary">Address: {act.address}</Typography>}
                </Paper>
              )) : (
                <Typography color="text.secondary">No activities listed</Typography>
              )}
            </CardContent>
          </Card>

          {/* Agent Actions */}
          {(app.status === 'PENDING' || app.status === 'IN_REVIEW') && (
            <Card>
              <CardContent sx={{ p: 3 }}>
                <Typography variant="h6" sx={{ mb: 2 }}>Agent Actions</Typography>
                <TextField
                  fullWidth multiline rows={3} label="Notes (optional)"
                  value={notes} onChange={(e) => setNotes(e.target.value)}
                  sx={{ mb: 2 }}
                />
                <Box sx={{ display: 'flex', gap: 2 }}>
                  <Button
                    variant="contained" color="success" disabled={actionLoading}
                    startIcon={actionLoading ? <CircularProgress size={18} color="inherit" /> : <CheckCircle />}
                    onClick={() => handleAction('APPROVED')}
                  >
                    Approve
                  </Button>
                  <Button
                    variant="contained" color="error" disabled={actionLoading}
                    startIcon={actionLoading ? <CircularProgress size={18} color="inherit" /> : <Cancel />}
                    onClick={() => handleAction('REJECTED')}
                  >
                    Reject
                  </Button>
                </Box>
              </CardContent>
            </Card>
          )}
        </Grid>

        {/* Sidebar: Documents & Verification */}
        <Grid size={{ xs: 12, md: 5 }}>
          <Card sx={{ mb: 3 }}>
            <CardContent sx={{ p: 3 }}>
              <Typography variant="h6" sx={{ mb: 2 }}>Documents</Typography>
              {app.documents?.length ? app.documents.map((doc, i) => (
                <Paper key={i} variant="outlined" sx={{ p: 1.5, mb: 1, display: 'flex', alignItems: 'center', gap: 1.5 }}>
                  <Description sx={{ color: 'primary.main' }} />
                  <Box sx={{ flex: 1 }}>
                    <Typography variant="body2" sx={{ fontWeight: 500 }}>{doc.type}</Typography>
                    <Typography variant="caption" color="text.secondary">{doc.fileName || doc.filePath}</Typography>
                  </Box>
                  {doc.verificationStatus && <StatusBadge status={doc.verificationStatus} size="small" />}
                </Paper>
              )) : (
                <Typography color="text.secondary">No documents uploaded</Typography>
              )}
            </CardContent>
          </Card>

          <Card sx={{ mb: 3 }}>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                <TextFields color="primary" />
                <Typography variant="h6">OCR Results</Typography>
              </Box>
              <Paper variant="outlined" sx={{ p: 2, textAlign: 'center' }}>
                <Typography color="text.secondary">AI features not yet implemented</Typography>
              </Paper>
            </CardContent>
          </Card>

          <Card>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                <Face color="primary" />
                <Typography variant="h6">Face Verification</Typography>
              </Box>
              <Paper variant="outlined" sx={{ p: 2, textAlign: 'center' }}>
                <Typography color="text.secondary">AI features not yet implemented</Typography>
              </Paper>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default ApplicationDetail;

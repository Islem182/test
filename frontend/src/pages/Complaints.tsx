import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  TextField,
  Button,
  Alert,
  Accordion,
  AccordionSummary,
  AccordionDetails,
} from '@mui/material';
import { ExpandMore as ExpandIcon, Send as SendIcon } from '@mui/icons-material';
import { StatusBadge } from '../components/StatusBadge';
import { complaintService } from '../services/complaint.service';
import { Complaint } from '../types';

export const Complaints: React.FC = () => {
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  const [statusMsg, setStatusMsg] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const fetchComplaints = async () => {
    try {
      const data = await complaintService.getComplaints();
      setComplaints(data);
    } catch {
      setComplaints([
        {
          id: 1,
          taxpayerId: 1,
          taxpayerName: 'Mohamed Ben Ali',
          subject: 'Demande de vérification de document CIN',
          message: 'Bonjour, j\'ai téléversé ma carte CIN mais le statut est toujours en attente.',
          status: 'RESOLVED',
          response: 'Bonjour, votre pièce CIN a été vérifiée par notre agent fiscal. Votre dossier est validé.',
          createdAt: new Date(Date.now() - 86400000 * 2).toISOString(),
        },
      ]);
    }
  };

  useEffect(() => {
    fetchComplaints();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setStatusMsg('');
    setError('');
    setLoading(true);

    try {
      await complaintService.createComplaint(subject, message);
      setStatusMsg('Votre réclamation a été soumise au service fiscal avec succès.');
      setSubject('');
      setMessage('');
      fetchComplaints();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Échec lors de la soumission de la réclamation.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 1 }}>
        Réclamations & Assistance Fiscale
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Transmettez vos demandes particulières ou réclamations directement aux agents de la DGI
      </Typography>

      {statusMsg && (
        <Alert severity="success" sx={{ mb: 3, borderRadius: 2 }}>
          {statusMsg}
        </Alert>
      )}
      {error && (
        <Alert severity="error" sx={{ mb: 3, borderRadius: 2 }}>
          {error}
        </Alert>
      )}

      <Grid container spacing={3}>
        {/* Create Complaint Form */}
        <Grid size={{ xs: 12, md: 5 }}>
          <Card variant="outlined" sx={{ borderRadius: 3 }}>
            <CardContent component="form" onSubmit={handleSubmit}>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Nouvelle Réclamation
              </Typography>
              <TextField
                fullWidth
                label="Sujet de la réclamation *"
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                required
                margin="normal"
              />
              <TextField
                fullWidth
                label="Détail du message *"
                multiline
                rows={4}
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                required
                margin="normal"
              />
              <Button
                type="submit"
                variant="contained"
                startIcon={<SendIcon />}
                disabled={loading}
                sx={{ mt: 2, fontWeight: 700 }}
              >
                Envoyer la Réclamation
              </Button>
            </CardContent>
          </Card>
        </Grid>

        {/* Complaint History & Replies */}
        <Grid size={{ xs: 12, md: 7 }}>
          <Card variant="outlined" sx={{ borderRadius: 3 }}>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Historique de vos Réclamations ({complaints.length})
              </Typography>

              {complaints.map((c) => (
                <Accordion key={c.id} variant="outlined" sx={{ mb: 1, borderRadius: 2, '&:before': { display: 'none' } }}>
                  <AccordionSummary expandIcon={<ExpandIcon />}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', width: '100%', alignItems: 'center', pr: 2 }}>
                      <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                        {c.subject}
                      </Typography>
                      <StatusBadge status={c.status} size="small" />
                    </Box>
                  </AccordionSummary>
                  <AccordionDetails>
                    <Typography variant="body2" sx={{ color: 'text.secondary', mb: 2 }}>
                      {c.message}
                    </Typography>

                    {c.response ? (
                      <Box sx={{ p: 2, bgcolor: 'action.hover', borderRadius: 2, borderLeft: '4px solid #059669' }}>
                        <Typography variant="caption" color="secondary.main" sx={{ fontWeight: 700, display: 'block', mb: 0.5 }}>
                          Réponse de l'Agent DGI:
                        </Typography>
                        <Typography variant="body2">{c.response}</Typography>
                      </Box>
                    ) : (
                      <Typography variant="caption" color="text.disabled" sx={{ display: 'block', fontStyle: 'italic' }}>
                        En cours de traitement par un agent fiscal.
                      </Typography>
                    )}
                  </AccordionDetails>
                </Accordion>
              ))}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

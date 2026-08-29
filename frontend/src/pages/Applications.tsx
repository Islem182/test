import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  TextField,
  InputAdornment,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  TablePagination,
  Button,
} from '@mui/material';
import { Search as SearchIcon, Visibility as ViewIcon } from '@mui/icons-material';
import { StatusBadge } from '../components/StatusBadge';
import { applicationService } from '../services/application.service';
import { TaxApplication } from '../types';

export const Applications: React.FC = () => {
  const [applications, setApplications] = useState<TaxApplication[]>([]);
  const [filteredApps, setFilteredApps] = useState<TaxApplication[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(5);

  useEffect(() => {
    const fetchApplications = async () => {
      try {
        const apps = await applicationService.getApplicationsStatus();
        setApplications(apps);
        setFilteredApps(apps);
      } catch {
        const mock: TaxApplication[] = [
          {
            id: 1042,
            taxpayerId: 1,
            taxpayerName: 'Mohamed Ben Ali',
            status: 'APPROVED',
            createdDate: '2026-07-20T10:30:00Z',
            updatedDate: '2026-07-22T14:15:00Z',
            activities: [{ businessName: 'DGI Tech Solutions SARL', sector: 'IT Services' }],
            documents: [],
          },
          {
            id: 1043,
            taxpayerId: 1,
            taxpayerName: 'Mohamed Ben Ali',
            status: 'PENDING',
            createdDate: '2026-08-01T09:00:00Z',
            updatedDate: '2026-08-01T09:00:00Z',
            activities: [{ businessName: 'Atlas Consulting', sector: 'Finance' }],
            documents: [],
          },
        ];
        setApplications(mock);
        setFilteredApps(mock);
      }
    };

    fetchApplications();
  }, []);

  useEffect(() => {
    let result = applications;

    if (searchTerm) {
      result = result.filter(
        (app) =>
          app.id.toString().includes(searchTerm) ||
          app.activities.some((a) => a.businessName.toLowerCase().includes(searchTerm.toLowerCase()))
      );
    }

    if (statusFilter !== 'ALL') {
      result = result.filter((app) => app.status === statusFilter);
    }

    setFilteredApps(result);
    setPage(0);
  }, [searchTerm, statusFilter, applications]);

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 1 }}>
        Mes Dossiers d'Immatriculation
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Suivi en temps réel de vos demandes d'immatriculation fiscale
      </Typography>

      <Card variant="outlined" sx={{ borderRadius: 3, mb: 3 }}>
        <CardContent sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center' }}>
          <TextField
            size="small"
            placeholder="Rechercher par N° dossier ou Raison Sociale..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            sx={{ flexGrow: 1, minWidth: 260 }}
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon color="action" />
                  </InputAdornment>
                ),
              },
            }}
          />

          <FormControl size="small" sx={{ minWidth: 180 }}>
            <InputLabel>Filtrer par Statut</InputLabel>
            <Select
              value={statusFilter}
              label="Filtrer par Statut"
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <MenuItem value="ALL">Tous les statuts</MenuItem>
              <MenuItem value="PENDING">PENDING (En attente)</MenuItem>
              <MenuItem value="IN_REVIEW">IN_REVIEW (En examen)</MenuItem>
              <MenuItem value="APPROVED">APPROVED (Approuvé)</MenuItem>
              <MenuItem value="REJECTED">REJECTED (Rejeté)</MenuItem>
            </Select>
          </FormControl>
        </CardContent>
      </Card>

      <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 3 }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 700 }}>N° Dossier</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Raison Sociale / Entreprise</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Date de Soumission</TableCell>
              <TableCell sx={{ fontWeight: 700 }}>Statut</TableCell>
              <TableCell sx={{ fontWeight: 700, textAlign: 'center' }}>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredApps
              .slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage)
              .map((app) => (
                <TableRow key={app.id} hover>
                  <TableCell sx={{ fontWeight: 700 }}>#{app.id}</TableCell>
                  <TableCell>
                    {app.activities.length > 0 ? app.activities[0].businessName : 'N/A'}
                  </TableCell>
                  <TableCell>{new Date(app.createdDate).toLocaleDateString()}</TableCell>
                  <TableCell>
                    <StatusBadge status={app.status} />
                  </TableCell>
                  <TableCell sx={{ textAlign: 'center' }}>
                    <Button size="small" startIcon={<ViewIcon />}>
                      Détails
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
          </TableBody>
        </Table>
        <TablePagination
          rowsPerPageOptions={[5, 10, 25]}
          component="div"
          count={filteredApps.length}
          rowsPerPage={rowsPerPage}
          page={page}
          onPageChange={(_, newPage) => setPage(newPage)}
          onRowsPerPageChange={(e) => setRowsPerPage(parseInt(e.target.value, 10))}
        />
      </TableContainer>
    </Box>
  );
};

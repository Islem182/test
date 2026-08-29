import React, { useState } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Button,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Divider,
} from '@mui/material';
import {
  Description as FileIcon,
  Download as DownloadIcon,
  Verified as VerifiedIcon,
} from '@mui/icons-material';
import { FileUploader } from '../components/FileUploader';
import { useAuth } from '../context/AuthContext';
import { DocumentItem } from '../types';

export const Documents: React.FC = () => {
  const { user } = useAuth();
  const [userDocs, setUserDocs] = useState<DocumentItem[]>([
    {
      id: 1,
      type: 'CIN_COPY',
      fileName: 'CIN_Mohamed_BenAli.pdf',
      filePath: '/uploads/cin_sample.pdf',
      fileSize: '1.4 MB',
      uploadDate: new Date().toISOString(),
      verificationStatus: 'VERIFIED',
    },
    {
      id: 2,
      type: 'COMMERCIAL_REGISTER',
      fileName: 'Registre_Commerce_DGI.pdf',
      filePath: '/uploads/rc_sample.pdf',
      fileSize: '2.8 MB',
      uploadDate: new Date().toISOString(),
      verificationStatus: 'VERIFIED',
    },
  ]);

  const handleUpload = (doc: DocumentItem) => {
    setUserDocs((prev) => [doc, ...prev]);
  };

  const handleDownloadCertificate = () => {
    window.open(`http://localhost:8082/api/documents/certificate/${user?.taxpayerId || 1}`, '_blank');
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3, flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h5" sx={{ fontWeight: 700 }}>
            Gestion des Documents & Attestations
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Téléversez vos pièces justificatives et téléchargez vos attestations officielles
          </Typography>
        </Box>

        <Button
          variant="contained"
          startIcon={<DownloadIcon />}
          onClick={handleDownloadCertificate}
          sx={{ fontWeight: 700 }}
        >
          Télécharger Attestation Matricule Fiscal (PDF)
        </Button>
      </Box>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 5 }}>
          <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
            Nouveau Téléversement
          </Typography>
          <FileUploader onFileUpload={handleUpload} />
        </Grid>

        <Grid size={{ xs: 12, md: 7 }}>
          <Card variant="outlined" sx={{ borderRadius: 3 }}>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                Mes Documents Archivés ({userDocs.length})
              </Typography>
              <Divider sx={{ mb: 2 }} />

              <List disablePadding>
                {userDocs.map((doc, i) => (
                  <ListItem
                    key={i}
                    divider
                    secondaryAction={
                      <Button size="small" startIcon={<DownloadIcon />} href={doc.filePath} download>
                        Télécharger
                      </Button>
                    }
                  >
                    <ListItemIcon>
                      <FileIcon color="primary" />
                    </ListItemIcon>
                    <ListItemText
                      primary={
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                            {doc.fileName || doc.type}
                          </Typography>
                          {doc.verificationStatus === 'VERIFIED' && (
                            <VerifiedIcon color="success" fontSize="small" />
                          )}
                        </Box>
                      }
                      secondary={`Type: ${doc.type} • Taille: ${doc.fileSize || '1.2 MB'}`}
                    />
                  </ListItem>
                ))}
              </List>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

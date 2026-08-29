import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  List,
  ListItem,
  ListItemText,
  Divider,
  IconButton,
  Chip,
} from '@mui/material';
import { CheckCircle as ReadIcon } from '@mui/icons-material';
import { apiClient } from '../api/axios';
import type { NotificationItem, ApiResponse } from '../types';

export const Notifications: React.FC = () => {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);

  useEffect(() => {
    const fetchNotifications = async () => {
      try {
        const res = await apiClient.get<ApiResponse<NotificationItem[]>>('/api/notifications');
        if (res.data?.data) {
          setNotifications(res.data.data);
        }
      } catch {
        setNotifications([
          {
            id: 1,
            title: 'Dossier d\'Immatriculation Reçu',
            message: 'Votre dossier d\'immatriculation fiscale #1042 a été bien reçu par nos services.',
            readStatus: true,
            createdAt: new Date().toISOString(),
          },
          {
            id: 2,
            title: 'Matricule Fiscal Généré !',
            message: 'Félicitations! Votre matricule fiscal officiel 1548962/A/M/000 a été généré.',
            readStatus: false,
            createdAt: new Date(Date.now() - 3600000).toISOString(),
          },
        ]);
      }
    };

    fetchNotifications();
  }, []);

  const handleMarkAsRead = async (id: number) => {
    try {
      await apiClient.put(`/api/notifications/${id}/read`);
    } catch {
      // ignore
    }
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, readStatus: true } : n))
    );
  };

  return (
    <Box sx={{ maxWidth: '900px' }}>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 1 }}>
        Centre de Notifications
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Toutes vos alertes et notifications système envoyées par la Direction Générale des Impôts
      </Typography>

      <Card variant="outlined" sx={{ borderRadius: 3 }}>
        <CardContent sx={{ p: 0 }}>
          <List disablePadding>
            {notifications.map((n) => (
              <React.Fragment key={n.id}>
                <ListItem
                  sx={{
                    p: 2.5,
                    bgcolor: n.readStatus ? 'transparent' : 'action.hover',
                  }}
                  secondaryAction={
                    !n.readStatus && (
                      <IconButton onClick={() => handleMarkAsRead(n.id)} color="primary">
                        <ReadIcon />
                      </IconButton>
                    )
                  }
                >
                  <ListItemText
                    primary={
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                        <Typography variant="subtitle1" sx={{ fontWeight: n.readStatus ? 600 : 800 }}>
                          {n.title}
                        </Typography>
                        {!n.readStatus && <Chip label="Nouveau" size="small" color="primary" sx={{ height: 20 }} />}
                      </Box>
                    }
                    secondary={
                      <>
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                          {n.message}
                        </Typography>
                        <Typography variant="caption" color="text.disabled">
                          {new Date(n.createdAt).toLocaleString()}
                        </Typography>
                      </>
                    }
                  />
                </ListItem>
                <Divider />
              </React.Fragment>
            ))}
          </List>
        </CardContent>
      </Card>
    </Box>
  );
};

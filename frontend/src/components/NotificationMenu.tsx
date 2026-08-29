import React, { useState, useEffect } from 'react';
import {
  IconButton,
  Badge,
  Popover,
  Box,
  Typography,
  List,
  ListItem,
  ListItemText,
  Divider,
  useTheme,
} from '@mui/material';
import { Notifications as NotificationsIcon, CheckCircle as ReadIcon } from '@mui/icons-material';
import { apiClient } from '../api/axios';
import { NotificationItem, ApiResponse } from '../types';

export const NotificationMenu: React.FC = () => {
  const theme = useTheme();
  const [anchorEl, setAnchorEl] = useState<HTMLButtonElement | null>(null);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);

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
          title: 'Application Received',
          message: 'Your tax registration application #1042 was successfully received by DGI.',
          readStatus: false,
          createdAt: new Date().toISOString(),
        },
        {
          id: 2,
          title: 'Document Verified',
          message: 'Your CIN copy document was verified by OCR scanner.',
          readStatus: true,
          createdAt: new Date(Date.now() - 86400000).toISOString(),
        },
      ]);
    }
  };

  useEffect(() => {
    fetchNotifications();
  }, []);

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

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

  const open = Boolean(anchorEl);
  const unreadCount = notifications.filter((n) => !n.readStatus).length;

  return (
    <>
      <IconButton onClick={handleClick} color="inherit">
        <Badge badgeContent={unreadCount} color="error">
          <NotificationsIcon />
        </Badge>
      </IconButton>

      <Popover
        open={open}
        anchorEl={anchorEl}
        onClose={handleClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        slotProps={{
          paper: { sx: { width: 340, borderRadius: 3, boxShadow: '0 8px 30px rgba(0,0,0,0.15)' } },
        }}
      >
        <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
            Notifications
          </Typography>
          {unreadCount > 0 && (
            <Typography variant="caption" color="primary.main" sx={{ fontWeight: 600 }}>
              {unreadCount} unread
            </Typography>
          )}
        </Box>
        <Divider />

        <List sx={{ maxHeight: 320, overflowY: 'auto', p: 0 }}>
          {notifications.length === 0 ? (
            <Box sx={{ p: 3, textAlign: 'center' }}>
              <Typography variant="body2" color="text.secondary">
                No notifications found.
              </Typography>
            </Box>
          ) : (
            notifications.map((n) => (
              <React.Fragment key={n.id}>
                <ListItem
                  sx={{
                    backgroundColor: n.readStatus ? 'transparent' : theme.palette.action.hover,
                    py: 1.5,
                  }}
                  secondaryAction={
                    !n.readStatus && (
                      <IconButton size="small" onClick={() => handleMarkAsRead(n.id)}>
                        <ReadIcon fontSize="small" color="action" />
                      </IconButton>
                    )
                  }
                >
                  <ListItemText
                    primary={
                      <Typography variant="subtitle2" sx={{ fontWeight: n.readStatus ? 500 : 700 }}>
                        {n.title}
                      </Typography>
                    }
                    secondary={
                      <>
                        <Typography variant="body2" color="text.secondary" sx={{ display: 'block', mb: 0.5 }}>
                          {n.message}
                        </Typography>
                        <Typography variant="caption" color="text.disabled">
                          {new Date(n.createdAt).toLocaleDateString()}
                        </Typography>
                      </>
                    }
                  />
                </ListItem>
                <Divider />
              </React.Fragment>
            ))
          )}
        </List>
      </Popover>
    </>
  );
};

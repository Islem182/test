import React, { useState } from 'react';
import {
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  Box,
  Avatar,
  Menu,
  MenuItem,
  ListItemIcon,
  Tooltip,
  useTheme,
} from '@mui/material';
import {
  LightMode as LightModeIcon,
  DarkMode as DarkModeIcon,
  Person as PersonIcon,
  Logout as LogoutIcon,
  Menu as MenuIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useColorMode } from '../context/ThemeContext';
import { NotificationMenu } from './NotificationMenu';

interface TopbarProps {
  onToggleMobileSidebar?: () => void;
}

export const Topbar: React.FC<TopbarProps> = ({ onToggleMobileSidebar }) => {
  const theme = useTheme();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const { mode, toggleTheme } = useColorMode();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const handleOpenMenu = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleCloseMenu = () => {
    setAnchorEl(null);
  };

  const handleLogout = async () => {
    handleCloseMenu();
    await logout();
    navigate('/login');
  };

  return (
    <AppBar
      position="sticky"
      elevation={0}
      sx={{
        backgroundColor: theme.palette.background.paper,
        color: theme.palette.text.primary,
        borderBottom: `1px solid ${theme.palette.divider}`,
      }}
    >
      <Toolbar sx={{ justifyContent: 'space-between', px: { xs: 2, sm: 3 } }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {onToggleMobileSidebar && (
            <IconButton onClick={onToggleMobileSidebar} sx={{ display: { md: 'none' } }}>
              <MenuIcon />
            </IconButton>
          )}
          <Typography variant="h6" color="primary.main" sx={{ fontWeight: 700 }}>
            DGI Online Portal
          </Typography>
        </Box>

        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          {/* Theme Mode Toggle */}
          <Tooltip title={`Switch to ${mode === 'light' ? 'Dark' : 'Light'} Mode`}>
            <IconButton onClick={toggleTheme} color="inherit">
              {mode === 'light' ? <DarkModeIcon /> : <LightModeIcon />}
            </IconButton>
          </Tooltip>

          {/* Notifications Menu */}
          <NotificationMenu />

          {/* User Profile Avatar */}
          <Tooltip title="Account options">
            <IconButton onClick={handleOpenMenu} sx={{ p: 0.5 }}>
              <Avatar
                sx={{
                  width: 36,
                  height: 36,
                  backgroundColor: theme.palette.primary.main,
                  fontSize: '0.9rem',
                  fontWeight: 700,
                }}
              >
                {user?.firstName ? user.firstName.charAt(0).toUpperCase() : user?.username?.charAt(0).toUpperCase() || 'U'}
              </Avatar>
            </IconButton>
          </Tooltip>

          <Menu
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleCloseMenu}
            transformOrigin={{ horizontal: 'right', vertical: 'top' }}
            anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
            slotProps={{
              paper: {
                sx: { width: 200, borderRadius: 2, mt: 1, boxShadow: '0 8px 24px rgba(0,0,0,0.12)' },
              },
            }}
          >
            <MenuItem onClick={() => { handleCloseMenu(); navigate('/profile'); }}>
              <ListItemIcon>
                <PersonIcon fontSize="small" />
              </ListItemIcon>
              Profile Settings
            </MenuItem>
            <MenuItem onClick={handleLogout} sx={{ color: theme.palette.error.main }}>
              <ListItemIcon sx={{ color: theme.palette.error.main }}>
                <LogoutIcon fontSize="small" />
              </ListItemIcon>
              Logout
            </MenuItem>
          </Menu>
        </Box>
      </Toolbar>
    </AppBar>
  );
};

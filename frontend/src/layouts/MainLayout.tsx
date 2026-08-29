import React, { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Box, AppBar, Toolbar, IconButton, Typography, Badge, Avatar, Menu, MenuItem, Divider, useMediaQuery, useTheme, Drawer } from '@mui/material';
import {
  Menu as MenuIcon,
  Notifications as NotificationsIcon,
  DarkMode as DarkModeIcon,
  LightMode as LightModeIcon,
  AccountCircle,
  Settings as SettingsIcon,
  Logout as LogoutIcon,
} from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';
import { useColorMode } from '../context/ThemeContext';
import { useNavigate } from 'react-router-dom';
import { Sidebar } from '../components/Sidebar';

const DRAWER_WIDTH = 260;

export const MainLayout: React.FC = () => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const { user, logout } = useAuth();
  const { mode, toggleTheme } = useColorMode();
  const navigate = useNavigate();

  const [mobileOpen, setMobileOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const handleProfileMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = async () => {
    handleMenuClose();
    await logout();
    navigate('/login');
  };

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      {/* Desktop Sidebar */}
      {!isMobile && <Sidebar drawerWidth={DRAWER_WIDTH} />}

      {/* Mobile Drawer */}
      {isMobile && (
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{
            '& .MuiDrawer-paper': {
              width: DRAWER_WIDTH,
              boxSizing: 'border-box',
            },
          }}
        >
          <Sidebar drawerWidth={DRAWER_WIDTH} />
        </Drawer>
      )}

      {/* Main Content */}
      <Box sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column' }}>
        {/* Top Bar */}
        <AppBar
          position="sticky"
          elevation={0}
          sx={{
            backgroundColor: theme.palette.background.paper,
            borderBottom: `1px solid ${theme.palette.divider}`,
            color: theme.palette.text.primary,
          }}
        >
          <Toolbar sx={{ gap: 1 }}>
            {isMobile && (
              <IconButton edge="start" onClick={() => setMobileOpen(!mobileOpen)}>
                <MenuIcon />
              </IconButton>
            )}

            <Box sx={{ flexGrow: 1 }} />

            <IconButton onClick={toggleTheme} color="inherit">
              {mode === 'dark' ? <LightModeIcon /> : <DarkModeIcon />}
            </IconButton>

            <IconButton color="inherit">
              <Badge badgeContent={3} color="error">
                <NotificationsIcon />
              </Badge>
            </IconButton>

            <IconButton onClick={handleProfileMenuOpen} color="inherit" sx={{ ml: 1 }}>
              <Avatar
                sx={{
                  width: 36,
                  height: 36,
                  bgcolor: theme.palette.primary.main,
                  fontSize: '0.9rem',
                  fontWeight: 700,
                }}
              >
                {user?.firstName?.charAt(0)?.toUpperCase() || user?.username?.charAt(0)?.toUpperCase() || 'U'}
              </Avatar>
            </IconButton>

            <Menu
              anchorEl={anchorEl}
              open={Boolean(anchorEl)}
              onClose={handleMenuClose}
              transformOrigin={{ horizontal: 'right', vertical: 'top' }}
              anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
              slotProps={{ paper: { sx: { mt: 1, minWidth: 200, borderRadius: 2 } } }}
            >
              <Box sx={{ px: 2, py: 1.5 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                  {user?.firstName && user?.lastName ? `${user.firstName} ${user.lastName}` : user?.username}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {user?.email}
                </Typography>
              </Box>
              <Divider />
              <MenuItem onClick={() => { handleMenuClose(); navigate('/profile'); }}>
                <AccountCircle sx={{ mr: 1.5, fontSize: 20 }} />
                Profile
              </MenuItem>
              <MenuItem onClick={() => { handleMenuClose(); navigate('/profile'); }}>
                <SettingsIcon sx={{ mr: 1.5, fontSize: 20 }} />
                Settings
              </MenuItem>
              <Divider />
              <MenuItem onClick={handleLogout} sx={{ color: theme.palette.error.main }}>
                <LogoutIcon sx={{ mr: 1.5, fontSize: 20 }} />
                Logout
              </MenuItem>
            </Menu>
          </Toolbar>
        </AppBar>

        {/* Page Content */}
        <Box
          component="main"
          sx={{
            flexGrow: 1,
            p: { xs: 2, sm: 3 },
            backgroundColor: theme.palette.background.default,
            overflow: 'auto',
          }}
        >
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
};

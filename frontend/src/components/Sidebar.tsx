import React from 'react';
import {
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Divider,
  Box,
  Typography,
  Chip,
  useTheme,
} from '@mui/material';
import {
  Dashboard as DashboardIcon,
  Person as PersonIcon,
  AppRegistration as RegistrationIcon,
  Assignment as ApplicationsIcon,
  Description as DocumentsIcon,
  Feedback as ComplaintsIcon,
  Notifications as NotificationsIcon,
  SmartToy as ChatbotIcon,
  Logout as LogoutIcon,
  AdminPanelSettings as AdminIcon,
  SupportAgent as AgentIcon,
  Analytics as StatsIcon,
  People as UsersIcon,
} from '@mui/icons-material';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export interface SidebarProps {
  drawerWidth?: number;
  openChatbot?: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ drawerWidth = 260, openChatbot }) => {
  const theme = useTheme();
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout, hasRole } = useAuth();

  const isTaxpayer = hasRole('TAXPAYER');
  const isAgent = hasRole('AGENT');
  const isAdmin = hasRole('ADMIN');

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const isActive = (path: string) => location.pathname === path;

  const renderNavGroup = (title: string, items: { label: string; icon: React.ReactNode; path?: string; onClick?: () => void; badge?: string }[]) => (
    <Box sx={{ mb: 2 }}>
      <Typography
        variant="caption"
        sx={{
          px: 3,
          py: 1,
          display: 'block',
          fontWeight: 700,
          color: theme.palette.text.secondary,
          letterSpacing: 1.1,
          textTransform: 'uppercase',
          fontSize: '0.7rem',
        }}
      >
        {title}
      </Typography>
      <List disablePadding>
        {items.map((item, index) => {
          const selected = item.path ? isActive(item.path) : false;
          return (
            <ListItem key={index} disablePadding sx={{ px: 1.5, py: 0.3 }}>
              <ListItemButton
                selected={selected}
                onClick={item.onClick ? item.onClick : () => item.path && navigate(item.path)}
                sx={{
                  borderRadius: '10px',
                  '&.Mui-selected': {
                    backgroundColor: theme.palette.primary.main,
                    color: '#ffffff',
                    '& .MuiListItemIcon-root': {
                      color: '#ffffff',
                    },
                    '&:hover': {
                      backgroundColor: theme.palette.primary.dark,
                    },
                  },
                }}
              >
                <ListItemIcon sx={{ minWidth: 40, color: selected ? '#ffffff' : theme.palette.text.secondary }}>
                  {item.icon}
                </ListItemIcon>
                <ListItemText
                  primary={item.label}
                  slotProps={{
                    primary: {
                      sx: {
                        fontSize: '0.9rem',
                        fontWeight: selected ? 600 : 500,
                      },
                    },
                  }}
                />
                {item.badge && (
                  <Chip
                    label={item.badge}
                    size="small"
                    color="secondary"
                    sx={{ height: 20, fontSize: '0.68rem', fontWeight: 700 }}
                  />
                )}
              </ListItemButton>
            </ListItem>
          );
        })}
      </List>
    </Box>
  );

  return (
    <Drawer
      variant="permanent"
      sx={{
        width: drawerWidth,
        flexShrink: 0,
        '& .MuiDrawer-paper': {
          width: drawerWidth,
          boxSizing: 'border-box',
          borderRight: `1px solid ${theme.palette.divider}`,
          backgroundColor: theme.palette.background.paper,
        },
      }}
    >
      {/* Brand Header */}
      <Box
        sx={{
          p: 2.5,
          display: 'flex',
          alignItems: 'center',
          gap: 1.5,
          borderBottom: `1px solid ${theme.palette.divider}`,
        }}
      >
        <Box
          sx={{
            width: 42,
            height: 42,
            borderRadius: '10px',
            backgroundColor: theme.palette.primary.main,
            color: '#fff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontWeight: 800,
            fontSize: '1.2rem',
            boxShadow: '0 4px 12px rgba(15, 76, 129, 0.4)',
          }}
        >
          DGI
        </Box>
        <Box>
          <Typography variant="subtitle1" sx={{ fontWeight: 700, lineHeight: 1.2 }}>
            DGI Platform
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Direction Générale des Impôts
          </Typography>
        </Box>
      </Box>

      {/* Navigation Links */}
      <Box sx={{ overflowY: 'auto', flex: 1, py: 2 }}>
        {/* Taxpayer Menu */}
        {(isTaxpayer || (!isAgent && !isAdmin)) &&
          renderNavGroup('Taxpayer Services', [
            { label: 'Dashboard', icon: <DashboardIcon />, path: '/dashboard' },
            { label: 'Profile', icon: <PersonIcon />, path: '/profile' },
            { label: 'New Registration', icon: <RegistrationIcon />, path: '/registration/new' },
            { label: 'My Applications', icon: <ApplicationsIcon />, path: '/applications' },
            { label: 'Tax Documents', icon: <DocumentsIcon />, path: '/documents' },
            { label: 'Complaints', icon: <ComplaintsIcon />, path: '/complaints' },
            { label: 'Notifications', icon: <NotificationsIcon />, path: '/notifications' },
            { label: 'Assistant AI', icon: <ChatbotIcon />, onClick: openChatbot, badge: 'AI' },
          ])}

        {/* Agent Menu */}
        {isAgent &&
          renderNavGroup('Agent Portal', [
            { label: 'Agent Dashboard', icon: <AgentIcon />, path: '/agent/dashboard' },
            { label: 'Applications Queue', icon: <ApplicationsIcon />, path: '/agent/applications' },
            { label: 'Taxpayer Complaints', icon: <ComplaintsIcon />, path: '/complaints' },
          ])}

        {/* Admin Menu */}
        {isAdmin &&
          renderNavGroup('Administration', [
            { label: 'Admin Dashboard', icon: <AdminIcon />, path: '/admin/dashboard' },
            { label: 'User Management', icon: <UsersIcon />, path: '/admin/users' },
            { label: 'Agents Roster', icon: <AgentIcon />, path: '/admin/agents' },
            { label: 'System Statistics', icon: <StatsIcon />, path: '/admin/statistics' },
          ])}
      </Box>

      <Divider />

      {/* Bottom User Info & Logout */}
      <Box sx={{ p: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1.5 }}>
          <Box
            sx={{
              width: 38,
              height: 38,
              borderRadius: '50%',
              backgroundColor: theme.palette.primary.light,
              color: '#fff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 700,
            }}
          >
            {user?.firstName ? user.firstName.charAt(0).toUpperCase() : user?.username?.charAt(0).toUpperCase() || 'U'}
          </Box>
          <Box sx={{ overflow: 'hidden' }}>
            <Typography variant="subtitle2" noWrap sx={{ fontWeight: 600 }}>
              {user?.firstName && user?.lastName ? `${user.firstName} ${user.lastName}` : user?.username}
            </Typography>
            <Typography variant="caption" color="text.secondary" noWrap sx={{ display: 'block' }}>
              {user?.email}
            </Typography>
          </Box>
        </Box>

        <ListItemButton
          onClick={handleLogout}
          sx={{
            borderRadius: '8px',
            color: theme.palette.error.main,
            '&:hover': {
              backgroundColor: theme.palette.error.light + '15',
            },
          }}
        >
          <ListItemIcon sx={{ minWidth: 36, color: theme.palette.error.main }}>
            <LogoutIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText
            primary="Logout"
            slotProps={{
              primary: { sx: { fontSize: '0.875rem', fontWeight: 600 } },
            }}
          />
        </ListItemButton>
      </Box>
    </Drawer>
  );
};

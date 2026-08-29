import React, { useState } from 'react';
import { Box, useTheme, useMediaQuery } from '@mui/material';
import { Outlet } from 'react-router-dom';
import { Sidebar } from '../components/Sidebar';
import { Topbar } from '../components/Topbar';
import { ChatbotModal } from '../components/ChatbotModal';

export const DashboardLayout: React.FC = () => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [mobileOpen, setMobileOpen] = useState(false);
  const [chatbotOpen, setChatbotOpen] = useState(false);

  const drawerWidth = 260;

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', backgroundColor: theme.palette.background.default }}>
      {/* Sidebar for Desktop & Mobile */}
      <Sidebar
        drawerWidth={drawerWidth}
        openChatbot={() => setChatbotOpen(true)}
      />

      {/* Main Content Area */}
      <Box
        sx={{
          flexGrow: 1,
          width: { md: `calc(100% - ${drawerWidth}px)` },
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <Topbar onToggleMobileSidebar={() => setMobileOpen(!mobileOpen)} />

        <Box component="main" sx={{ flexGrow: 1, p: { xs: 2, sm: 3, md: 4 } }}>
          <Outlet />
        </Box>
      </Box>

      {/* Global AI Chatbot Dialog */}
      <ChatbotModal open={chatbotOpen} onClose={() => setChatbotOpen(false)} />
    </Box>
  );
};

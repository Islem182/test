import React, { useEffect, useState } from 'react';
import {
  Box, Card, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Chip, CircularProgress, Avatar,
} from '@mui/material';
import { SupportAgent } from '@mui/icons-material';
import { userService } from '../../services/user.service';
import type { UserProfile } from '../../types';

const AdminAgents: React.FC = () => {
  const [agents, setAgents] = useState<UserProfile[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetch = async () => {
      try {
        const users = await userService.getAllUsers();
        const agentList = (users || []).filter((u) => u.isAdmin);
        setAgents(agentList);
      } catch { setAgents([]); }
      finally { setLoading(false); }
    };
    fetch();
  }, []);

  return (
    <Box>
      <Typography variant="h4" sx={{ fontWeight: 700, mb: 3 }}>Agents Roster</Typography>

      <Card>
        <TableContainer>
          {loading ? (
            <Box sx={{ textAlign: 'center', py: 4 }}><CircularProgress /></Box>
          ) : (
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 700 }}>Agent</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Email</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {agents.length === 0 ? (
                  <TableRow><TableCell colSpan={3} sx={{ textAlign: 'center', py: 4 }}>
                    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 1 }}>
                      <SupportAgent sx={{ fontSize: 40, color: 'text.secondary' }} />
                      <Typography color="text.secondary">No agents found</Typography>
                    </Box>
                  </TableCell></TableRow>
                ) : agents.map((a) => (
                  <TableRow key={a.id} hover>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                        <Avatar sx={{ width: 34, height: 34, bgcolor: 'secondary.main', fontSize: '0.8rem' }}>
                          {a.firstName?.[0]?.toUpperCase() || a.username?.[0]?.toUpperCase() || 'A'}
                        </Avatar>
                        <Typography variant="body2" sx={{ fontWeight: 500 }}>
                          {a.firstName && a.lastName ? `${a.firstName} ${a.lastName}` : a.username}
                        </Typography>
                      </Box>
                    </TableCell>
                    <TableCell>{a.email}</TableCell>
                    <TableCell>
                      <Chip label={a.enabled ? 'Active' : 'Disabled'} size="small"
                        color={a.enabled ? 'success' : 'default'} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </TableContainer>
      </Card>
    </Box>
  );
};

export default AdminAgents;

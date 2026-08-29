import React, { useEffect, useState } from 'react';
import {
  Box, Card, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Chip, CircularProgress, TextField, InputAdornment,
  Avatar,
} from '@mui/material';
import { Search, Person } from '@mui/icons-material';
import { userService } from '../../services/user.service';
import type { UserProfile } from '../../types';

const AdminUsers: React.FC = () => {
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => {
    const fetch = async () => {
      try {
        const data = await userService.getAllUsers();
        setUsers(data || []);
      } catch { setUsers([]); }
      finally { setLoading(false); }
    };
    fetch();
  }, []);

  const filtered = users.filter((u) =>
    u.username?.toLowerCase().includes(search.toLowerCase()) ||
    u.email?.toLowerCase().includes(search.toLowerCase()) ||
    u.firstName?.toLowerCase().includes(search.toLowerCase()) ||
    u.lastName?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <Box>
      <Typography variant="h4" sx={{ fontWeight: 700, mb: 3 }}>User Management</Typography>

      <Card>
        <Box sx={{ p: 2 }}>
          <TextField
            size="small" placeholder="Search users..."
            value={search} onChange={(e) => setSearch(e.target.value)}
            sx={{ maxWidth: 320 }}
            slotProps={{ input: { startAdornment: <InputAdornment position="start"><Search /></InputAdornment> } }}
          />
        </Box>
        <TableContainer>
          {loading ? (
            <Box sx={{ textAlign: 'center', py: 4 }}><CircularProgress /></Box>
          ) : (
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 700 }}>User</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Email</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Roles</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filtered.length === 0 ? (
                  <TableRow><TableCell colSpan={4} sx={{ textAlign: 'center', py: 4 }}>
                    <Typography color="text.secondary">No users found</Typography>
                  </TableCell></TableRow>
                ) : filtered.map((u) => (
                  <TableRow key={u.id} hover>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                        <Avatar sx={{ width: 34, height: 34, bgcolor: 'primary.main', fontSize: '0.8rem' }}>
                          {u.firstName?.[0]?.toUpperCase() || u.username?.[0]?.toUpperCase() || 'U'}
                        </Avatar>
                        <Box>
                          <Typography variant="body2" sx={{ fontWeight: 500 }}>
                            {u.firstName && u.lastName ? `${u.firstName} ${u.lastName}` : u.username}
                          </Typography>
                        </Box>
                      </Box>
                    </TableCell>
                    <TableCell>{u.email}</TableCell>
                    <TableCell>
                      <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                        {u.roles?.map((role, i) => (
                          <Chip key={i} label={role} size="small"
                            color={role.includes('ADMIN') ? 'error' : role.includes('AGENT') ? 'info' : 'default'} />
                        ))}
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Chip label={u.enabled ? 'Active' : 'Disabled'} size="small"
                        color={u.enabled ? 'success' : 'default'} />
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

export default AdminUsers;

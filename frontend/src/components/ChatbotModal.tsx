import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Box,
  Typography,
  TextField,
  IconButton,
  Avatar,
  Paper,
  CircularProgress,
} from '@mui/material';
import {
  Close as CloseIcon,
  Send as SendIcon,
  SmartToy as BotIcon,
  Person as PersonIcon,
} from '@mui/icons-material';
import { apiClient } from '../api/axios';

interface ChatbotModalProps {
  open: boolean;
  onClose: () => void;
}

interface Message {
  sender: 'user' | 'bot';
  text: string;
}

export const ChatbotModal: React.FC<ChatbotModalProps> = ({ open, onClose }) => {
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState<Message[]>([
    {
      sender: 'bot',
      text: 'Bonjour! Je suis l\'Assistant IA Officiel de la Direction Générale des Impôts (DGI). Comment puis-je vous aider aujourd\'hui?',
    },
  ]);

  const handleSend = async () => {
    if (!input.trim() || loading) return;

    const userMsg = input.trim();
    setInput('');
    setMessages((prev) => [...prev, { sender: 'user', text: userMsg }]);
    setLoading(true);

    try {
      const res = await apiClient.post('/api/chatbot/ask', { message: userMsg });
      const botReply = res.data?.data?.reply || res.data?.reply || 'Désolé, une erreur est survenue lors de la réponse.';
      setMessages((prev) => [...prev, { sender: 'bot', text: botReply }]);
    } catch {
      setMessages((prev) => [
        ...prev,
        {
          sender: 'bot',
          text: 'Pour toute demande d\'immatriculation fiscale, veuillez fournir votre numéro CIN et le registre de commerce (Patente).',
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      slotProps={{
        paper: { sx: { borderRadius: 3, height: '80vh' } },
      }}
    >
      <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', p: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <Avatar sx={{ bgcolor: 'primary.main' }}>
            <BotIcon />
          </Avatar>
          <Box>
            <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
              Assistant Fiscal DGI AI
            </Typography>

          </Box>
        </Box>
        <IconButton onClick={onClose} size="small">
          <CloseIcon />
        </IconButton>
      </DialogTitle>

      <DialogContent dividers sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 1.5, overflowY: 'auto' }}>
        {messages.map((msg, index) => (
          <Box
            key={index}
            sx={{
              display: 'flex',
              justifyContent: msg.sender === 'user' ? 'flex-end' : 'flex-start',
              gap: 1,
            }}
          >
            {msg.sender === 'bot' && (
              <Avatar sx={{ width: 32, height: 32, bgcolor: 'primary.main' }}>
                <BotIcon fontSize="small" />
              </Avatar>
            )}
            <Paper
              elevation={0}
              sx={{
                p: 1.5,
                maxWidth: '75%',
                borderRadius: 2,
                bgcolor: msg.sender === 'user' ? 'primary.main' : 'action.hover',
                color: msg.sender === 'user' ? '#ffffff' : 'text.primary',
              }}
            >
              <Typography variant="body2">{msg.text}</Typography>
            </Paper>
            {msg.sender === 'user' && (
              <Avatar sx={{ width: 32, height: 32, bgcolor: 'secondary.main' }}>
                <PersonIcon fontSize="small" />
              </Avatar>
            )}
          </Box>
        ))}
        {loading && (
          <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
            <Avatar sx={{ width: 32, height: 32, bgcolor: 'primary.main' }}>
              <BotIcon fontSize="small" />
            </Avatar>
            <CircularProgress size={20} />
          </Box>
        )}
      </DialogContent>

      <DialogActions sx={{ p: 1.5 }}>
        <TextField
          fullWidth
          size="small"
          placeholder="Posez votre question fiscale..."
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyPress={(e) => e.key === 'Enter' && handleSend()}
        />
        <IconButton color="primary" onClick={handleSend} disabled={loading}>
          <SendIcon />
        </IconButton>
      </DialogActions>
    </Dialog>
  );
};

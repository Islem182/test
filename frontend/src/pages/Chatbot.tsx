import React, { useState, useRef, useEffect } from 'react';
import {
  Box, Card, CardContent, Typography, TextField, Button, Avatar,
  Paper, IconButton,
} from '@mui/material';
import { Send, SmartToy, Person } from '@mui/icons-material';
import type { ChatMessage } from '../types';

const INITIAL_MESSAGES: ChatMessage[] = [
  {
    id: 1,
    sender: 'bot',
    text: 'Hello! I\'m the DGI virtual assistant. How can I help you today? You can ask me about tax registration, application status, or document requirements.',
    timestamp: new Date().toISOString(),
  },
];

const BOT_RESPONSES: Record<string, string> = {
  'registration': 'To register for a fiscal number, go to "New Registration" in the sidebar and follow the multi-step form. You\'ll need your CIN, business details, and supporting documents.',
  'status': 'You can check your application status from the "My Applications" page. Each application shows its current status: Pending, In Review, Approved, or Rejected.',
  'document': 'Accepted documents include: CIN copy, passport copy, commercial register, patente, and tax returns. Files must be in PDF, JPG, or PNG format.',
  'fiscal': 'Your fiscal number is assigned after your registration application is approved. You can find it on your Dashboard once it\'s available.',
  'complaint': 'To submit a complaint, navigate to the "Complaints" page and fill out the form. An agent will review and respond to your complaint.',
  'password': 'You can change your password from the "Profile" page under the "Change Password" tab.',
};

const Chatbot: React.FC = () => {
  const [messages, setMessages] = useState<ChatMessage[]>(INITIAL_MESSAGES);
  const [input, setInput] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const getBotResponse = (text: string): string => {
    const lower = text.toLowerCase();
    for (const [key, response] of Object.entries(BOT_RESPONSES)) {
      if (lower.includes(key)) return response;
    }
    return 'I\'m not sure I understand. Could you try rephrasing? You can ask about registration, application status, documents, fiscal numbers, complaints, or password changes.';
  };

  const handleSend = () => {
    if (!input.trim()) return;

    const userMsg: ChatMessage = {
      id: messages.length + 1,
      sender: 'user',
      text: input.trim(),
      timestamp: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, userMsg]);
    setInput('');

    setTimeout(() => {
      const botMsg: ChatMessage = {
        id: messages.length + 2,
        sender: 'bot',
        text: getBotResponse(input),
        timestamp: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, botMsg]);
    }, 800);
  };

  return (
    <Box>
      <Typography variant="h4" sx={{ fontWeight: 700, mb: 3 }}>AI Assistant</Typography>

      <Card sx={{ height: 'calc(100vh - 220px)', display: 'flex', flexDirection: 'column' }}>
        {/* Messages Area */}
        <Box sx={{ flex: 1, overflow: 'auto', p: 3 }}>
          {messages.map((msg) => (
            <Box
              key={msg.id}
              sx={{
                display: 'flex',
                gap: 1.5,
                mb: 2,
                flexDirection: msg.sender === 'user' ? 'row-reverse' : 'row',
              }}
            >
              <Avatar sx={{
                bgcolor: msg.sender === 'bot' ? 'primary.main' : 'secondary.main',
                width: 36, height: 36,
              }}>
                {msg.sender === 'bot' ? <SmartToy sx={{ fontSize: 20 }} /> : <Person sx={{ fontSize: 20 }} />}
              </Avatar>
              <Paper
                elevation={0}
                sx={{
                  p: 1.5, px: 2,
                  maxWidth: '70%',
                  bgcolor: msg.sender === 'bot' ? 'action.hover' : 'primary.main',
                  color: msg.sender === 'bot' ? 'text.primary' : '#fff',
                  borderRadius: 2,
                }}
              >
                <Typography variant="body2">{msg.text}</Typography>
              </Paper>
            </Box>
          ))}
          <div ref={messagesEndRef} />
        </Box>

        {/* Input Area */}
        <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider', display: 'flex', gap: 1 }}>
          <TextField
            fullWidth
            size="small"
            placeholder="Type your message..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
          />
          <Button
            variant="contained"
            onClick={handleSend}
            disabled={!input.trim()}
            sx={{ minWidth: 48 }}
          >
            <Send />
          </Button>
        </Box>
      </Card>
    </Box>
  );
};

export default Chatbot;

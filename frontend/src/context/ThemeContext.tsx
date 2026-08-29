import React, { createContext, useContext, useState, useMemo } from 'react';
import type { ReactNode } from 'react';
import { ThemeProvider as MuiThemeProvider, createTheme, CssBaseline } from '@mui/material';
import type { PaletteMode } from '@mui/material';

interface ThemeContextType {
  mode: PaletteMode;
  toggleTheme: () => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export const ColorModeProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [mode, setMode] = useState<PaletteMode>(() => {
    const saved = localStorage.getItem('themeMode');
    return (saved as PaletteMode) || 'light';
  });

  const toggleTheme = () => {
    setMode((prev) => {
      const next = prev === 'light' ? 'dark' : 'light';
      localStorage.setItem('themeMode', next);
      return next;
    });
  };

  const theme = useMemo(
    () =>
      createTheme({
        palette: {
          mode,
          primary: {
            main: mode === 'light' ? '#0f4c81' : '#3b82f6',
            light: '#357abd',
            dark: '#0a3258',
            contrastText: '#ffffff',
          },
          secondary: {
            main: '#059669',
            light: '#10b981',
            dark: '#047857',
            contrastText: '#ffffff',
          },
          background: {
            default: mode === 'light' ? '#f4f6f9' : '#0f172a',
            paper: mode === 'light' ? '#ffffff' : '#1e293b',
          },
          text: {
            primary: mode === 'light' ? '#1e293b' : '#f8fafc',
            secondary: mode === 'light' ? '#64748b' : '#94a3b8',
          },
        },
        typography: {
          fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
          h4: {
            fontWeight: 700,
          },
          h5: {
            fontWeight: 600,
          },
          h6: {
            fontWeight: 600,
          },
          button: {
            textTransform: 'none',
            fontWeight: 600,
          },
        },
        shape: {
          borderRadius: 10,
        },
        components: {
          MuiButton: {
            styleOverrides: {
              root: {
                borderRadius: 8,
                padding: '8px 20px',
                boxShadow: 'none',
                '&:hover': {
                  boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                },
              },
            },
          },
          MuiCard: {
            styleOverrides: {
              root: {
                borderRadius: 12,
                boxShadow: mode === 'light' 
                  ? '0 4px 20px 0 rgba(0,0,0,0.05)' 
                  : '0 4px 20px 0 rgba(0,0,0,0.3)',
              },
            },
          },
          MuiPaper: {
            styleOverrides: {
              root: {
                backgroundImage: 'none',
              },
            },
          },
        },
      }),
    [mode]
  );

  return (
    <ThemeContext.Provider value={{ mode, toggleTheme }}>
      <MuiThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </MuiThemeProvider>
    </ThemeContext.Provider>
  );
};

export const useColorMode = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useColorMode must be used within ColorModeProvider');
  }
  return context;
};

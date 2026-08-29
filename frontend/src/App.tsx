import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { ColorModeProvider } from './context/ThemeContext';
import { AuthProvider } from './context/AuthContext';
import { AppRoutes } from './routes/AppRoutes';

function App() {
  return (
    <ColorModeProvider>
      <AuthProvider>
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </AuthProvider>
    </ColorModeProvider>
  );
}

export default App;

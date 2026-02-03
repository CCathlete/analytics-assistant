// src/App.tsx

import React, { useState } from 'react';
import { AuthState, User } from './types';
import LoginForm from './components/LoginForm';
import Dashboard from './components/Dashboard';

const App: React.FC = () => {
  const [state, setState] = useState<AuthState>({ type: 'UNAUTHENTICATED' });

  const handleLoginSuccess = (user: User): void => {
    setState({ type: 'AUTHENTICATED', user });
  };

  return (
    <main className="app-container">
      {state.type === 'AUTHENTICATED' ? (
        <Dashboard user={state.user} />
      ) : (
        <LoginForm 
          onSuccess={handleLoginSuccess} 
          isLoading={state.type === 'AUTHENTICATING'} 
        />
      )}
    </main>
  );
};

export default App;

import React, { useState, FormEvent } from 'react';
import { User } from '../types';

interface LoginFormProps {
  onSuccess: (user: User) => void;
}

const LoginForm: React.FC<LoginFormProps> = ({ onSuccess }) => {
  const [username, setUsername] = useState<string>('');
  const [password, setPassword] = useState<string>('');
  const [isPending, setIsPending] = useState<boolean>(false);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();
    setIsPending(true);

    // Mocking the future LiteLLM/OpenWebUI auth call
    // For now, we just simulate a delay and succeed
    setTimeout(() => {
      const mockUser: User = {
        username: username,
        accessToken: "local-browser-session" // Placeholder
      };
      
      setIsPending(false);
      onSuccess(mockUser);
    }, 500);
  };

  return (
    <div style={pageContainer}>
      <form onSubmit={handleSubmit} style={formCard}>
        <h2 style={{ marginBottom: '1.5rem', textAlign: 'center' }}>Data Platform</h2>
        
        <div style={inputWrapper}>
          <label style={labelStyle}>Username</label>
          <input
            style={inputStyle}
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Ken"
            required
          />
        </div>

        <div style={inputWrapper}>
          <label style={labelStyle}>Password</label>
          <input
            style={inputStyle}
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            required
          />
        </div>

        <button 
          type="submit" 
          disabled={isPending} 
          style={isPending ? {...buttonStyle, opacity: 0.7} : buttonStyle}
        >
          {isPending ? 'Connecting...' : 'Enter Platform'}
        </button>
      </form>
    </div>
  );
};

// Quick Styles for a clean "Data Platform" look
const pageContainer: React.CSSProperties = { display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', background: '#0f172a' };
const formCard: React.CSSProperties = { background: '#ffffff', padding: '2.5rem', borderRadius: '12px', width: '100%', maxWidth: '400px', boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.3)' };
const inputWrapper: React.CSSProperties = { marginBottom: '1rem' };
const labelStyle: React.CSSProperties = { display: 'block', marginBottom: '0.5rem', fontSize: '0.875rem', fontWeight: 600, color: '#475569' };
const inputStyle: React.CSSProperties = { width: '100%', padding: '0.75rem', borderRadius: '6px', border: '1px solid #cbd5e1', boxSizing: 'border-box' };
const buttonStyle: React.CSSProperties = { width: '100%', padding: '0.75rem', background: '#2563eb', color: 'white', border: 'none', borderRadius: '6px', fontWeight: 600, cursor: 'pointer', marginTop: '1rem' };

export default LoginForm;

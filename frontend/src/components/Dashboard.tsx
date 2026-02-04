import React, { useState } from 'react';
import { type User } from '../types.tsx';

interface DashboardProps {
  user: User;
}

const Dashboard: React.FC<DashboardProps> = ({ user }) => {
  const [prompt, setPrompt] = useState<string>("");

  // In production, this URL would be signed or use Guest Tokens for Security
  const SUPERSET_URL = ""; // TODO: needs to be sent from backend.

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      {/* The Big Window (Superset) */}
      <section style={{ flex: 1, border: 'none' }}>
        <iframe
          src={SUPERSET_URL}
          width="100%"
          height="100%"
          title="Apache Superset Explore"
          style={{ border: 'none' }}
        />
      </section>

      {/* The Prompt Window */}
      <footer style={{ height: '200px', padding: '20px', background: '#f5f5f5', borderTop: '1px solid #ccc' }}>
        <textarea
          style={{ width: '100%', height: '100px', borderRadius: '8px', padding: '10px' }}
          placeholder="Enter your prompt for the data platform..."
          value={prompt}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setPrompt(e.target.value)}
        />
        <button 
          onClick={() => console.log("Sending to LiteLLM:", prompt)}
          style={{ marginTop: '10px', padding: '10px 20px', cursor: 'pointer' }}
        >
          Execute
        </button>
      </footer>
    </div>
  );
};

export default Dashboard;

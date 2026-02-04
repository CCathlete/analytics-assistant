import React, { useState } from 'react';
import { type User } from '../types.tsx';

interface DashboardProps {
  user: User;
}

const Dashboard: React.FC<DashboardProps> = ({ user }) => {
  const [prompt, setPrompt] = useState<string>("");
  const [supersetUrl, setSupersetUrl] = useState<string>(""); 
  const [isExecuting, setIsExecuting] = useState<boolean>(false);

  const handleExecute = async (): Promise<void> => {
    if (!prompt.trim()) return;

    setIsExecuting(true);
    try {
      const response = await fetch('/api/v1/charts/generate', {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          // Even if we rely on Chrome cookies, passing the token is good practice for the future
          'Authorization': `Bearer ${user.accessToken}` 
        },
        body: JSON.stringify({ 
            prompt: prompt,
            username: user.username 
        }),
      });

      if (response.ok) {
        const data = await response.json();
        // Assuming the backend returns the URL of the generated chart/dashboard
        if (data.url) {
          setSupersetUrl(data.url);
        }
      } else {
        console.error("Failed to generate chart metadata");
      }
    } catch (error) {
      console.error("Error connecting to generator service:", error);
    } finally {
      setIsExecuting(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', background: '#0f172a' }}>
      {/* The Big Window (Superset) */}
      <section style={{ flex: 1, border: 'none', background: '#ffffff' }}>
        {supersetUrl ? (
          <iframe
            src={supersetUrl}
            width="100%"
            height="100%"
            title="Apache Superset Explore"
            style={{ border: 'none' }}
          />
        ) : (
          <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%', color: '#94a3b8' }}>
            <p>Enter a prompt below to generate a visualization...</p>
          </div>
        )}
      </section>

      {/* The Prompt Window */}
      <footer style={{ height: '220px', padding: '20px', background: '#1e293b', borderTop: '1px solid #334155' }}>
        <textarea
          style={{ 
            width: '100%', 
            height: '100px', 
            borderRadius: '8px', 
            padding: '12px', 
            background: '#334155', 
            color: '#f8fafc',
            border: '1px solid #475569',
            resize: 'none'
          }}
          placeholder="Ask Gemma to analyze data (e.g., 'Show me sales by region from pg_domain_data')"
          value={prompt}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setPrompt(e.target.value)}
          disabled={isExecuting}
        />
        <button 
          onClick={handleExecute}
          disabled={isExecuting || !prompt.trim()}
          style={{ 
            marginTop: '12px', 
            padding: '10px 24px', 
            cursor: isExecuting ? 'not-allowed' : 'pointer',
            background: '#3b82f6',
            color: 'white',
            border: 'none',
            borderRadius: '6px',
            fontWeight: 600,
            opacity: isExecuting ? 0.6 : 1
          }}
        >
          {isExecuting ? 'Generating...' : 'Execute'}
        </button>
      </footer>
    </div>
  );
};

export default Dashboard;

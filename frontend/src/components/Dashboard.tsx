import React, { useState, useCallback, useEffect } from 'react';
import { type User } from '../types.tsx';

interface DashboardProps {
  user: User;
}

const Dashboard: React.FC<DashboardProps> = ({ user }) => {
  const [prompt, setPrompt] = useState<string>("");
  const [supersetUrl, setSupersetUrl] = useState<string>(""); 
  const [isExecuting, setIsExecuting] = useState<boolean>(false);
  
  const [footerHeight, setFooterHeight] = useState<number>(250);
  const [isResizing, setIsResizing] = useState<boolean>(false);

  const startResizing = useCallback(() => setIsResizing(true), []);
  const stopResizing = useCallback(() => setIsResizing(false), []);

  const resize = useCallback(
    (mouseMoveEvent: MouseEvent) => {
      if (isResizing) {
        const newHeight = window.innerHeight - mouseMoveEvent.clientY;
        if (newHeight > 150 && newHeight < 600) {
          setFooterHeight(newHeight);
        }
      }
    },
    [isResizing]
  );

  useEffect(() => {
    window.addEventListener("mousemove", resize);
    window.addEventListener("mouseup", stopResizing);
    return () => {
      window.removeEventListener("mousemove", resize);
      window.removeEventListener("mouseup", stopResizing);
    };
  }, [resize, stopResizing]);

  const handleExecute = async (): Promise<void> => {
    if (!prompt.trim()) return;
    setIsExecuting(true);
    try {
      const response = await fetch('/api/v1/charts/generate', {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${user.accessToken}` 
        },
        body: JSON.stringify({ 
            prompt: prompt,
            modelName: "visualisation-assistant",
            sourceUrls: [""],
            targetDatasetId: 1
        }),
      });

      if (response.ok) {
        const data = await response.json();
        if (data.supersetUrl) {
          setSupersetUrl(data.supersetUrl);
        }
      }
    } catch (error) {
      console.error("Connection error:", error);
    } finally {
      setIsExecuting(false);
    }
  };

  return (
    <div style={{ 
      display: 'flex', 
      flexDirection: 'column', 
      height: '100vh', 
      background: '#020617', 
      overflow: 'hidden' 
    }}>
      
      {/* Top Banner */}
      <header style={{
        height: '40px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '0 20px',
        background: '#0f172a',
        borderBottom: '1px solid #1e293b',
        boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
        position: 'relative'
      }}>
        <h1 style={{ 
          margin: 0, 
          fontSize: '21px', 
          fontWeight: 600, 
          color: '#f8fafc', 
          letterSpacing: '0.025em' 
        }}>
          Visualisation Assistant - <span style={{ color: '#3b82f6' }}> ARIS </span>
        </h1>
      </header>
      
      {/* Chart Explorer Section */}
      <section style={{ 
        flex: 1, 
        position: 'relative', 
        background: '#ffffff',
        margin: '0', // Full width to meet the header
        overflow: 'hidden'
      }}>
        {supersetUrl ? (
          <iframe
            src={supersetUrl}
            width="100%"
            height="100%"
            title="Apache Superset"
            style={{ border: 'none' }}
          />
        ) : (
          <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%', color: '#64748b', background: '#f8fafc' }}>
            <p>Enter a prompt in the workspace below to generate a dataset for visualisation.</p>
          </div>
        )}
      </section>

      {/* The Resizer (Grip) */}
      <div 
        onMouseDown={startResizing}
        style={{ 
          height: '4px', 
          cursor: 'ns-resize', 
          background: isResizing ? '#3b82f6' : '#1e293b', 
          transition: 'background 0.2s',
          zIndex: 10
        }} 
      />

      {/* The Prompt Workspace */}
      <footer style={{ 
        height: `${footerHeight}px`, 
        padding: '16px 24px', 
        background: '#0f172a', 
        borderTop: '1px solid #1e293b',
        display: 'flex',
        flexDirection: 'column',
        gap: '12px'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ color: '#94a3b8', fontSize: '11px', fontWeight: 600, letterSpacing: '0.05em' }}>
                PROMPT WORKSPACE
            </span>
            <span style={{ color: '#475569', fontSize: '11px' }}>
                User: {user.username}
            </span>
        </div>

        <textarea
          style={{ 
            flex: 1,
            width: '100%', 
            borderRadius: '6px', 
            padding: '12px', 
            background: '#1e293b', 
            color: '#f8fafc',
            border: '1px solid #334155',
            resize: 'none',
            fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
            fontSize: '14px',
            outline: 'none',
            lineHeight: '1.5'
          }}
          placeholder="e.g., 'Analyze repository growth over the last quarter...'"
          value={prompt}
          onChange={(e) => setPrompt(e.target.value)}
          disabled={isExecuting}
        />
        
        <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <button 
              onClick={handleExecute}
              disabled={isExecuting || !prompt.trim()}
              style={{ 
                padding: '8px 24px', 
                cursor: isExecuting ? 'not-allowed' : 'pointer',
                background: '#2563eb',
                color: 'white',
                border: 'none',
                borderRadius: '6px',
                fontWeight: 600,
                fontSize: '13px',
                opacity: isExecuting ? 0.7 : 1,
                boxShadow: '0 4px 6px -1px rgba(37, 99, 235, 0.2)'
              }}
            >
              {isExecuting ? 'Analyzing...' : 'Execute'}
            </button>
        </div>
      </footer>
    </div>
  );
};

export default Dashboard;

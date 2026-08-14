import React, { useEffect, useState } from 'react';
import { dashboardApi } from '../api/dashboardApi';
import { Users, CheckSquare, Clock, CheckCircle, AlertTriangle } from 'lucide-react';

export const DashboardPage = () => {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchSummary = async () => {
      try {
        const data = await dashboardApi.getSummary();
        setSummary(data);
      } catch (err) {
        console.error('Failed to fetch dashboard summary:', err);
        setError('Failed to load dashboard statistics.');
      } finally {
        setLoading(false);
      }
    };
    fetchSummary();
  }, []);

  if (loading) {
    return <div style={{ color: 'var(--text-muted)' }}>Loading dashboard metrics...</div>;
  }

  if (error) {
    return <div className="error-banner">{error}</div>;
  }

  const leadsByStatus = summary?.leadsByStatus || {};

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Dashboard Overview</h1>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Users size={18} color="var(--primary)" />
            Total Leads
          </div>
          <div className="stat-value">{summary?.totalLeads ?? 0}</div>
        </div>

        <div className="stat-card">
          <div className="stat-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <CheckSquare size={18} color="var(--purple)" />
            Total Tasks
          </div>
          <div className="stat-value">{summary?.totalTasks ?? 0}</div>
        </div>

        <div className="stat-card">
          <div className="stat-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Clock size={18} color="var(--warning)" />
            Pending Tasks
          </div>
          <div className="stat-value" style={{ color: 'var(--warning)' }}>{summary?.pendingTasks ?? 0}</div>
        </div>

        <div className="stat-card">
          <div className="stat-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <CheckCircle size={18} color="var(--success)" />
            Completed Tasks
          </div>
          <div className="stat-value" style={{ color: 'var(--success)' }}>{summary?.completedTasks ?? 0}</div>
        </div>

        <div className="stat-card">
          <div className="stat-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <AlertTriangle size={18} color="var(--danger)" />
            Overdue Tasks
          </div>
          <div className="stat-value" style={{ color: 'var(--danger)' }}>{summary?.overdueTasks ?? 0}</div>
        </div>

      </div>

      <h2 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1rem', marginTop: '2rem' }}>
        Leads Distribution by Stage
      </h2>

      <div className="stats-grid">
        {Object.entries(leadsByStatus).length > 0 ? (
          Object.entries(leadsByStatus).map(([status, count]) => (
            <div className="stat-card" key={status} style={{ borderLeft: '4px solid var(--primary)' }}>
              <div className="stat-title">{status.replace('_', ' ')}</div>
              <div className="stat-value" style={{ fontSize: '1.5rem' }}>{count}</div>
            </div>
          ))
        ) : (
          <div style={{ color: 'var(--text-muted)', gridColumn: '1 / -1' }}>No lead breakdown available</div>
        )}
      </div>
    </div>
  );
};

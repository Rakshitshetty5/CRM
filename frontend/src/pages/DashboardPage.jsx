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
          <div className="stat-title">
            <Users size={16} color="var(--primary)" />
            <span>Total Leads</span>
          </div>
          <div className="stat-value">{summary?.totalLeads ?? 0}</div>
        </div>

        <div className="stat-card">
          <div className="stat-title">
            <CheckSquare size={16} color="var(--purple)" />
            <span>Total Tasks</span>
          </div>
          <div className="stat-value">{summary?.totalTasks ?? 0}</div>
        </div>

        <div className="stat-card">
          <div className="stat-title">
            <Clock size={16} color="var(--warning)" />
            <span>Pending Tasks</span>
          </div>
          <div className="stat-value">{summary?.pendingTasks ?? 0}</div>
        </div>

        <div className="stat-card">
          <div className="stat-title">
            <CheckCircle size={16} color="var(--success)" />
            <span>Completed Tasks</span>
          </div>
          <div className="stat-value">{summary?.completedTasks ?? 0}</div>
        </div>

        <div className="stat-card">
          <div className="stat-title">
            <AlertTriangle size={16} color="var(--danger)" />
            <span>Overdue Tasks</span>
          </div>
          <div className="stat-value">{summary?.overdueTasks ?? 0}</div>
        </div>
      </div>

      <h2 style={{ fontSize: '1.125rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '1rem', marginTop: '2rem' }}>
        Leads Pipeline Stage Breakdown
      </h2>

      <div className="stats-grid">
        {Object.entries(leadsByStatus).length > 0 ? (
          Object.entries(leadsByStatus).map(([status, count]) => (
            <div className="stat-card" key={status}>
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

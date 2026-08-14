import React, { useEffect, useState } from 'react';
import { notificationApi } from '../api/notificationApi';
import { Bell, CheckCircle } from 'lucide-react';

export const NotificationsPage = () => {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchNotifications();
  }, []);

  const fetchNotifications = async () => {
    setLoading(true);
    try {
      const data = await notificationApi.getNotifications({ page: 0, size: 50 });
      setNotifications(data.content || []);
    } catch (err) {
      console.error('Failed to fetch notifications:', err);
      setError('Failed to load notifications');
    } finally {
      setLoading(false);
    }
  };

  const handleMarkAsRead = async (id) => {
    try {
      await notificationApi.markAsRead(id);
      setNotifications(prev =>
        prev.map(n => n.id === id ? { ...n, isRead: true } : n)
      );
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to mark notification as read');
    }
  };

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Notifications</h1>
      </div>

      {loading ? (
        <div style={{ color: 'var(--text-muted)' }}>Loading notifications...</div>
      ) : error ? (
        <div className="error-banner">{error}</div>
      ) : notifications.length === 0 ? (
        <div className="table-container" style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          No notifications yet. You're all caught up!
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {notifications.map((n) => (
            <div
              key={n.id}
              style={{
                backgroundColor: n.isRead ? 'var(--bg-card)' : '#1e293b',
                border: '1px solid var(--border-color)',
                borderLeft: n.isRead ? '1px solid var(--border-color)' : '4px solid var(--primary)',
                borderRadius: 'var(--radius)',
                padding: '1.25rem',
                display: 'flex',
                alignItems: 'flex-start',
                justifyContent: 'space-between',
                gap: '1rem'
              }}
            >
              <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>
                <div
                  style={{
                    padding: '0.6rem',
                    borderRadius: '50%',
                    backgroundColor: n.isRead ? 'rgba(148, 163, 184, 0.1)' : 'rgba(59, 130, 246, 0.15)',
                    color: n.isRead ? 'var(--text-muted)' : 'var(--primary)',
                    marginTop: '0.2rem'
                  }}
                >
                  <Bell size={20} />
                </div>
                <div>
                  <div style={{ fontWeight: 600, fontSize: '1rem', marginBottom: '0.25rem' }}>
                    {n.title}
                  </div>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '0.5rem' }}>
                    {n.message}
                  </div>
                  <div style={{ fontSize: '0.75rem', color: '#64748b' }}>
                    Type: <span className="badge badge-new" style={{ fontSize: '0.7rem' }}>{n.type}</span> • {new Date(n.createdAt).toLocaleString()}
                  </div>
                </div>
              </div>

              {!n.isRead && (
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={() => handleMarkAsRead(n.id)}
                  title="Mark as read"
                >
                  <CheckCircle size={14} />
                  <span>Mark Read</span>
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

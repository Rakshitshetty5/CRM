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
      const notifsList = Array.isArray(data) ? data : (data?.content || data?.data?.content || data?.data || []);
      setNotifications(notifsList);
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
      console.error('Failed to mark notification as read:', err);
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
          {notifications.map((n) => {
            const meta = n.metadata || {};
            const itemTitle = meta.taskTitle
              ? `${n.title}: ${meta.taskTitle}`
              : meta.leadName
              ? `${n.title}: ${meta.leadName}`
              : n.title;

            const itemDescription = meta.taskDescription || meta.leadDescription || n.message;

            const hasDetails = Boolean(meta.leadName || meta.taskTitle || meta.stage);

            return (
              <div
                key={n.id}
                style={{
                  backgroundColor: n.isRead ? 'var(--bg-surface)' : '#0f172a',
                  border: '1px solid var(--border-color)',
                  borderLeft: n.isRead ? '1px solid var(--border-color)' : '4px solid var(--primary)',
                  borderRadius: 'var(--radius-lg)',
                  padding: '1.25rem',
                  display: 'flex',
                  alignItems: 'flex-start',
                  justifyContent: 'space-between',
                  gap: '1rem',
                  boxShadow: 'var(--shadow-sm)',
                  transition: 'all 0.15s ease'
                }}
              >
                <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start', flex: 1 }}>
                  <div
                    style={{
                      padding: '0.6rem',
                      borderRadius: '50%',
                      backgroundColor: n.isRead ? 'var(--bg-secondary)' : 'rgba(37, 99, 235, 0.25)',
                      color: n.isRead ? 'var(--text-muted)' : '#60a5fa',
                      marginTop: '0.2rem'
                    }}
                  >
                    <Bell size={20} />
                  </div>

                  <div style={{ flex: 1 }}>
                    <div
                      style={{
                        fontWeight: 600,
                        fontSize: '1rem',
                        marginBottom: '0.35rem',
                        color: n.isRead ? 'var(--text-main)' : '#ffffff'
                      }}
                    >
                      {itemTitle}
                    </div>

                    <div
                      style={{
                        color: n.isRead ? 'var(--text-secondary)' : '#e2e8f0',
                        fontSize: '0.875rem',
                        marginBottom: '0.75rem',
                        lineHeight: 1.5
                      }}
                    >
                      {itemDescription}
                    </div>

                    {/* Human-readable metadata details block without raw IDs */}
                    {hasDetails && (
                      <div
                        style={{
                          backgroundColor: n.isRead ? 'var(--bg-page)' : 'rgba(255, 255, 255, 0.07)',
                          border: `1px solid ${n.isRead ? 'var(--border-color)' : 'rgba(255, 255, 255, 0.12)'}`,
                          borderRadius: 'var(--radius)',
                          padding: '0.75rem 1rem',
                          marginBottom: '0.75rem',
                          display: 'grid',
                          gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
                          gap: '0.5rem 1rem',
                          fontSize: '0.8125rem'
                        }}
                      >
                        {meta.leadName && (
                          <div>
                            <span style={{ color: n.isRead ? 'var(--text-muted)' : '#94a3b8' }}>Lead Name: </span>
                            <strong style={{ color: n.isRead ? 'var(--text-main)' : '#ffffff' }}>{meta.leadName}</strong>
                          </div>
                        )}
                        {meta.taskTitle && (
                          <div>
                            <span style={{ color: n.isRead ? 'var(--text-muted)' : '#94a3b8' }}>Task Title: </span>
                            <strong style={{ color: n.isRead ? 'var(--text-main)' : '#ffffff' }}>{meta.taskTitle}</strong>
                          </div>
                        )}
                        {meta.stage && (
                          <div>
                            <span style={{ color: n.isRead ? 'var(--text-muted)' : '#94a3b8' }}>Stage: </span>
                            <span className="badge badge-qualified" style={{ fontSize: '0.7rem' }}>{meta.stage}</span>
                          </div>
                        )}
                      </div>
                    )}

                    <div style={{ fontSize: '0.75rem', color: n.isRead ? 'var(--text-muted)' : '#94a3b8' }}>
                      Type: <span className="badge badge-new" style={{ fontSize: '0.7rem' }}>{n.type}</span> • {new Date(n.createdAt).toLocaleString()}
                    </div>
                  </div>
                </div>

                {!n.isRead && (
                  <button
                    className="btn btn-primary btn-sm"
                    onClick={() => handleMarkAsRead(n.id)}
                    title="Mark as read"
                    style={{ flexShrink: 0 }}
                  >
                    <CheckCircle size={14} />
                    <span>Mark Read</span>
                  </button>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

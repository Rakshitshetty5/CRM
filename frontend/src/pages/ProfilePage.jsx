import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Shield, Building, Mail, Calendar } from 'lucide-react';

export const ProfilePage = () => {
  const { user } = useAuth();

  if (!user) {
    return <div style={{ color: 'var(--text-muted)' }}>Loading user profile...</div>;
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">User Profile</h1>
      </div>

      <div className="stat-card" style={{ maxWidth: '600px', padding: '2rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem', marginBottom: '2rem' }}>
          <div className="user-avatar" style={{ width: '60px', height: '60px', fontSize: '1.375rem' }}>
            {user.firstName ? user.firstName[0].toUpperCase() : 'U'}
            {user.lastName ? user.lastName[0].toUpperCase() : ''}
          </div>
          <div>
            <h2 style={{ fontSize: '1.375rem', fontWeight: 600, color: 'var(--text-main)' }}>
              {user.firstName} {user.lastName}
            </h2>
            <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.4rem' }}>
              <span className="badge badge-qualified">{user.role}</span>
              <span className={`badge ${user.active ? 'badge-active' : 'badge-inactive'}`}>
                {user.active ? 'ACTIVE' : 'INACTIVE'}
              </span>
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem', borderTop: '1px solid var(--border-color)', paddingTop: '1.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <Mail size={18} color="var(--primary)" />
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Email Address</div>
              <div style={{ fontWeight: 500, color: 'var(--text-main)' }}>{user.email}</div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <Building size={18} color="var(--purple)" />
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Organization</div>
              <div style={{ fontWeight: 500, color: 'var(--text-main)' }}>{user.organizationName || user.organizationId}</div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <Shield size={18} color="var(--warning)" />
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Account ID</div>
              <div style={{ fontWeight: 500, fontSize: '0.85rem', color: 'var(--text-main)' }}>{user.id}</div>
            </div>
          </div>

          {user.createdAt && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <Calendar size={18} color="var(--success)" />
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Member Since</div>
                <div style={{ fontWeight: 500, color: 'var(--text-main)' }}>{new Date(user.createdAt).toLocaleDateString()}</div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

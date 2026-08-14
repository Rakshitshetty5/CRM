import React from 'react';
import { useAuth } from '../context/AuthContext';

export const Header = () => {
  const { user } = useAuth();

  const getInitials = () => {
    if (!user) return 'U';
    const first = user.firstName ? user.firstName[0] : '';
    const last = user.lastName ? user.lastName[0] : '';
    return (first + last).toUpperCase() || 'U';
  };

  return (
    <header className="header">
      <div className="org-badge">
        <span>Organization:</span>
        <strong>{user?.organizationName || user?.organizationId || 'Default Organization'}</strong>
      </div>

      <div className="header-user">
        <div className="user-avatar">{getInitials()}</div>
        <div>
          <div style={{ fontWeight: 600, fontSize: '0.875rem', color: 'var(--text-main)' }}>
            {user ? `${user.firstName} ${user.lastName}` : 'User'}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            {user?.role || 'USER'}
          </div>
        </div>
      </div>
    </header>
  );
};

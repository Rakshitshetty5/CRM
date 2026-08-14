import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { 
  LayoutDashboard, 
  Users, 
  CheckSquare, 
  Bell, 
  User, 
  UserCheck, 
  LogOut 
} from 'lucide-react';

export const Sidebar = () => {
  const { user, logout } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <LayoutDashboard size={24} />
        <span>FlowCRM</span>
      </div>

      <ul className="nav-list">
        <li>
          <NavLink to="/" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            <LayoutDashboard size={18} />
            <span>Dashboard</span>
          </NavLink>
        </li>
        <li>
          <NavLink to="/leads" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            <Users size={18} />
            <span>Leads</span>
          </NavLink>
        </li>
        <li>
          <NavLink to="/tasks" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            <CheckSquare size={18} />
            <span>Tasks</span>
          </NavLink>
        </li>
        <li>
          <NavLink to="/notifications" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            <Bell size={18} />
            <span>Notifications</span>
          </NavLink>
        </li>
        <li>
          <NavLink to="/profile" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            <User size={18} />
            <span>Profile</span>
          </NavLink>
        </li>
        {isAdmin && (
          <li>
            <NavLink to="/users" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
              <UserCheck size={18} />
              <span>Users</span>
            </NavLink>
          </li>
        )}
      </ul>

      <div style={{ marginTop: 'auto', paddingTop: '1rem', borderTop: '1px solid var(--border-color)' }}>
        <button onClick={logout} className="nav-link" style={{ width: '100%', background: 'none', border: 'none', cursor: 'pointer' }}>
          <LogOut size={18} />
          <span>Logout</span>
        </button>
      </div>
    </aside>
  );
};

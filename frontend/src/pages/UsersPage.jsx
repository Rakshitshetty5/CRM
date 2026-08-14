import React, { useEffect, useState } from 'react';
import { userApi } from '../api/userApi';
import { Plus, UserCheck, Shield } from 'lucide-react';

export const UsersPage = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Create User Modal State
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newUser, setNewUser] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    role: 'SALES_REP',
  });

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const data = await userApi.getUsers({ size: 100 });
      const usersList = Array.isArray(data) ? data : (data?.content || data?.data?.content || data?.data || []);
      setUsers(usersList);
    } catch (err) {
      console.error('Failed to fetch users:', err);
      setError('Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    try {
      await userApi.createUser(newUser);
      setShowCreateModal(false);
      setNewUser({ firstName: '', lastName: '', email: '', password: '', role: 'SALES_REP' });
      await fetchUsers();
    } catch (err) {
      console.error('Failed to create user:', err);
    }
  };

  const handleStatusToggle = async (userId, currentStatus) => {
    try {
      await userApi.updateUserStatus(userId, !currentStatus);
      await fetchUsers();
    } catch (err) {
      console.error('Failed to update user status:', err);
    }
  };



  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">User Management (Admin)</h1>
        <button className="btn btn-primary" onClick={() => setShowCreateModal(true)}>
          <Plus size={18} />
          <span>Create User</span>
        </button>
      </div>

      {loading ? (
        <div style={{ color: 'var(--text-muted)' }}>Loading users...</div>
      ) : error ? (
        <div className="error-banner">{error}</div>
      ) : (
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td style={{ fontWeight: 600 }}>{u.firstName} {u.lastName}</td>
                  <td>{u.email}</td>
                  <td>
                    <span className="badge badge-qualified">{u.role === "ADMIN" ? "ADMIN" : "SALES"}</span>
                  </td>
                  <td>
                    <span className={`badge ${u.active ? 'badge-active' : 'badge-inactive'}`}>
                      {u.active ? 'ACTIVE' : 'INACTIVE'}
                    </span>
                  </td>
                  <td>
                    <button
                      className={`btn ${u.active ? 'btn-secondary' : 'btn-primary'} btn-sm`}
                      onClick={() => handleStatusToggle(u.id, u.active)}
                    >
                      <UserCheck size={14} />
                      {u.active ? 'Deactivate' : 'Activate'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Create User Modal */}
      {showCreateModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3 className="modal-title">Create Organization User</h3>
              <button onClick={() => setShowCreateModal(false)} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}>✕</button>
            </div>
            <form onSubmit={handleCreateSubmit}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div className="form-group">
                  <label className="form-label">First Name</label>
                  <input type="text" className="form-input" required value={newUser.firstName} onChange={(e) => setNewUser({ ...newUser, firstName: e.target.value })} />
                </div>
                <div className="form-group">
                  <label className="form-label">Last Name</label>
                  <input type="text" className="form-input" required value={newUser.lastName} onChange={(e) => setNewUser({ ...newUser, lastName: e.target.value })} />
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">Email</label>
                <input type="email" className="form-input" required value={newUser.email} onChange={(e) => setNewUser({ ...newUser, email: e.target.value })} />
              </div>
              <div className="form-group">
                <label className="form-label">Initial Password</label>
                <input type="password" className="form-input" required value={newUser.password} onChange={(e) => setNewUser({ ...newUser, password: e.target.value })} />
              </div>
              {/*<div className="form-group">*/}
              {/*  <label className="form-label">Role</label>*/}
              {/*  <select className="form-select" value={newUser.role} onChange={(e) => setNewUser({ ...newUser, role: e.target.value })}>*/}
              {/*    <option value="SALES_REP">USER</option>*/}
              {/*    <option value="ADMIN">ADMIN</option>*/}
              {/*  </select>*/}
              {/*</div>*/}
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '1.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowCreateModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Create User</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

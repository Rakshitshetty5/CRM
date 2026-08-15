import React, { useEffect, useState } from 'react';
import { leadApi } from '../api/leadApi';
import { userApi } from '../api/userApi';
import { useAuth } from '../context/AuthContext';
import { canEditLead } from '../utils/permissionUtils';
import { Plus, Search, Filter, UserCheck, Eye, Edit2 } from 'lucide-react';

export const LeadsPage = () => {
  const { user } = useAuth();
  const [leads, setLeads] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [users, setUsers] = useState([]);

  // Search & Filter
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  // Modals
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [selectedLead, setSelectedLead] = useState(null);
  const [editingLead, setEditingLead] = useState(null);
  const [formError, setFormError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [leadActivities, setLeadActivities] = useState([]);

  // Create/Edit Form State
  const [newLead, setNewLead] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    company: '',
    source: 'WEBSITE',
    notes: '',
  });

  // Assign Form State
  const [assigneeId, setAssigneeId] = useState('');

  useEffect(() => {
    fetchLeads();
    fetchUsers();
  }, [search, statusFilter]);

  const fetchLeads = async () => {
    setLoading(true);
    try {
      const params = {};
      if (search) params.search = search;
      if (statusFilter) params.status = statusFilter;
      const data = await leadApi.getLeads(params);
      const leadsList = Array.isArray(data) ? data : (data?.content || data?.data?.content || data?.data || []);
      setLeads(leadsList);
    } catch (err) {
      console.error('Failed to fetch leads:', err);
      setError('Failed to load leads');
    } finally {
      setLoading(false);
    }
  };

  const fetchUsers = async () => {
    try {
      const data = await userApi.getUsers({ role: 'SALES_REP', active: true, size: 100 });
      const usersList = Array.isArray(data) ? data : (data?.content || data?.data?.content || data?.data || []);
      setUsers(usersList);
    } catch (err) {
      console.error('Failed to fetch users:', err);
    }
  };

  const openCreateModal = () => {
    setEditingLead(null);
    setFormError('');
    setFieldErrors({});
    setNewLead({
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      company: '',
      source: 'WEBSITE',
      notes: '',
    });
    setShowCreateModal(true);
  };

  const openEditModal = (lead) => {
    setEditingLead(lead);
    setFormError('');
    setFieldErrors({});
    setNewLead({
      firstName: lead.firstName || '',
      lastName: lead.lastName || '',
      email: lead.email || '',
      phone: lead.phone || '',
      company: lead.company || '',
      source: lead.source || 'WEBSITE',
      notes: lead.notes || '',
    });
    setShowCreateModal(true);
  };

  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    setFormError('');
    setFieldErrors({});
    try {
      if (editingLead) {
        await leadApi.updateLead(editingLead.id, newLead);
      } else {
        await leadApi.createLead(newLead);
      }
      setShowCreateModal(false);
      setEditingLead(null);
      setNewLead({ firstName: '', lastName: '', email: '', phone: '', company: '', source: 'WEBSITE', notes: '' });
      await fetchLeads();
    } catch (err) {
      console.error('Failed to save lead:', err);
      const serverErrors = err.response?.data?.errors;
      if (serverErrors && typeof serverErrors === 'object') {
        setFieldErrors(serverErrors);
      }
      let errorMsg = err.response?.data?.message || err.message || 'Failed to save lead';
      if (serverErrors && typeof serverErrors === 'object') {
        const fieldMsgs = Object.values(serverErrors).filter(Boolean);
        if (fieldMsgs.length > 0) {
          errorMsg = fieldMsgs.join('. ');
        }
      }
      setFormError(errorMsg);
    }
  };

  const handleStatusChange = async (leadId, newStatus) => {
    try {
      await leadApi.updateLeadStatus(leadId, newStatus);
      await fetchLeads();
      if (selectedLead && selectedLead.id === leadId) {
        openDetailModal({ ...selectedLead, status: newStatus });
      }
    } catch (err) {
      console.error('Failed to update lead status:', err);
    }
  };

  const handleAssignSubmit = async (e) => {
    e.preventDefault();
    if (!selectedLead || !assigneeId) return;
    try {
      await leadApi.assignLead(selectedLead.id, assigneeId);
      setShowAssignModal(false);
      await fetchLeads();
    } catch (err) {
      console.error('Failed to assign lead:', err);
    }
  };

  const openDetailModal = async (lead) => {
    setSelectedLead(lead);
    setShowDetailModal(true);
    try {
      const activities = await leadApi.getLeadActivities(lead.id);
      setLeadActivities(activities || []);
    } catch (err) {
      setLeadActivities([]);
    }
  };

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'NEW': return 'badge-new';
      case 'CONTACTED': return 'badge-contacted';
      case 'QUALIFIED': return 'badge-qualified';
      case 'WON': return 'badge-won';
      case 'LOST': return 'badge-lost';
      default: return 'badge-new';
    }
  };

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Lead Management</h1>
        <button className="btn btn-primary" onClick={openCreateModal}>
          <Plus size={18} />
          <span>Create Lead</span>
        </button>
      </div>

      {/* Filters Bar */}
      <div className="filter-bar">
        <div className="search-input-container">
          <Search size={18} className="search-icon" />
          <input
            type="text"
            className="form-input search-input"
            placeholder="Search name, email, or company..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        <select
          className="form-select filter-select"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
        >
          <option value="">All Statuses</option>
          <option value="NEW">New</option>
          <option value="CONTACTED">Contacted</option>
          <option value="QUALIFIED">Qualified</option>
          <option value="DEMO_SCHEDULED">Demo Scheduled</option>
          <option value="PROPOSAL_SENT">Proposal Sent</option>
          <option value="NEGOTIATION">Negotiation</option>
          <option value="WON">Won</option>
          <option value="LOST">Lost</option>
        </select>
      </div>

      {loading ? (
        <div style={{ color: 'var(--text-muted)' }}>Loading leads...</div>
      ) : leads.length === 0 ? (
        <div className="table-container" style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          No leads found. Create your first lead to get started!
        </div>
      ) : (
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Company</th>
                <th>Email</th>
                <th>Status</th>
                <th>Assigned User</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {leads.map((lead) => (
                <tr key={lead.id}>
                  <td style={{ fontWeight: 600 }}>{lead.firstName} {lead.lastName}</td>
                  <td>{lead.company || '-'}</td>
                  <td>{lead.email}</td>
                  <td>
                    <select
                      className={`badge ${getStatusBadgeClass(lead.status)}`}
                      style={{ border: 'none', cursor: 'pointer', outline: 'none' }}
                      value={lead.status}
                      onChange={(e) => handleStatusChange(lead.id, e.target.value)}
                    >
                      <option value="NEW">NEW</option>
                      <option value="CONTACTED">CONTACTED</option>
                      <option value="QUALIFIED">QUALIFIED</option>
                      <option value="DEMO_SCHEDULED">DEMO SCHEDULED</option>
                      <option value="PROPOSAL_SENT">PROPOSAL SENT</option>
                      <option value="NEGOTIATION">NEGOTIATION</option>
                      <option value="WON">WON</option>
                      <option value="LOST">LOST</option>
                    </select>
                  </td>
                  <td>{lead.assignedToName || 'Unassigned'}</td>
                  <td>
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                      <button className="btn btn-secondary btn-sm" onClick={() => openDetailModal(lead)}>
                        <Eye size={14} /> View
                      </button>
                      {canEditLead(user, lead) && (
                        <button className="btn btn-secondary btn-sm" onClick={() => openEditModal(lead)}>
                          <Edit2 size={14} /> Edit
                        </button>
                      )}
                      <button className="btn btn-secondary btn-sm" onClick={() => { setSelectedLead(lead); setShowAssignModal(true); }}>
                        <UserCheck size={14} /> Assign
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Create / Edit Lead Modal */}
      {showCreateModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3 className="modal-title">{editingLead ? 'Edit Lead' : 'Create New Lead'}</h3>
              <button onClick={() => { setShowCreateModal(false); setEditingLead(null); }} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', fontSize: '1.2rem' }}>✕</button>
            </div>
            <form onSubmit={handleCreateSubmit}>
              {formError && (
                <div style={{ padding: '0.75rem 1rem', backgroundColor: 'rgba(239, 68, 68, 0.1)', border: '1px solid var(--danger)', borderRadius: 'var(--radius)', color: 'var(--danger)', marginBottom: '1rem', fontSize: '0.875rem' }}>
                  {formError}
                </div>
              )}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div className="form-group">
                  <label className="form-label">First Name</label>
                  <input
                    type="text"
                    className={`form-input ${fieldErrors.firstName ? 'is-invalid' : ''}`}
                    required
                    value={newLead.firstName}
                    onChange={(e) => setNewLead({ ...newLead, firstName: e.target.value })}
                  />
                  {fieldErrors.firstName && <span className="form-error-msg">{fieldErrors.firstName}</span>}
                </div>
                <div className="form-group">
                  <label className="form-label">Last Name</label>
                  <input
                    type="text"
                    className={`form-input ${fieldErrors.lastName ? 'is-invalid' : ''}`}
                    required
                    value={newLead.lastName}
                    onChange={(e) => setNewLead({ ...newLead, lastName: e.target.value })}
                  />
                  {fieldErrors.lastName && <span className="form-error-msg">{fieldErrors.lastName}</span>}
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">Email</label>
                <input
                  type="email"
                  className={`form-input ${fieldErrors.email ? 'is-invalid' : ''}`}
                  required
                  value={newLead.email}
                  onChange={(e) => setNewLead({ ...newLead, email: e.target.value })}
                />
                {fieldErrors.email && <span className="form-error-msg">{fieldErrors.email}</span>}
              </div>
              <div className="form-group">
                <label className="form-label">Phone</label>
                <input
                  type="text"
                  className={`form-input ${fieldErrors.phone ? 'is-invalid' : ''}`}
                  value={newLead.phone}
                  onChange={(e) => setNewLead({ ...newLead, phone: e.target.value })}
                />
                {fieldErrors.phone && <span className="form-error-msg">{fieldErrors.phone}</span>}
              </div>
              <div className="form-group">
                <label className="form-label">Company</label>
                <input
                  type="text"
                  className={`form-input ${fieldErrors.company ? 'is-invalid' : ''}`}
                  value={newLead.company}
                  onChange={(e) => setNewLead({ ...newLead, company: e.target.value })}
                />
                {fieldErrors.company && <span className="form-error-msg">{fieldErrors.company}</span>}
              </div>
              <div className="form-group">
                <label className="form-label">Source</label>
                <select
                  className={`form-select ${fieldErrors.source ? 'is-invalid' : ''}`}
                  value={newLead.source}
                  onChange={(e) => setNewLead({ ...newLead, source: e.target.value })}
                >
                  <option value="WEBSITE">Website</option>
                  <option value="REFERRAL">Referral</option>
                  <option value="EMAIL">Email</option>
                  <option value="SOCIAL_MEDIA">Social Media</option>
                  <option value="MANUAL">Manual</option>
                </select>
                {fieldErrors.source && <span className="form-error-msg">{fieldErrors.source}</span>}
              </div>

              <div className="form-group">
                <label className="form-label">Notes</label>
                <textarea
                  className={`form-textarea ${fieldErrors.notes ? 'is-invalid' : ''}`}
                  rows="3"
                  value={newLead.notes}
                  onChange={(e) => setNewLead({ ...newLead, notes: e.target.value })}
                ></textarea>
                {fieldErrors.notes && <span className="form-error-msg">{fieldErrors.notes}</span>}
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '1.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => { setShowCreateModal(false); setEditingLead(null); }}>Cancel</button>
                <button type="submit" className="btn btn-primary">{editingLead ? 'Update Lead' : 'Create Lead'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Assign Lead Modal */}
      {showAssignModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3 className="modal-title">Assign Lead</h3>
              <button onClick={() => setShowAssignModal(false)} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}>✕</button>
            </div>
            <form onSubmit={handleAssignSubmit}>
              <div className="form-group">
                <label className="form-label">Select Assignee User</label>
                <select className="form-select" required value={assigneeId} onChange={(e) => setAssigneeId(e.target.value)}>
                  <option value="">-- Choose User --</option>
                  {users.map((u) => (
                    <option key={u.id} value={u.id}>{u.firstName} {u.lastName} ({u.email})</option>
                  ))}
                </select>
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '1.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowAssignModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Assign Lead</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Lead Detail & Activity Audit Modal */}
      {showDetailModal && selectedLead && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '600px' }}>
            <div className="modal-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <h3 className="modal-title">{selectedLead.firstName} {selectedLead.lastName}</h3>
                {canEditLead(user, selectedLead) && (
                  <button className="btn btn-secondary btn-sm" onClick={() => { setShowDetailModal(false); openEditModal(selectedLead); }}>
                    <Edit2 size={14} /> Edit
                  </button>
                )}
              </div>
              <button onClick={() => setShowDetailModal(false)} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}>✕</button>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.5rem' }}>
              <div><strong style={{ color: 'var(--text-muted)' }}>Email:</strong> {selectedLead.email}</div>
              <div><strong style={{ color: 'var(--text-muted)' }}>Phone:</strong> {selectedLead.phone || '-'}</div>
              <div><strong style={{ color: 'var(--text-muted)' }}>Company:</strong> {selectedLead.company || '-'}</div>
              <div><strong style={{ color: 'var(--text-muted)' }}>Status:</strong> {selectedLead.status}</div>
              <div><strong style={{ color: 'var(--text-muted)' }}>Source:</strong> {selectedLead.source || '-'}</div>
              <div><strong style={{ color: 'var(--text-muted)' }}>Assigned To:</strong> {selectedLead.assignedToName || 'Unassigned'}</div>
            </div>

            <h4 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '0.75rem' }}>Activity Audit Trail</h4>
            <div style={{ maxHeight: '200px', overflowY: 'auto', border: '1px solid var(--border-color)', borderRadius: 'var(--radius)', padding: '0.75rem' }}>
              {leadActivities.length > 0 ? (
                leadActivities.map((act) => (
                  <div key={act.id} style={{ marginBottom: '0.75rem', paddingBottom: '0.5rem', borderBottom: '1px solid var(--border-color)', fontSize: '0.85rem' }}>
                    <div style={{ fontWeight: 600, color: 'var(--primary)' }}>{act.type}</div>
                    <div>{act.description}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{new Date(act.createdAt).toLocaleString()}</div>
                  </div>
                ))
              ) : (
                <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>No activity records available</div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

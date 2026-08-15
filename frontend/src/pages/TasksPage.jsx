import React, { useEffect, useState } from 'react';
import { taskApi } from '../api/taskApi';
import { leadApi } from '../api/leadApi';
import { userApi } from '../api/userApi';
import { useAuth } from '../context/AuthContext';
import { canEditTask } from '../utils/permissionUtils';
import { toast } from '../utils/toast';
import { Plus, CheckCircle, Clock, AlertTriangle, Play, Edit2 } from 'lucide-react';

const formatForDatetimeLocal = (dateStr) => {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return '';
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

export const TasksPage = () => {
  const { user } = useAuth();
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [leads, setLeads] = useState([]);
  const [users, setUsers] = useState([]);

  // Filters
  const [statusFilter, setStatusFilter] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');

  // Create / Edit Modal State
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingTask, setEditingTask] = useState(null);
  const [formError, setFormError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [newTask, setNewTask] = useState({
    title: '',
    description: '',
    priority: 'MEDIUM',
    dueDate: '',
    leadId: '',
    assignedTo: '',
  });

  useEffect(() => {
    fetchTasks();
  }, [statusFilter, priorityFilter]);

  useEffect(() => {
    fetchLeadsAndUsers();
  }, []);

  const fetchTasks = async () => {
    setLoading(true);
    try {
      const params = { page: 0, size: 50 };
      if (statusFilter) params.status = statusFilter;
      if (priorityFilter) params.priority = priorityFilter;

      const data = await taskApi.getTasks(params);
      const tasksList = Array.isArray(data) ? data : (data?.content || data?.data?.content || data?.data || []);
      setTasks(tasksList);
    } catch (err) {
      console.error('Failed to fetch tasks:', err);
      setError('Failed to load tasks');
    } finally {
      setLoading(false);
    }
  };

  const fetchLeadsAndUsers = async () => {
    try {
      const leadData = await leadApi.getLeads({ size: 100 });
      const leadsList = Array.isArray(leadData) ? leadData : (leadData?.content || leadData?.data?.content || leadData?.data || []);
      setLeads(leadsList);

      const userData = await userApi.getUsers({ role: 'SALES_REP', active: true, size: 100 });
      const usersList = Array.isArray(userData) ? userData : (userData?.content || userData?.data?.content || userData?.data || []);
      setUsers(usersList);
    } catch (err) {
      console.error('Failed to fetch filter dependencies:', err);
    }
  };

  const openCreateModal = () => {
    setEditingTask(null);
    setFormError('');
    setFieldErrors({});
    setNewTask({ title: '', description: '', priority: 'MEDIUM', dueDate: '', leadId: '', assignedTo: '' });
    setShowCreateModal(true);
  };

  const openEditModal = (task) => {
    setEditingTask(task);
    setFormError('');
    setFieldErrors({});
    setNewTask({
      title: task.title || '',
      description: task.description || '',
      priority: task.priority || 'MEDIUM',
      dueDate: formatForDatetimeLocal(task.dueDate),
      leadId: task.leadId || '',
      assignedTo: task.assignedTo || '',
    });
    setShowCreateModal(true);
  };

  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    setFormError('');
    setFieldErrors({});

    if (newTask.dueDate && new Date(newTask.dueDate) <= new Date()) {
      setFieldErrors({ dueDate: 'Due date must be in the future' });
      setFormError('Due date must be in the future');
      return;
    }

    try {
      if (editingTask) {
        await taskApi.updateTask(editingTask.id, {
          title: newTask.title,
          description: newTask.description,
          priority: newTask.priority,
          dueDate: newTask.dueDate,
          assignedTo: newTask.assignedTo,
        });
      } else {
        await taskApi.createTask(newTask);
      }
      setShowCreateModal(false);
      setEditingTask(null);
      setNewTask({ title: '', description: '', priority: 'MEDIUM', dueDate: '', leadId: '', assignedTo: '' });
      await fetchTasks();
    } catch (err) {
      console.error('Failed to save task:', err);
      const serverErrors = err.response?.data?.errors;
      if (serverErrors && typeof serverErrors === 'object') {
        setFieldErrors(serverErrors);
      }
      let errorMsg = err.response?.data?.message || err.message || 'Failed to save task';
      if (serverErrors && typeof serverErrors === 'object') {
        const fieldMsgs = Object.values(serverErrors).filter(Boolean);
        if (fieldMsgs.length > 0) {
          errorMsg = fieldMsgs.join('. ');
        }
      }
      setFormError(errorMsg);
    }
  };

  const handleStatusChange = async (taskId, newStatus) => {
    try {
      await taskApi.updateTaskStatus(taskId, newStatus);
      await fetchTasks();
    } catch (err) {
      console.error('Failed to update task status:', err);
    }
  };

  const getPriorityBadgeClass = (priority) => {
    switch (priority) {
      case 'HIGH': return 'badge-high';
      case 'MEDIUM': return 'badge-medium';
      case 'LOW': return 'badge-low';
      default: return 'badge-medium';
    }
  };

  const isOverdue = (dueDate, status) => {
    if (status === 'COMPLETED' || !dueDate) return false;
    return new Date(dueDate) < new Date();
  };

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Tasks & Reminders</h1>
        <button className="btn btn-primary" onClick={openCreateModal}>
          <Plus size={18} />
          <span>Create Task</span>
        </button>
      </div>

      {/* Filters Bar */}
      <div className="filter-bar">
        <select
          className="form-select filter-select"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
        >
          <option value="">All Statuses</option>
          <option value="PENDING">Pending</option>
          <option value="IN_PROGRESS">In Progress</option>
          <option value="COMPLETED">Completed</option>
          <option value="OVERDUE">Overdue</option>
        </select>

        <select
          className="form-select filter-select"
          value={priorityFilter}
          onChange={(e) => setPriorityFilter(e.target.value)}
        >
          <option value="">All Priorities</option>
          <option value="LOW">Low</option>
          <option value="MEDIUM">Medium</option>
          <option value="HIGH">High</option>
        </select>
      </div>

      {loading ? (
        <div style={{ color: 'var(--text-muted)' }}>Loading tasks...</div>
      ) : tasks.length === 0 ? (
        <div className="table-container" style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          No tasks found. Create a new task to organize your follow-ups!
        </div>
      ) : (
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Title</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Due Date</th>
                <th>Related Lead</th>
                <th>Assigned To</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {tasks.map((task) => {
                const overdue = isOverdue(task.dueDate, task.status);
                return (
                  <tr key={task.id}>
                    <td>
                      <div style={{ fontWeight: 600, color: 'var(--text-main)' }}>{task.title}</div>
                      {task.description && (
                        <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.15rem' }}>
                          {task.description}
                        </div>
                      )}
                    </td>
                    <td>
                      <span className={`badge ${getPriorityBadgeClass(task.priority)}`}>
                        {task.priority}
                      </span>
                    </td>
                    <td>
                      <span className={`badge ${task.status === 'COMPLETED' ? 'badge-won' : task.status === 'IN_PROGRESS' ? 'badge-qualified' : overdue ? 'badge-lost' : 'badge-contacted'}`}>
                        {overdue ? 'OVERDUE' : task.status.replace('_', ' ')}
                      </span>
                    </td>

                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: overdue ? '#f87171' : 'inherit' }}>
                        {overdue ? <AlertTriangle size={14} /> : <Clock size={14} />}
                        {task.dueDate ? new Date(task.dueDate).toLocaleString() : '-'}
                      </div>
                    </td>
                    <td>{task.leadName || '-'}</td>
                    <td>{task.assignedToName || 'Unassigned'}</td>

                    <td>
                      <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                        {canEditTask(user, task) && (
                          <button className="btn btn-secondary btn-sm" onClick={() => openEditModal(task)}>
                            <Edit2 size={13} /> Edit
                          </button>
                        )}
                        {task.status === 'COMPLETED' ? (
                          <button
                            className="btn btn-secondary btn-sm"
                            onClick={() => handleStatusChange(task.id, 'PENDING')}
                          >
                            Reopen
                          </button>
                        ) : task.status === 'IN_PROGRESS' ? (
                          <button
                            className="btn btn-primary btn-sm"
                            onClick={() => handleStatusChange(task.id, 'COMPLETED')}
                          >
                            <CheckCircle size={14} /> Complete
                          </button>
                        ) : (
                          <>
                            <button
                              className="btn btn-secondary btn-sm"
                              onClick={() => handleStatusChange(task.id, 'IN_PROGRESS')}
                            >
                              <Play size={13} /> Start
                            </button>
                            <button
                              className="btn btn-primary btn-sm"
                              onClick={() => handleStatusChange(task.id, 'COMPLETED')}
                            >
                              <CheckCircle size={14} /> Complete
                            </button>
                          </>
                        )}
                      </div>
                    </td>

                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Create / Edit Task Modal */}
      {showCreateModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3 className="modal-title">{editingTask ? 'Edit Task' : 'Create New Task'}</h3>
              <button onClick={() => { setShowCreateModal(false); setEditingTask(null); }} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}>✕</button>
            </div>
            <form onSubmit={handleCreateSubmit}>
              {formError && (
                <div style={{ padding: '0.75rem 1rem', backgroundColor: 'rgba(239, 68, 68, 0.1)', border: '1px solid var(--danger)', borderRadius: 'var(--radius)', color: 'var(--danger)', marginBottom: '1rem', fontSize: '0.875rem' }}>
                  {formError}
                </div>
              )}
              <div className="form-group">
                <label className="form-label">Task Title</label>
                <input
                  type="text"
                  className={`form-input ${fieldErrors.title ? 'is-invalid' : ''}`}
                  required
                  value={newTask.title}
                  onChange={(e) => setNewTask({ ...newTask, title: e.target.value })}
                  placeholder="Follow up call"
                />
                {fieldErrors.title && <span className="form-error-msg">{fieldErrors.title}</span>}
              </div>
              <div className="form-group">
                <label className="form-label">Description</label>
                <textarea
                  className={`form-textarea ${fieldErrors.description ? 'is-invalid' : ''}`}
                  rows="2"
                  value={newTask.description}
                  onChange={(e) => setNewTask({ ...newTask, description: e.target.value })}
                ></textarea>
                {fieldErrors.description && <span className="form-error-msg">{fieldErrors.description}</span>}
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div className="form-group">
                  <label className="form-label">Priority</label>
                  <select
                    className={`form-select ${fieldErrors.priority ? 'is-invalid' : ''}`}
                    value={newTask.priority}
                    onChange={(e) => setNewTask({ ...newTask, priority: e.target.value })}
                  >
                    <option value="LOW">LOW</option>
                    <option value="MEDIUM">MEDIUM</option>
                    <option value="HIGH">HIGH</option>
                  </select>
                  {fieldErrors.priority && <span className="form-error-msg">{fieldErrors.priority}</span>}
                </div>
                <div className="form-group">
                  <label className="form-label">Due Date & Time</label>
                  <input
                    type="datetime-local"
                    className={`form-input ${fieldErrors.dueDate ? 'is-invalid' : ''}`}
                    required
                    value={newTask.dueDate}
                    onChange={(e) => setNewTask({ ...newTask, dueDate: e.target.value })}
                  />
                  {fieldErrors.dueDate && <span className="form-error-msg">{fieldErrors.dueDate}</span>}
                </div>
              </div>
              {!editingTask && (
                <div className="form-group">
                  <label className="form-label">Related Lead</label>
                  <select
                    className={`form-select ${fieldErrors.leadId ? 'is-invalid' : ''}`}
                    value={newTask.leadId}
                    onChange={(e) => setNewTask({ ...newTask, leadId: e.target.value })}
                  >
                    <option value="">-- Select Lead --</option>
                    {leads.map(l => (
                      <option key={l.id} value={l.id}>
                        {l.company ? `${l.firstName} ${l.lastName} (${l.company})` : `${l.firstName} ${l.lastName}`}
                      </option>
                    ))}
                  </select>
                  {fieldErrors.leadId && <span className="form-error-msg">{fieldErrors.leadId}</span>}
                </div>
              )}
              <div className="form-group">
                <label className="form-label">Assign To</label>
                <select
                  className={`form-select ${fieldErrors.assignedTo ? 'is-invalid' : ''}`}
                  required
                  value={newTask.assignedTo}
                  onChange={(e) => setNewTask({ ...newTask, assignedTo: e.target.value })}
                >
                  <option value="">-- Assign Sales Rep --</option>
                  {users.map(u => (
                    <option key={u.id} value={u.id}>
                      {u.firstName} {u.lastName} ({u.role})
                    </option>
                  ))}
                </select>
                {fieldErrors.assignedTo && <span className="form-error-msg">{fieldErrors.assignedTo}</span>}
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '1.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => { setShowCreateModal(false); setEditingTask(null); }}>Cancel</button>
                <button type="submit" className="btn btn-primary">{editingTask ? 'Update Task' : 'Create Task'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

import React, { useEffect, useState } from 'react';
import { taskApi } from '../api/taskApi';
import { leadApi } from '../api/leadApi';
import { userApi } from '../api/userApi';
import { toast } from '../utils/toast';
import { Plus, CheckCircle, Clock, AlertTriangle, Play } from 'lucide-react';

export const TasksPage = () => {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [leads, setLeads] = useState([]);
  const [users, setUsers] = useState([]);

  // Filters
  const [statusFilter, setStatusFilter] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');

  // Create Modal State
  const [showCreateModal, setShowCreateModal] = useState(false);
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

  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    try {
      await taskApi.createTask(newTask);
      setShowCreateModal(false);
      setNewTask({ title: '', description: '', priority: 'MEDIUM', dueDate: '', leadId: '', assignedTo: '' });
      await fetchTasks();
    } catch (err) {
      console.error('Failed to create task:', err);
    }
  };

  const handleStatusChange = async (taskId, newStatus) => {
    try {
      await taskApi.updateTaskStatus(taskId, newStatus);
      await fetchTasks();
    } catch (err) {
      console.error('Failed to update task status:', err);
      const errorMsg = err.response?.data?.message || err.message || 'Failed to update task status';
      toast.error(errorMsg);
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
        <button className="btn btn-primary" onClick={() => setShowCreateModal(true)}>
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
                      <div style={{ display: 'flex', gap: '0.5rem' }}>
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

      {/* Create Task Modal */}
      {showCreateModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3 className="modal-title">Create New Task</h3>
              <button onClick={() => setShowCreateModal(false)} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}>✕</button>
            </div>
            <form onSubmit={handleCreateSubmit}>
              <div className="form-group">
                <label className="form-label">Task Title</label>
                <input type="text" className="form-input" required value={newTask.title} onChange={(e) => setNewTask({ ...newTask, title: e.target.value })} placeholder="Follow up call" />
              </div>
              <div className="form-group">
                <label className="form-label">Description</label>
                <textarea className="form-textarea" rows="2" value={newTask.description} onChange={(e) => setNewTask({ ...newTask, description: e.target.value })}></textarea>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div className="form-group">
                  <label className="form-label">Priority</label>
                  <select className="form-select" value={newTask.priority} onChange={(e) => setNewTask({ ...newTask, priority: e.target.value })}>
                    <option value="LOW">LOW</option>
                    <option value="MEDIUM">MEDIUM</option>
                    <option value="HIGH">HIGH</option>
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Due Date & Time</label>
                  <input type="datetime-local" className="form-input" required value={newTask.dueDate} onChange={(e) => setNewTask({ ...newTask, dueDate: e.target.value })} />
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">Related Lead</label>
                <select className="form-select" value={newTask.leadId} onChange={(e) => setNewTask({ ...newTask, leadId: e.target.value })}>
                  <option value="">-- Select Lead --</option>
                  {leads.map(l => (
                    <option key={l.id} value={l.id}>
                      {l.company ? `${l.firstName} ${l.lastName} (${l.company})` : `${l.firstName} ${l.lastName}`}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Assign To</label>
                <select className="form-select" value={newTask.assignedTo} onChange={(e) => setNewTask({ ...newTask, assignedTo: e.target.value })}>
                  <option value="">-- Assign Sales Rep --</option>
                  {users.map(u => (
                    <option key={u.id} value={u.id}>
                      {u.firstName} {u.lastName} ({u.role})
                    </option>
                  ))}
                </select>
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '1.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowCreateModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Create Task</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

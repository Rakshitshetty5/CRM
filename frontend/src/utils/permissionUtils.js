/**
 * Utility functions for frontend permission checks.
 * Note: Backend remains the source of truth for authorization.
 */

/**
 * Determines whether the given user has permission to edit a Lead.
 * Allowed when:
 * - Current user is ADMIN, OR
 * - Current user created the lead, OR
 * - Current user is assigned to the lead.
 */
export const canEditLead = (user, lead) => {
  if (!user || !lead) return false;
  if (user.role === 'ADMIN') return true;
  if (lead.createdBy && user.id === lead.createdBy) return true;
  if (lead.assignedTo && user.id === lead.assignedTo) return true;
  return false;
};

/**
 * Determines whether the given user has permission to edit a Task.
 * Allowed when:
 * - Current user is ADMIN, OR
 * - Current user created the task, OR
 * - Current user is assigned to the task.
 */
export const canEditTask = (user, task) => {
  if (!user || !task) return false;
  if (user.role === 'ADMIN') return true;
  if (task.createdBy && user.id === task.createdBy) return true;
  if (task.assignedTo && user.id === task.assignedTo) return true;
  return false;
};

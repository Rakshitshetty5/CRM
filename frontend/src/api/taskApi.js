import axiosClient from './axiosClient';

export const taskApi = {
  getTasks: async (params = {}) => {
    const response = await axiosClient.get('/api/v1/tasks', { params });
    return response.data;
  },

  getTaskById: async (taskId) => {
    const response = await axiosClient.get(`/api/v1/tasks/${taskId}`);
    return response.data;
  },

  createTask: async (taskData) => {
    const response = await axiosClient.post('/api/v1/tasks', taskData);
    return response.data;
  },

  updateTask: async (taskId, taskData) => {
    const response = await axiosClient.put(`/api/v1/tasks/${taskId}`, taskData);
    return response.data;
  },

  updateTaskStatus: async (taskId, status) => {
    const response = await axiosClient.patch(`/api/v1/tasks/${taskId}/status`, { status });
    return response.data;
  },
};

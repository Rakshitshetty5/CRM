import axiosClient from './axiosClient';

export const userApi = {
  getCurrentUser: async () => {
    const response = await axiosClient.get('/api/v1/users/me');
    return response.data;
  },

  getUsers: async (params = {}) => {
    const response = await axiosClient.get('/api/v1/users', { params });
    return response.data;
  },

  getUserById: async (userId) => {
    const response = await axiosClient.get(`/api/v1/users/${userId}`);
    return response.data;
  },

  createUser: async (userData) => {
    const response = await axiosClient.post('/api/v1/users', userData);
    return response.data;
  },

  updateUserStatus: async (userId, active) => {
    const response = await axiosClient.patch(`/api/v1/users/${userId}/status`, { active });
    return response.data;
  },
};

import axiosClient from './axiosClient';

export const notificationApi = {
  getNotifications: async (params = {}) => {
    const response = await axiosClient.get('/api/v1/notifications', { params });
    return response.data;
  },

  markAsRead: async (notificationId) => {
    const response = await axiosClient.patch(`/api/v1/notifications/${notificationId}/read`);
    return response.data;
  },
};

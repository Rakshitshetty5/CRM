import axiosClient from './axiosClient';

export const dashboardApi = {
  getSummary: async () => {
    const response = await axiosClient.get('/api/v1/dashboard/summary');
    return response.data;
  },
};

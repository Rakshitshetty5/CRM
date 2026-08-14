import axiosClient from './axiosClient';

export const leadApi = {
  getLeads: async (params = {}) => {
    const response = await axiosClient.get('/api/v1/leads', { params });
    return response.data;
  },

  getLeadById: async (leadId) => {
    const response = await axiosClient.get(`/api/v1/leads/${leadId}`);
    return response.data;
  },

  createLead: async (leadData) => {
    const response = await axiosClient.post('/api/v1/leads', leadData);
    return response.data;
  },

  updateLead: async (leadId, leadData) => {
    const response = await axiosClient.put(`/api/v1/leads/${leadId}`, leadData);
    return response.data;
  },

  updateLeadStatus: async (leadId, status) => {
    const response = await axiosClient.patch(`/api/v1/leads/${leadId}/status`, { status });
    return response.data;
  },

  assignLead: async (leadId, assignedTo) => {
    const response = await axiosClient.patch(`/api/v1/leads/${leadId}/assignment`, { assignedTo });
    return response.data;
  },

  getLeadActivities: async (leadId) => {
    const response = await axiosClient.get(`/api/v1/leads/${leadId}/activities`);
    return response.data;
  },
};

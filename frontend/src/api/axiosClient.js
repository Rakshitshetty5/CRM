import axios from 'axios';
import { toast } from '../utils/toast';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const axiosClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

axiosClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

axiosClient.interceptors.response.use(
  (response) => {
    const method = response.config?.method?.toLowerCase();
    const isWriteOp = ['post', 'put', 'patch', 'delete'].includes(method);

    if (response.data && typeof response.data === 'object' && response.data.success === true) {
      if (isWriteOp && response.data.message) {
        toast.success(response.data.message);
      }
      if (response.data.data !== undefined) {
        return { ...response, data: response.data.data };
      }
    }
    return response;
  },

  (error) => {
    const method = error.config?.method?.toLowerCase();
    const isWriteOp = ['post', 'put', 'patch', 'delete'].includes(method);

    if (isWriteOp) {
      const msg = error.response?.data?.message || 'Action failed. Please try again.';
      toast.error(msg);
    }

    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default axiosClient;


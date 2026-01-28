import axios from 'axios';

// Base API URL - uses proxy in development, direct in production
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

// Create axios instance with default config
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000, // 10 seconds
});

// Request interceptor for JWT token and logging
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    console.log(`[API Request] ${config.method.toUpperCase()} ${config.url}`, config.data);
    return config;
  },
  (error) => {
    console.error('[API Request Error]', error);
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => {
    console.log(`[API Response] ${response.status}`, response.data);
    return response;
  },
  (error) => {
    console.error('[API Response Error]', error.response || error);

    // Handle 401 Unauthorized - token expired or invalid
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    // Format error message
    const errorMessage = error.response?.data?.message ||
                        error.response?.data?.error ||
                        error.message ||
                        'An unexpected error occurred';

    const formattedError = {
      message: errorMessage,
      status: error.response?.status,
      details: error.response?.data?.details || [],
      timestamp: error.response?.data?.timestamp,
    };

    return Promise.reject(formattedError);
  }
);

// API Service
const api = {
  // User registration
  registerUser: async (userData) => {
    try {
      const response = await apiClient.post('/api/users/register', userData);
      return {
        success: true,
        data: response.data,
      };
    } catch (error) {
      return {
        success: false,
        error: error,
      };
    }
  },

  // User login
  login: async (credentials) => {
    return await apiClient.post('/api/auth/login', credentials);
  },

  // Get all users
  getUsers: async () => {
    return await apiClient.get('/api/users');
  },

  // Get single user by ID
  getUser: async (id) => {
    return await apiClient.get(`/api/users/${id}`);
  },

  // Update user
  updateUser: async (id, data) => {
    return await apiClient.put(`/api/users/${id}`, data);
  },

  // Delete user
  deleteUser: async (id) => {
    return await apiClient.delete(`/api/users/${id}`);
  },

  // Health check
  checkHealth: async () => {
    try {
      const response = await apiClient.get('/actuator/health');
      return {
        success: true,
        data: response.data,
      };
    } catch (error) {
      return {
        success: false,
        error: error,
      };
    }
  },
};

export default api;

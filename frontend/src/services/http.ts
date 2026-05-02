import axios from 'axios';

export class AppError extends Error {
  status?: number;
  details?: unknown;

  constructor(message: string, status?: number, details?: unknown) {
    super(message);
    this.name = 'AppError';
    this.status = status;
    this.details = details;
  }
}

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const http = axios.create({
  baseURL: `${API_BASE_URL}/api`,
  timeout: 30000,
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('career-agent-plus.auth-token');
  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const message =
      error.response?.data?.message ||
      error.response?.data?.detail ||
      error.message ||
      '请求失败，请稍后重试';

    return Promise.reject(new AppError(message, error.response?.status, error.response?.data));
  },
);

export default http;

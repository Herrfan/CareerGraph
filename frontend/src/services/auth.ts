import http from './http';
import type { AuthResponse, AuthUser } from '@/types/api';

export async function register(username: string, password: string): Promise<AuthResponse> {
  const response = await http.post<AuthResponse>('/auth/register', { username, password });
  return response.data;
}

export async function login(username: string, password: string): Promise<AuthResponse> {
  const response = await http.post<AuthResponse>('/auth/login', { username, password });
  return response.data;
}

export async function me(): Promise<AuthUser> {
  const response = await http.get<AuthUser>('/auth/me');
  return response.data;
}

export async function logout(): Promise<void> {
  await http.post('/auth/logout');
}

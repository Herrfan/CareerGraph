import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import type { AuthUser } from '@/types/api';
import { login, logout, me, register } from '@/services/auth';

const TOKEN_KEY = 'career-agent-plus.auth-token';

function readToken() {
  return localStorage.getItem(TOKEN_KEY) || '';
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(readToken());
  const currentUser = ref<AuthUser | null>(null);
  const isLoading = ref(false);

  const isAuthenticated = computed(() => !!token.value && !!currentUser.value);
  const isGuest = computed(() => !token.value && currentUser.value?.is_guest === true);
  const isAdmin = computed(() => currentUser.value?.role === 'ADMIN');

  function setToken(value: string) {
    token.value = value;
    if (value) {
      localStorage.setItem(TOKEN_KEY, value);
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
  }

  async function bootstrap() {
    if (!token.value) {
      currentUser.value = null;
      return null;
    }
    try {
      currentUser.value = await me();
      return currentUser.value;
    } catch {
      setToken('');
      currentUser.value = null;
      return null;
    }
  }

  async function loginWithPassword(username: string, password: string) {
    isLoading.value = true;
    try {
      const response = await login(username, password);
      setToken(response.token);
      currentUser.value = response.user;
      return response.user;
    } finally {
      isLoading.value = false;
    }
  }

  async function registerWithPassword(username: string, password: string) {
    isLoading.value = true;
    try {
      const response = await register(username, password);
      setToken(response.token);
      currentUser.value = response.user;
      return response.user;
    } finally {
      isLoading.value = false;
    }
  }

  function enterGuestMode() {
    setToken('');
    currentUser.value = {
      id: 0,
      username: '游客',
      role: 'GUEST',
      is_guest: true,
    };
  }

  async function logoutCurrentUser() {
    try {
      if (token.value) {
        await logout();
      }
    } finally {
      setToken('');
      currentUser.value = null;
    }
  }

  return {
    token,
    currentUser,
    isLoading,
    isAuthenticated,
    isGuest,
    isAdmin,
    bootstrap,
    loginWithPassword,
    registerWithPassword,
    enterGuestMode,
    logoutCurrentUser,
  };
});

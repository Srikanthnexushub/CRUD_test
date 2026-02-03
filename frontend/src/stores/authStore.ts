import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import api from '../services/api';
import { User, LoginResult, RegisterResult } from '../types';

interface AuthState {
  user: User | null;
  token: string | null;
  loading: boolean;
  isAuthenticated: boolean;
  isAdmin: boolean;

  // Actions
  login: (username: string, password: string) => Promise<LoginResult>;
  register: (username: string, email: string, password: string) => Promise<RegisterResult>;
  logout: () => void;
  setUser: (user: User | null) => void;
  setToken: (token: string | null) => void;
  canManageUser: (userId: number) => boolean;
  initializeAuth: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      loading: true,
      isAuthenticated: false,
      isAdmin: false,

      login: async (username: string, password: string): Promise<LoginResult> => {
        try {
          const response = await api.login({ username, password });
          const { token, id, username: userName, email, role, requiresMfa, tempToken } = response.data;

          // If MFA is required, don't store the token yet
          if (requiresMfa && tempToken) {
            return {
              success: true,
              requiresMfa: true,
              tempToken,
            };
          }

          const userData: User = { id, username: userName, email, role };

          set({
            token,
            user: userData,
            isAuthenticated: true,
            isAdmin: role === 'ROLE_ADMIN',
          });

          return { success: true };
        } catch (error: any) {
          console.error('Login error:', error);
          return {
            success: false,
            error: error?.message || 'Invalid username or password',
          };
        }
      },

      register: async (username: string, email: string, password: string): Promise<RegisterResult> => {
        try {
          await api.register({ username, email, password });
          return { success: true };
        } catch (error: any) {
          console.error('Registration error:', error);
          return {
            success: false,
            error: error?.message || 'Registration failed',
          };
        }
      },

      logout: (): void => {
        set({
          user: null,
          token: null,
          isAuthenticated: false,
          isAdmin: false,
        });
      },

      setUser: (user: User | null): void => {
        set({
          user,
          isAuthenticated: !!user,
          isAdmin: user?.role === 'ROLE_ADMIN',
        });
      },

      setToken: (token: string | null): void => {
        set({
          token,
          isAuthenticated: !!token,
        });
      },

      canManageUser: (userId: number): boolean => {
        const { user, isAdmin } = get();
        if (!user) return false;
        return isAdmin || user.id === userId;
      },

      initializeAuth: (): void => {
        set({ loading: false });
      },
    }),
    {
      name: 'auth-storage',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        isAuthenticated: state.isAuthenticated,
        isAdmin: state.isAdmin,
      }),
      onRehydrateStorage: () => (state) => {
        // Set loading to false after rehydration
        if (state) {
          state.loading = false;
        }
      },
    }
  )
);

import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import api from '../services/api';
import { User } from '../types';

interface UserState {
  users: User[];
  selectedUser: User | null;
  loading: boolean;
  error: string | null;

  // Pagination
  page: number;
  pageSize: number;
  totalPages: number;
  totalElements: number;

  // Filters
  searchTerm: string;
  roleFilter: string | null;

  // Actions
  fetchUsers: () => Promise<void>;
  fetchUser: (id: number) => Promise<void>;
  createUser: (userData: Partial<User>) => Promise<User>;
  updateUser: (id: number, userData: Partial<User>) => Promise<User>;
  deleteUser: (id: number) => Promise<void>;

  setSelectedUser: (user: User | null) => void;
  setSearchTerm: (term: string) => void;
  setRoleFilter: (role: string | null) => void;
  setPage: (page: number) => void;
  setPageSize: (size: number) => void;

  clearError: () => void;
  reset: () => void;
}

export const useUserStore = create<UserState>()(
  devtools(
    (set, get) => ({
      // Initial state
      users: [],
      selectedUser: null,
      loading: false,
      error: null,

      page: 0,
      pageSize: 20,
      totalPages: 0,
      totalElements: 0,

      searchTerm: '',
      roleFilter: null,

      // Actions
      fetchUsers: async (): Promise<void> => {
        set({ loading: true, error: null });
        try {
          const response = await api.getUsers();
          set({
            users: response.data,
            loading: false,
          });
        } catch (error: any) {
          set({
            error: error.message || 'Failed to fetch users',
            loading: false,
          });
        }
      },

      fetchUser: async (id: number): Promise<void> => {
        set({ loading: true, error: null });
        try {
          const response = await api.getUser(id);
          set({
            selectedUser: response.data,
            loading: false,
          });
        } catch (error: any) {
          set({
            error: error.message || 'Failed to fetch user',
            loading: false,
          });
        }
      },

      createUser: async (userData: Partial<User>): Promise<User> => {
        set({ loading: true, error: null });
        try {
          // API doesn't have create endpoint, so we'll use register
          await api.register({
            username: userData.username || '',
            email: userData.email || '',
            password: (userData as any).password || '',
          });

          // Refresh users list
          await get().fetchUsers();

          set({ loading: false });
          return userData as User;
        } catch (error: any) {
          set({
            error: error.message || 'Failed to create user',
            loading: false,
          });
          throw error;
        }
      },

      updateUser: async (id: number, userData: Partial<User>): Promise<User> => {
        set({ loading: true, error: null });
        try {
          const response = await api.updateUser(id, userData);

          // Update users list
          set((state) => ({
            users: state.users.map((u) => (u.id === id ? response.data : u)),
            selectedUser: state.selectedUser?.id === id ? response.data : state.selectedUser,
            loading: false,
          }));

          return response.data;
        } catch (error: any) {
          set({
            error: error.message || 'Failed to update user',
            loading: false,
          });
          throw error;
        }
      },

      deleteUser: async (id: number): Promise<void> => {
        set({ loading: true, error: null });
        try {
          await api.deleteUser(id);

          // Remove user from list
          set((state) => ({
            users: state.users.filter((u) => u.id !== id),
            selectedUser: state.selectedUser?.id === id ? null : state.selectedUser,
            loading: false,
          }));
        } catch (error: any) {
          set({
            error: error.message || 'Failed to delete user',
            loading: false,
          });
          throw error;
        }
      },

      setSelectedUser: (user: User | null): void => {
        set({ selectedUser: user });
      },

      setSearchTerm: (term: string): void => {
        set({ searchTerm: term });
      },

      setRoleFilter: (role: string | null): void => {
        set({ roleFilter: role });
      },

      setPage: (page: number): void => {
        set({ page });
      },

      setPageSize: (size: number): void => {
        set({ pageSize: size, page: 0 });
      },

      clearError: (): void => {
        set({ error: null });
      },

      reset: (): void => {
        set({
          users: [],
          selectedUser: null,
          loading: false,
          error: null,
          page: 0,
          pageSize: 20,
          totalPages: 0,
          totalElements: 0,
          searchTerm: '',
          roleFilter: null,
        });
      },
    }),
    { name: 'user-store' }
  )
);

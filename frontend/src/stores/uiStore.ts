import { create } from 'zustand';

interface Modal {
  id: string;
  type: 'confirm' | 'edit' | 'create' | 'info' | 'error';
  title: string;
  content?: React.ReactNode;
  data?: any;
  onConfirm?: () => void | Promise<void>;
  onCancel?: () => void;
}

interface Toast {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  duration?: number;
}

interface LoadingState {
  id: string;
  message?: string;
}

interface UIState {
  // Modal state
  modals: Modal[];
  activeModal: Modal | null;

  // Toast state
  toasts: Toast[];

  // Loading state
  loadingStates: LoadingState[];
  isLoading: boolean;

  // Sidebar state
  sidebarOpen: boolean;

  // Theme
  theme: 'light' | 'dark';

  // Actions
  openModal: (modal: Omit<Modal, 'id'>) => string;
  closeModal: (id: string) => void;
  closeAllModals: () => void;

  showToast: (toast: Omit<Toast, 'id'>) => string;
  hideToast: (id: string) => void;

  startLoading: (id: string, message?: string) => void;
  stopLoading: (id: string) => void;

  toggleSidebar: () => void;
  setSidebarOpen: (open: boolean) => void;

  setTheme: (theme: 'light' | 'dark') => void;
  toggleTheme: () => void;
}

export const useUIStore = create<UIState>((set, get) => ({
  // Initial state
  modals: [],
  activeModal: null,
  toasts: [],
  loadingStates: [],
  isLoading: false,
  sidebarOpen: true,
  theme: 'light',

  // Modal actions
  openModal: (modal: Omit<Modal, 'id'>): string => {
    const id = `modal-${Date.now()}-${Math.random()}`;
    const newModal: Modal = { ...modal, id };

    set((state) => ({
      modals: [...state.modals, newModal],
      activeModal: newModal,
    }));

    return id;
  },

  closeModal: (id: string): void => {
    set((state) => {
      const filteredModals = state.modals.filter((m) => m.id !== id);
      return {
        modals: filteredModals,
        activeModal: filteredModals[filteredModals.length - 1] || null,
      };
    });
  },

  closeAllModals: (): void => {
    set({
      modals: [],
      activeModal: null,
    });
  },

  // Toast actions
  showToast: (toast: Omit<Toast, 'id'>): string => {
    const id = `toast-${Date.now()}-${Math.random()}`;
    const newToast: Toast = { ...toast, id, duration: toast.duration || 5000 };

    set((state) => ({
      toasts: [...state.toasts, newToast],
    }));

    // Auto-hide toast after duration
    if (newToast.duration && newToast.duration > 0) {
      setTimeout(() => {
        get().hideToast(id);
      }, newToast.duration);
    }

    return id;
  },

  hideToast: (id: string): void => {
    set((state) => ({
      toasts: state.toasts.filter((t) => t.id !== id),
    }));
  },

  // Loading actions
  startLoading: (id: string, message?: string): void => {
    set((state) => ({
      loadingStates: [...state.loadingStates, { id, message }],
      isLoading: true,
    }));
  },

  stopLoading: (id: string): void => {
    set((state) => {
      const filteredLoadingStates = state.loadingStates.filter((l) => l.id !== id);
      return {
        loadingStates: filteredLoadingStates,
        isLoading: filteredLoadingStates.length > 0,
      };
    });
  },

  // Sidebar actions
  toggleSidebar: (): void => {
    set((state) => ({
      sidebarOpen: !state.sidebarOpen,
    }));
  },

  setSidebarOpen: (open: boolean): void => {
    set({ sidebarOpen: open });
  },

  // Theme actions
  setTheme: (theme: 'light' | 'dark'): void => {
    set({ theme });
    document.documentElement.setAttribute('data-theme', theme);
  },

  toggleTheme: (): void => {
    const currentTheme = get().theme;
    const newTheme = currentTheme === 'light' ? 'dark' : 'light';
    get().setTheme(newTheme);
  },
}));

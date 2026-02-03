export interface User {
  id: number;
  username: string;
  email: string;
  role: string;
  createdAt?: string;
  updatedAt?: string;
  lastLogin?: string;
  isDemo?: boolean;
}

export interface UserFormData {
  username: string;
  email: string;
  password?: string;
  role?: string;
}

export interface LoginCredentials {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  id: number;
  username: string;
  email: string;
  role: string;
  requiresMfa?: boolean;
  tempToken?: string;
}

export interface RegisterData {
  username: string;
  email: string;
  password: string;
}

export interface AuthContextType {
  user: User | null;
  token: string | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<LoginResult>;
  register: (username: string, email: string, password: string) => Promise<RegisterResult>;
  logout: () => void;
  canManageUser: (userId: number) => boolean;
  isAuthenticated: boolean;
  isAdmin: boolean;
}

export interface LoginResult {
  success: boolean;
  error?: string;
  requiresMfa?: boolean;
  tempToken?: string;
}

export interface RegisterResult {
  success: boolean;
  error?: string;
}

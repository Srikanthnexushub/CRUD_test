import React, { createContext, useState, useContext, useEffect, ReactNode } from 'react';
import api from '../services/api';
import { User, AuthContextType, LoginResult, RegisterResult } from '../types';

const AuthContext = createContext<AuthContextType | null>(null);

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    const storedToken = localStorage.getItem('token');
    const storedUser = localStorage.getItem('user');

    if (storedToken && storedUser) {
      setToken(storedToken);
      try {
        setUser(JSON.parse(storedUser));
      } catch (error) {
        console.error('Error parsing stored user data:', error);
        localStorage.removeItem('user');
      }
    }
    setLoading(false);
  }, []);

  const login = async (username: string, password: string): Promise<LoginResult> => {
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

      localStorage.setItem('token', token);
      localStorage.setItem('user', JSON.stringify(userData));

      setToken(token);
      setUser(userData);

      return { success: true };
    } catch (error: any) {
      console.error('Login error:', error);
      return {
        success: false,
        error: error?.message || 'Invalid username or password',
      };
    }
  };

  const register = async (username: string, email: string, password: string): Promise<RegisterResult> => {
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
  };

  const logout = (): void => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  };

  const canManageUser = (userId: number): boolean => {
    if (!user) return false;
    return user.role === 'ROLE_ADMIN' || user.id === userId;
  };

  const value: AuthContextType = {
    user,
    token,
    loading,
    login,
    register,
    logout,
    canManageUser,
    isAuthenticated: !!token,
    isAdmin: user?.role === 'ROLE_ADMIN',
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

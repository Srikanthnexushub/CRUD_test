import React, { useState, FormEvent, ChangeEvent } from 'react';
import { useAuth } from '../contexts/AuthContext';
import api from '../services/api';
import { User } from '../types';
import '../styles/UserEditModal.css';

interface UserEditModalProps {
  user: User;
  onClose: () => void;
  onSuccess: () => void;
}

interface UserFormData {
  username: string;
  email: string;
  password: string;
  role: string;
}

const UserEditModal: React.FC<UserEditModalProps> = ({ user, onClose, onSuccess }) => {
  const { isAdmin } = useAuth();
  const [formData, setFormData] = useState<UserFormData>({
    username: user.username,
    email: user.email,
    password: '',
    role: user.role
  });
  const [error, setError] = useState<string>('');
  const [isLoading, setIsLoading] = useState<boolean>(false);

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>): void => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
    setError('');
  };

  const handleSubmit = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      const updateData: Partial<User> = {};

      if (formData.username !== user.username) {
        updateData.username = formData.username;
      }

      if (formData.email !== user.email) {
        updateData.email = formData.email;
      }

      if (formData.password) {
        // Type assertion needed since User doesn't have password field
        (updateData as any).password = formData.password;
      }

      if (formData.role !== user.role && isAdmin) {
        updateData.role = formData.role;
      }

      if (Object.keys(updateData).length === 0) {
        setError('No changes to update');
        setIsLoading(false);
        return;
      }

      await api.updateUser(user.id, updateData);
      onSuccess();
    } catch (err: any) {
      setError(err.message || 'Failed to update user');
      setIsLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content edit-modal" onClick={(e) => e.stopPropagation()}>
        <h2>Edit User</h2>
        <form onSubmit={handleSubmit}>
          {error && (
            <div className="error-message">
              {error}
            </div>
          )}

          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input
              type="text"
              id="username"
              name="username"
              value={formData.username}
              onChange={handleChange}
              pattern="^[a-zA-Z0-9_]{3,50}$"
              title="Username must be 3-50 characters and contain only letters, numbers, and underscores"
              disabled={isLoading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              type="email"
              id="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              maxLength={100}
              disabled={isLoading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">
              New Password
              <small> (leave blank to keep current password)</small>
            </label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              pattern="^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$"
              title="Password must be at least 8 characters with uppercase, lowercase, and digit"
              disabled={isLoading}
            />
          </div>

          {isAdmin && (
            <div className="form-group">
              <label htmlFor="role">
                User Role
                <small> (admin can manage all users)</small>
              </label>
              <select
                id="role"
                name="role"
                value={formData.role}
                onChange={handleChange}
                disabled={isLoading}
                className="role-select"
              >
                <option value="ROLE_USER">Regular User</option>
                <option value="ROLE_ADMIN">Administrator</option>
              </select>
            </div>
          )}

          <div className="modal-actions">
            <button
              type="submit"
              className="btn-save"
              disabled={isLoading}
            >
              {isLoading ? 'Saving...' : 'Save Changes'}
            </button>
            <button
              type="button"
              onClick={onClose}
              className="btn-cancel"
              disabled={isLoading}
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default UserEditModal;

import React, { useState, useEffect, MouseEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore, useUserStore } from '../stores';
import UserEditModal from './UserEditModal';
import { User } from '../types';
import '../styles/UserDashboard.css';

const UserDashboard: React.FC = () => {
  const navigate = useNavigate();

  // Auth store
  const { user: currentUser, logout, canManageUser } = useAuthStore();

  // User store
  const { users, loading, error, fetchUsers } = useUserStore();

  // Local state
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [deleteConfirm, setDeleteConfirm] = useState<User | null>(null);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleLogout = (): void => {
    logout();
    navigate('/login');
  };

  const handleEdit = (user: User): void => {
    setEditingUser(user);
  };

  const handleDelete = async (userId: number): Promise<void> => {
    const deleteUser = useUserStore.getState().deleteUser;

    try {
      await deleteUser(userId);
      setDeleteConfirm(null);

      // If user deleted themselves, logout
      if (currentUser && userId === currentUser.id) {
        handleLogout();
      }
    } catch (err: any) {
      console.error('Delete error:', err);
    }
  };

  const handleUpdateSuccess = (): void => {
    setEditingUser(null);
    fetchUsers();
  };

  if (!currentUser) {
    return null;
  }

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <div className="header-content">
          <div className="header-left">
            <h1 className="brand-name">AI NEXUS HUB</h1>
            <span className="page-title">User Dashboard</span>
          </div>
          <div className="header-info">
            <span className="welcome-text">
              Welcome, <strong>{currentUser.username}</strong>
              {currentUser.role === 'ROLE_ADMIN' && (
                <span className="admin-badge">Admin</span>
              )}
            </span>
            <button onClick={handleLogout} className="logout-button">
              Logout
            </button>
          </div>
        </div>
      </header>

      <main className="dashboard-main">
        {error && (
          <div className="error-message">
            {error}
          </div>
        )}

        {loading ? (
          <div className="loading">Loading users...</div>
        ) : (
          <div className="users-grid">
            {users.map((user) => (
              <div key={user.id} className="user-card">
                <div className="user-card-header">
                  <h3>{user.username}</h3>
                  {user.role === 'ROLE_ADMIN' && (
                    <span className="role-badge admin">Admin</span>
                  )}
                  {user.role === 'ROLE_USER' && (
                    <span className="role-badge user">User</span>
                  )}
                </div>
                <div className="user-card-body">
                  <p><strong>Email:</strong> {user.email}</p>
                  <p><strong>ID:</strong> {user.id}</p>
                  {user.createdAt && (
                    <p className="user-dates">
                      <small>Created: {new Date(user.createdAt).toLocaleDateString()}</small>
                    </p>
                  )}
                </div>
                {canManageUser(user.id) && (
                  <div className="user-card-actions">
                    <button
                      onClick={() => handleEdit(user)}
                      className="btn-edit"
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => setDeleteConfirm(user)}
                      className="btn-delete"
                    >
                      Delete
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </main>

      {editingUser && (
        <UserEditModal
          user={editingUser}
          onClose={() => setEditingUser(null)}
          onSuccess={handleUpdateSuccess}
        />
      )}

      {deleteConfirm && (
        <div className="modal-overlay" onClick={() => setDeleteConfirm(null)}>
          <div className="modal-content confirm-dialog" onClick={(e: MouseEvent) => e.stopPropagation()}>
            <h3>Confirm Delete</h3>
            <p>
              Are you sure you want to delete user <strong>{deleteConfirm.username}</strong>?
              {deleteConfirm.id === currentUser.id && (
                <span className="warning-text">
                  <br />Warning: You are deleting your own account. You will be logged out.
                </span>
              )}
            </p>
            <div className="modal-actions">
              <button
                onClick={() => handleDelete(deleteConfirm.id)}
                className="btn-confirm"
              >
                Delete
              </button>
              <button
                onClick={() => setDeleteConfirm(null)}
                className="btn-cancel"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      <footer className="dashboard-footer">
        <p>All rights reserved | 2026-27</p>
      </footer>
    </div>
  );
};

export default UserDashboard;

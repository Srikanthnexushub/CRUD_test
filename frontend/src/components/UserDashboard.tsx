import React, { useState, useEffect, MouseEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore, useUserStore } from '../stores';
import UserEditModal from './UserEditModal';
import SkeletonLoader from './SkeletonLoader';
import ErrorFallback from './ErrorFallback';
import FocusTrap from './FocusTrap';
import { useAnnouncer } from '../hooks/useAnnouncer';
import { useKeyboardShortcuts } from '../hooks/useKeyboardShortcuts';
import { User } from '../types';
import '../styles/UserDashboard.css';

const UserDashboard: React.FC = () => {
  const navigate = useNavigate();
  const { announce } = useAnnouncer();

  // Auth store
  const { user: currentUser, logout, canManageUser } = useAuthStore();

  // User store
  const { users, loading, error, fetchUsers, clearError } = useUserStore();

  // Local state
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [deleteConfirm, setDeleteConfirm] = useState<User | null>(null);

  // Keyboard shortcuts
  useKeyboardShortcuts([
    {
      key: 'escape',
      handler: () => {
        if (editingUser) setEditingUser(null);
        if (deleteConfirm) setDeleteConfirm(null);
      },
      description: 'Close modal'
    }
  ]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleLogout = (): void => {
    announce('Logging out', 'polite');
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
      announce('User deleted successfully', 'polite');
      setDeleteConfirm(null);

      // If user deleted themselves, logout
      if (currentUser && userId === currentUser.id) {
        handleLogout();
      }
    } catch (err: any) {
      announce('Failed to delete user', 'assertive');
      console.error('Delete error:', err);
    }
  };

  const handleUpdateSuccess = (): void => {
    setEditingUser(null);
    fetchUsers();
  };

  const handleRetry = (): void => {
    clearError();
    fetchUsers();
  };

  if (!currentUser) {
    return null;
  }

  return (
    <div className="dashboard-container">
      <header className="dashboard-header" role="banner">
        <div className="header-content">
          <div className="header-left">
            <h1 className="brand-name">AI NEXUS HUB</h1>
            <span className="page-title">User Dashboard</span>
          </div>
          <div className="header-info">
            <span className="welcome-text" aria-label={`Welcome ${currentUser.username}${currentUser.role === 'ROLE_ADMIN' ? ', Administrator' : ''}`}>
              Welcome, <strong>{currentUser.username}</strong>
              {currentUser.role === 'ROLE_ADMIN' && (
                <span className="admin-badge" role="status">Admin</span>
              )}
            </span>
            <button onClick={handleLogout} className="logout-button" aria-label="Log out of your account">
              Logout
            </button>
          </div>
        </div>
      </header>

      <main id="main-content" className="dashboard-main" role="main" aria-busy={loading}>
        {error && (
          <ErrorFallback
            error={new Error(error)}
            resetError={handleRetry}
            title="Failed to load users"
            message="There was an error loading the user list. Please try again."
          />
        )}

        {loading && !error && (
          <div className="users-grid">
            <SkeletonLoader type="card" count={6} />
          </div>
        )}

        {!loading && !error && (
          <div className="users-grid" role="list" aria-label="User list">
            {users.length === 0 ? (
              <div className="empty-state" role="status">
                <p>No users found</p>
              </div>
            ) : (
              users.map((user) => (
                <article key={user.id} className="user-card" role="listitem" aria-label={`User ${user.username}`}>
                  <div className="user-card-header">
                    <h3>{user.username}</h3>
                    {user.role === 'ROLE_ADMIN' && (
                      <span className="role-badge admin" role="status" aria-label="Administrator role">Admin</span>
                    )}
                    {user.role === 'ROLE_USER' && (
                      <span className="role-badge user" role="status" aria-label="Regular user role">User</span>
                    )}
                  </div>
                  <div className="user-card-body">
                    <p>
                      <strong>Email:</strong> {user.email}
                    </p>
                    <p>
                      <strong>ID:</strong> {user.id}
                    </p>
                    {user.createdAt && (
                      <p className="user-dates">
                        <small>
                          Created: {new Date(user.createdAt).toLocaleDateString()}
                        </small>
                      </p>
                    )}
                  </div>
                  {canManageUser(user.id) && (
                    <div className="user-card-actions">
                      <button
                        onClick={() => handleEdit(user)}
                        className="btn-edit"
                        aria-label={`Edit ${user.username}`}
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => setDeleteConfirm(user)}
                        className="btn-delete"
                        aria-label={`Delete ${user.username}`}
                      >
                        Delete
                      </button>
                    </div>
                  )}
                </article>
              ))
            )}
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
        <div className="modal-overlay" onClick={() => setDeleteConfirm(null)} role="dialog" aria-modal="true" aria-labelledby="delete-modal-title">
          <FocusTrap onEscape={() => setDeleteConfirm(null)}>
            <div
              className="modal-content confirm-dialog"
              onClick={(e: MouseEvent) => e.stopPropagation()}
            >
              <h3 id="delete-modal-title">Confirm Delete</h3>
              <p>
                Are you sure you want to delete user <strong>{deleteConfirm.username}</strong>?
                {deleteConfirm.id === currentUser.id && (
                  <span className="warning-text" role="alert">
                    <br />
                    Warning: You are deleting your own account. You will be logged out.
                  </span>
                )}
              </p>
              <div className="modal-actions">
                <button
                  onClick={() => handleDelete(deleteConfirm.id)}
                  className="btn-confirm"
                  aria-label={`Confirm delete ${deleteConfirm.username}`}
                >
                  Delete
                </button>
                <button
                  onClick={() => setDeleteConfirm(null)}
                  className="btn-cancel"
                  aria-label="Cancel delete operation"
                >
                  Cancel
                </button>
              </div>
            </div>
          </FocusTrap>
        </div>
      )}

      <footer className="dashboard-footer" role="contentinfo">
        <p>All rights reserved | 2026-27</p>
      </footer>
    </div>
  );
};

export default UserDashboard;

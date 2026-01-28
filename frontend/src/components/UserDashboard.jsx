import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import UserEditModal from './UserEditModal';
import api from '../services/api';
import '../styles/UserDashboard.css';

function UserDashboard() {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [editingUser, setEditingUser] = useState(null);
    const [deleteConfirm, setDeleteConfirm] = useState(null);

    const { user: currentUser, logout, canManageUser } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        loadUsers();
    }, []);

    const loadUsers = async () => {
        try {
            setLoading(true);
            setError('');
            const response = await api.getUsers();
            setUsers(response.data);
        } catch (err) {
            setError(err.message || 'Failed to load users');
        } finally {
            setLoading(false);
        }
    };

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    const handleEdit = (user) => {
        setEditingUser(user);
    };

    const handleDelete = async (userId) => {
        try {
            await api.deleteUser(userId);
            setDeleteConfirm(null);

            // If user deleted themselves, logout
            if (userId === currentUser.id) {
                handleLogout();
            } else {
                loadUsers();
            }
        } catch (err) {
            setError(err.message || 'Failed to delete user');
        }
    };

    const handleUpdateSuccess = () => {
        setEditingUser(null);
        loadUsers();
    };

    return (
        <div className="dashboard-container">
            <header className="dashboard-header">
                <div className="header-content">
                    <h1>User Dashboard</h1>
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
                                    <p className="user-dates">
                                        <small>Created: {new Date(user.createdAt).toLocaleDateString()}</small>
                                    </p>
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
                    <div className="modal-content confirm-dialog" onClick={(e) => e.stopPropagation()}>
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
        </div>
    );
}

export default UserDashboard;

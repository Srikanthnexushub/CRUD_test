import React, { useState } from 'react';
import api from '../services/api';
import '../styles/UserEditModal.css';

function UserEditModal({ user, onClose, onSuccess }) {
    const [formData, setFormData] = useState({
        username: user.username,
        email: user.email,
        password: ''
    });
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value
        });
        setError('');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        try {
            const updateData = {};

            if (formData.username !== user.username) {
                updateData.username = formData.username;
            }

            if (formData.email !== user.email) {
                updateData.email = formData.email;
            }

            if (formData.password) {
                updateData.password = formData.password;
            }

            if (Object.keys(updateData).length === 0) {
                setError('No changes to update');
                setIsLoading(false);
                return;
            }

            await api.updateUser(user.id, updateData);
            onSuccess();
        } catch (err) {
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
}

export default UserEditModal;

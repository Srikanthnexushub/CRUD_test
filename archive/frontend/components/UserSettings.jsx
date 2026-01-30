import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import MFASettings from './MFA/MFASettings';
import NotificationPreferences from './Notifications/NotificationPreferences';
import '../styles/UserSettings.css';

function UserSettings() {
    const [activeTab, setActiveTab] = useState('profile');
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    const renderTabContent = () => {
        switch (activeTab) {
            case 'profile':
                return (
                    <div className="settings-section">
                        <h2>Profile Information</h2>
                        <div className="info-grid">
                            <div className="info-item">
                                <label>Username</label>
                                <div className="info-value">{user.username}</div>
                            </div>
                            <div className="info-item">
                                <label>Email</label>
                                <div className="info-value">{user.email}</div>
                            </div>
                            <div className="info-item">
                                <label>Role</label>
                                <div className="info-value">
                                    {user.role === 'ROLE_ADMIN' ? 'Administrator' : 'User'}
                                </div>
                            </div>
                            <div className="info-item">
                                <label>User ID</label>
                                <div className="info-value">{user.id}</div>
                            </div>
                        </div>
                    </div>
                );
            case 'security':
                return (
                    <div className="settings-section">
                        <h2>Security Settings</h2>
                        <MFASettings />
                    </div>
                );
            case 'notifications':
                return (
                    <div className="settings-section">
                        <h2>Notification Preferences</h2>
                        <NotificationPreferences />
                    </div>
                );
            default:
                return null;
        }
    };

    return (
        <div className="settings-container">
            <header className="settings-header">
                <div className="header-content">
                    <div className="header-left">
                        <h1 className="brand-name">AI NEXUS HUB</h1>
                        <span className="page-title">Account Settings</span>
                    </div>
                    <div className="header-right">
                        <span className="welcome-text">
                            {user.username}
                            {user.role === 'ROLE_ADMIN' && (
                                <span className="admin-badge">Admin</span>
                            )}
                        </span>
                        <button onClick={() => navigate('/dashboard')} className="btn-secondary">
                            Back to Dashboard
                        </button>
                        <button onClick={handleLogout} className="logout-button">
                            Logout
                        </button>
                    </div>
                </div>
            </header>

            <main className="settings-main">
                <div className="settings-tabs">
                    <button
                        className={`tab-button ${activeTab === 'profile' ? 'active' : ''}`}
                        onClick={() => setActiveTab('profile')}
                    >
                        Profile
                    </button>
                    <button
                        className={`tab-button ${activeTab === 'security' ? 'active' : ''}`}
                        onClick={() => setActiveTab('security')}
                    >
                        Security
                    </button>
                    <button
                        className={`tab-button ${activeTab === 'notifications' ? 'active' : ''}`}
                        onClick={() => setActiveTab('notifications')}
                    >
                        Notifications
                    </button>
                </div>

                <div className="settings-content">
                    {renderTabContent()}
                </div>
            </main>

            <footer className="settings-footer">
                <p>All rights reserved | 2026-27</p>
            </footer>
        </div>
    );
}

export default UserSettings;

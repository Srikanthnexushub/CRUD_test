import React, { useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import NotificationPreferences from './NotificationPreferences';
import EmailDashboard from './EmailDashboard';
import EmailLogTable from './EmailLogTable';
import EmailTemplateEditor from './EmailTemplateEditor';
import SmtpConfigModal from './SmtpConfigModal';
import '../../styles/Notifications.css';

/**
 * NotificationHub - Main component that manages all notification-related views
 * This component provides tabs to navigate between different notification features
 */
const NotificationHub = () => {
    const { isAdmin } = useAuth();
    const [activeTab, setActiveTab] = useState('preferences');
    const [showSmtpModal, setShowSmtpModal] = useState(false);

    const tabs = [
        { id: 'preferences', label: 'Preferences', icon: 'fa-cog', adminOnly: false },
        { id: 'dashboard', label: 'Dashboard', icon: 'fa-chart-line', adminOnly: true },
        { id: 'logs', label: 'Email Logs', icon: 'fa-list', adminOnly: true },
        { id: 'templates', label: 'Templates', icon: 'fa-file-code', adminOnly: true },
    ];

    // Filter tabs based on user role
    const availableTabs = tabs.filter(tab => !tab.adminOnly || isAdmin());

    const renderTabContent = () => {
        switch (activeTab) {
            case 'preferences':
                return <NotificationPreferences />;
            case 'dashboard':
                return <EmailDashboard />;
            case 'logs':
                return <EmailLogTable />;
            case 'templates':
                return <EmailTemplateEditor />;
            default:
                return <NotificationPreferences />;
        }
    };

    return (
        <div className="notification-hub">
            <div className="hub-header">
                <div className="hub-title-section">
                    <h1>
                        <i className="fas fa-bell"></i>
                        Notification Center
                    </h1>
                    <p className="hub-subtitle">
                        Manage email notifications, templates, and preferences
                    </p>
                </div>
                {isAdmin() && (
                    <button
                        className="btn-smtp-config"
                        onClick={() => setShowSmtpModal(true)}
                    >
                        <i className="fas fa-server"></i>
                        SMTP Settings
                    </button>
                )}
            </div>

            <div className="hub-tabs">
                {availableTabs.map((tab) => (
                    <button
                        key={tab.id}
                        className={`tab-button ${activeTab === tab.id ? 'active' : ''}`}
                        onClick={() => setActiveTab(tab.id)}
                    >
                        <i className={`fas ${tab.icon}`}></i>
                        <span>{tab.label}</span>
                    </button>
                ))}
            </div>

            <div className="hub-content">
                {renderTabContent()}
            </div>

            <SmtpConfigModal
                isOpen={showSmtpModal}
                onClose={() => setShowSmtpModal(false)}
                onSave={(config) => {
                    console.log('SMTP config saved:', config);
                    // Optionally show a success message
                }}
            />
        </div>
    );
};

export default NotificationHub;

import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import websocketService from '../services/websocket';
import api from '../services/api';
import LiveAnalyticsCharts from './LiveAnalyticsCharts';
import ThreatIntelligencePanel from './Threat/ThreatIntelligencePanel';
import RateLimitDashboard from './RateLimit/RateLimitDashboard';
import EmailDashboard from './Notifications/EmailDashboard';
import '../styles/SOCDashboard.css';

const SOCDashboard = () => {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('overview');
    const [realtimeEvents, setRealtimeEvents] = useState([]);
    const [securityAlerts, setSecurityAlerts] = useState([]);
    const [stats, setStats] = useState({
        todayEvents: 0,
        failedLogins: 0,
        activeUsers: 0,
        criticalAlerts: 0
    });
    const [connected, setConnected] = useState(false);
    const [notifications, setNotifications] = useState([]);
    const [loading, setLoading] = useState(true);
    const maxEvents = 50; // Keep only last 50 events
    const dataLoadedRef = useRef(false); // Prevent duplicate API calls

    const currentUser = JSON.parse(localStorage.getItem('user'));

    useEffect(() => {
        // Check if user is admin
        if (!currentUser || currentUser.role !== 'ROLE_ADMIN') {
            navigate('/dashboard');
            return;
        }

        // Load historical data first
        loadDashboardData();

        // Connect to WebSocket
        websocketService.connect(
            () => {
                console.log('Connected to real-time dashboard');
                setConnected(true);

                // Subscribe to audit events
                websocketService.subscribeToAuditEvents((event) => {
                    const eventWithKey = {
                        ...event,
                        uniqueKey: `realtime-${event.id || Date.now()}-${Math.random()}`, // Unique key for React
                        severity: getSeverity(event.eventType)
                    };

                    setRealtimeEvents(prev => {
                        const updated = [eventWithKey, ...prev];
                        return updated.slice(0, maxEvents); // Keep only last 50
                    });

                    // Update stats based on event
                    updateStatsIncremental(event);
                });

                // Subscribe to security alerts
                websocketService.subscribeToSecurityAlerts((alert) => {
                    const alertWithKey = {
                        ...alert,
                        uniqueKey: `realtime-alert-${alert.id || Date.now()}-${Math.random()}`, // Unique key for React
                        severity: 'HIGH'
                    };

                    setSecurityAlerts(prev => {
                        const updated = [alertWithKey, ...prev];
                        return updated.slice(0, 20); // Keep only last 20 alerts
                    });

                    // Update critical alerts count
                    setStats(prev => ({
                        ...prev,
                        criticalAlerts: prev.criticalAlerts + 1
                    }));

                    // Show browser notification for high severity alerts
                    showNotification(alert);
                });

                // Subscribe to system notifications
                websocketService.subscribeToNotifications((notification) => {
                    addNotification(notification.message, notification.severity);
                });
            },
            (error) => {
                console.error('WebSocket connection error:', error);
                setConnected(false);
            }
        );

        return () => {
            websocketService.disconnect();
            dataLoadedRef.current = false; // Reset on unmount
        };
    }, [navigate]); // Remove currentUser from dependencies to prevent re-renders

    const loadDashboardData = async () => {
        // Prevent duplicate calls
        if (dataLoadedRef.current) {
            console.log('Dashboard data already loaded, skipping duplicate call');
            return;
        }

        try {
            setLoading(true);
            console.log('Loading dashboard data...');

            const response = await api.getDashboardStats();
            const data = response.data;

            dataLoadedRef.current = true; // Mark as loaded

            // Set stats from historical data
            setStats({
                todayEvents: data.todayEvents || 0,
                failedLogins: data.failedLogins || 0,
                activeUsers: 0, // Will be calculated from sessions
                criticalAlerts: data.criticalAlerts || 0
            });

            // Set recent events with unique keys
            if (data.recentEvents && data.recentEvents.length > 0) {
                const formattedEvents = data.recentEvents.map((event, idx) => ({
                    ...event,
                    uniqueKey: `historical-${event.id}-${idx}`, // Unique key for React
                    severity: getSeverity(event.eventType)
                }));
                setRealtimeEvents(formattedEvents);
            }

            // Set recent alerts with unique keys
            if (data.recentAlerts && data.recentAlerts.length > 0) {
                const formattedAlerts = data.recentAlerts.map((alert, idx) => ({
                    ...alert,
                    uniqueKey: `alert-${alert.id}-${idx}`, // Unique key for React
                    severity: 'HIGH'
                }));
                setSecurityAlerts(formattedAlerts);
            }

            console.log('Dashboard data loaded successfully:', data);
            setLoading(false);
        } catch (error) {
            console.error('Error loading dashboard data:', error);
            setLoading(false);
            // Only show error notification if it's not a network timeout
            if (error.message !== 'Network Error') {
                addNotification('Failed to load dashboard data: ' + (error.message || 'Unknown error'), 'HIGH');
            }
        }
    };

    const getSeverity = (eventType) => {
        if (['LOGIN_FAILURE', 'ACCESS_DENIED', 'SUSPICIOUS_ACTIVITY'].includes(eventType)) {
            return 'HIGH';
        } else if (['USER_DELETED', 'USER_UPDATED'].includes(eventType)) {
            return 'MEDIUM';
        }
        return 'LOW';
    };

    const updateStatsIncremental = (event) => {
        setStats(prev => {
            const updated = { ...prev };
            updated.todayEvents = prev.todayEvents + 1;

            if (event.eventType === 'LOGIN_FAILURE') {
                updated.failedLogins = prev.failedLogins + 1;
            }

            if (['LOGIN_FAILURE', 'ACCESS_DENIED', 'SUSPICIOUS_ACTIVITY'].includes(event.eventType)) {
                updated.criticalAlerts = prev.criticalAlerts + 1;
            }

            return updated;
        });
    };

    const showNotification = (alert) => {
        if (alert.severity === 'HIGH' && 'Notification' in window) {
            if (Notification.permission === 'granted') {
                new Notification('Security Alert', {
                    body: alert.action,
                    icon: '/alert-icon.png',
                    badge: '/badge-icon.png'
                });
            } else if (Notification.permission !== 'denied') {
                Notification.requestPermission().then(permission => {
                    if (permission === 'granted') {
                        new Notification('Security Alert', {
                            body: alert.action,
                        });
                    }
                });
            }
        }

        addNotification(alert.action, alert.severity);
    };

    const addNotification = (message, severity) => {
        const notification = {
            id: Date.now(),
            message,
            severity,
            timestamp: new Date()
        };

        setNotifications(prev => [...prev, notification]);

        // Auto-remove after 5 seconds
        setTimeout(() => {
            setNotifications(prev => prev.filter(n => n.id !== notification.id));
        }, 5000);
    };

    const getEventIcon = (eventType) => {
        const icons = {
            'LOGIN_SUCCESS': '✓',
            'LOGIN_FAILURE': '✗',
            'LOGOUT': '←',
            'USER_CREATED': '+',
            'USER_UPDATED': '✎',
            'USER_DELETED': '⊗',
            'USER_VIEWED': '👁',
            'ACCESS_DENIED': '🚫',
            'SUSPICIOUS_ACTIVITY': '⚠'
        };
        return icons[eventType] || '•';
    };

    const getEventClass = (eventType) => {
        const classes = {
            'LOGIN_SUCCESS': 'event-success',
            'LOGIN_FAILURE': 'event-danger',
            'LOGOUT': 'event-info',
            'USER_CREATED': 'event-success',
            'USER_UPDATED': 'event-warning',
            'USER_DELETED': 'event-danger',
            'USER_VIEWED': 'event-info',
            'ACCESS_DENIED': 'event-danger',
            'SUSPICIOUS_ACTIVITY': 'event-critical'
        };
        return classes[eventType] || 'event-default';
    };

    const formatTime = (timestamp) => {
        const date = new Date(timestamp);
        return date.toLocaleTimeString('en-US', {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    };

    const formatEventType = (eventType) => {
        return eventType.replace(/_/g, ' ');
    };

    const handleLogout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        navigate('/login');
    };

    const renderTabContent = () => {
        switch (activeTab) {
            case 'overview':
                return (
                    <>
                        {/* Stats Grid */}
                        <div className="stats-grid">
                            <div className="stat-card">
                                <div className="stat-icon stat-blue">📊</div>
                                <div className="stat-content">
                                    <div className="stat-value">{stats.todayEvents}</div>
                                    <div className="stat-label">Events Today</div>
                                </div>
                            </div>
                            <div className="stat-card">
                                <div className="stat-icon stat-red">⚠</div>
                                <div className="stat-content">
                                    <div className="stat-value">{stats.failedLogins}</div>
                                    <div className="stat-label">Failed Logins</div>
                                </div>
                            </div>
                            <div className="stat-card">
                                <div className="stat-icon stat-green">👥</div>
                                <div className="stat-content">
                                    <div className="stat-value">{realtimeEvents.filter(e => e.eventType === 'LOGIN_SUCCESS').length}</div>
                                    <div className="stat-label">Active Sessions</div>
                                </div>
                            </div>
                            <div className="stat-card">
                                <div className="stat-icon stat-orange">🔔</div>
                                <div className="stat-content">
                                    <div className="stat-value">{stats.criticalAlerts}</div>
                                    <div className="stat-label">Critical Alerts</div>
                                </div>
                            </div>
                        </div>

                        {/* Main Content Grid */}
                        <div className="content-grid">
                            {/* Live Activity Feed */}
                            <div className="panel live-feed">
                                <div className="panel-header">
                                    <h2>Live Activity Feed</h2>
                                    <span className="event-count">{realtimeEvents.length} events</span>
                                </div>
                                <div className="panel-content">
                                    <div className="events-list">
                                        {realtimeEvents.length === 0 ? (
                                            <div className="no-events">
                                                <p>Waiting for events...</p>
                                                <span className="pulse-dot"></span>
                                            </div>
                                        ) : (
                                            realtimeEvents.map((event) => (
                                                <div key={event.uniqueKey} className={`event-item ${getEventClass(event.eventType)}`}>
                                                    <div className="event-icon">
                                                        {getEventIcon(event.eventType)}
                                                    </div>
                                                    <div className="event-details">
                                                        <div className="event-header">
                                                            <span className="event-type">{formatEventType(event.eventType)}</span>
                                                            <span className="event-time">{formatTime(event.timestamp)}</span>
                                                        </div>
                                                        <div className="event-action">{event.action}</div>
                                                        <div className="event-meta">
                                                            <span className="event-user">{event.username}</span>
                                                            {event.ipAddress && (
                                                                <span className="event-ip">IP: {event.ipAddress}</span>
                                                            )}
                                                        </div>
                                                    </div>
                                                    {event.severity === 'HIGH' && (
                                                        <div className="severity-badge severity-high">HIGH</div>
                                                    )}
                                                </div>
                                            ))
                                        )}
                                    </div>
                                </div>
                            </div>

                            {/* Security Alerts */}
                            <div className="panel security-alerts">
                                <div className="panel-header">
                                    <h2>Security Alerts</h2>
                                    <span className="alert-count">{securityAlerts.length} alerts</span>
                                </div>
                                <div className="panel-content">
                                    <div className="alerts-list">
                                        {securityAlerts.length === 0 ? (
                                            <div className="no-alerts">
                                                <p>No security alerts</p>
                                                <span className="checkmark">✓</span>
                                            </div>
                                        ) : (
                                            securityAlerts.map((alert) => (
                                                <div key={alert.uniqueKey} className="alert-item">
                                                    <div className="alert-icon">⚠</div>
                                                    <div className="alert-details">
                                                        <div className="alert-action">{alert.action}</div>
                                                        <div className="alert-meta">
                                                            <span>{alert.username}</span>
                                                            <span>{formatTime(alert.timestamp)}</span>
                                                        </div>
                                                    </div>
                                                    <div className={`severity-badge severity-${alert.severity.toLowerCase()}`}>
                                                        {alert.severity}
                                                    </div>
                                                </div>
                                            ))
                                        )}
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Live Analytics Charts */}
                        <LiveAnalyticsCharts realtimeEvents={realtimeEvents} />
                    </>
                );
            case 'threat':
                return <ThreatIntelligencePanel />;
            case 'ratelimit':
                return <RateLimitDashboard />;
            case 'email':
                return <EmailDashboard />;
            default:
                return null;
        }
    };

    return (
        <div className="soc-dashboard">
            <header className="dashboard-header">
                <div className="header-left">
                    <h1 className="brand-name">AI NEXUS HUB</h1>
                    <span className="page-title">Security Operations Center</span>
                </div>
                <div className="header-right">
                    <div className={`connection-status ${connected ? 'connected' : 'disconnected'}`}>
                        <span className="status-dot"></span>
                        {connected ? 'Live' : 'Disconnected'}
                    </div>
                    <span className="username">{currentUser?.username}</span>
                    <button onClick={() => navigate('/dashboard')} className="btn-secondary">
                        Back to Dashboard
                    </button>
                    <button onClick={handleLogout} className="btn-logout">
                        Logout
                    </button>
                </div>
            </header>

            {/* Toast Notifications */}
            <div className="toast-container">
                {notifications.map(notif => (
                    <div key={notif.id} className={`toast toast-${notif.severity.toLowerCase()}`}>
                        <span className="toast-message">{notif.message}</span>
                        <button
                            className="toast-close"
                            onClick={() => setNotifications(prev => prev.filter(n => n.id !== notif.id))}
                        >
                            ×
                        </button>
                    </div>
                ))}
            </div>

            {/* Tab Navigation */}
            <div className="soc-tabs">
                <button
                    className={`tab-button ${activeTab === 'overview' ? 'active' : ''}`}
                    onClick={() => setActiveTab('overview')}
                >
                    Overview
                </button>
                <button
                    className={`tab-button ${activeTab === 'threat' ? 'active' : ''}`}
                    onClick={() => setActiveTab('threat')}
                >
                    Threat Intelligence
                </button>
                <button
                    className={`tab-button ${activeTab === 'ratelimit' ? 'active' : ''}`}
                    onClick={() => setActiveTab('ratelimit')}
                >
                    Rate Limiting
                </button>
                <button
                    className={`tab-button ${activeTab === 'email' ? 'active' : ''}`}
                    onClick={() => setActiveTab('email')}
                >
                    Email Notifications
                </button>
            </div>

            {/* Tab Content */}
            <div className="soc-content">
                {renderTabContent()}
            </div>

            <footer className="dashboard-footer">
                <p>All rights reserved | 2026-27 | Real-time monitoring active</p>
            </footer>
        </div>
    );
};

export default SOCDashboard;

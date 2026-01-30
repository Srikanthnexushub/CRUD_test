import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
    constructor() {
        this.client = null;
        this.connected = false;
        this.subscriptions = {};
    }

    connect(onConnected, onError) {
        if (this.connected) {
            console.log('WebSocket already connected');
            return;
        }

        // Get WebSocket URL from environment or use current host
        const wsUrl = import.meta.env.VITE_WS_URL ||
                     `${window.location.protocol}//${window.location.hostname}:8080/ws`;
        console.log('Connecting to WebSocket:', wsUrl);

        // Create STOMP client with SockJS
        this.client = new Client({
            webSocketFactory: () => new SockJS(wsUrl),
            connectHeaders: {},
            debug: (str) => {
                console.log('STOMP: ' + str);
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        this.client.onConnect = (frame) => {
            console.log('WebSocket Connected:', frame);
            this.connected = true;
            if (onConnected) onConnected();
        };

        this.client.onStompError = (frame) => {
            console.error('STOMP Error:', frame);
            this.connected = false;
            if (onError) onError(frame);
        };

        this.client.onWebSocketClose = () => {
            console.log('WebSocket Disconnected');
            this.connected = false;
        };

        this.client.activate();
    }

    disconnect() {
        if (this.client && this.connected) {
            this.client.deactivate();
            this.connected = false;
            this.subscriptions = {};
            console.log('WebSocket Disconnected');
        }
    }

    subscribeToAuditEvents(callback) {
        if (!this.connected) {
            console.error('WebSocket not connected');
            return null;
        }

        const subscription = this.client.subscribe('/topic/audit-events', (message) => {
            const event = JSON.parse(message.body);
            callback(event);
        });

        this.subscriptions['audit-events'] = subscription;
        return subscription;
    }

    subscribeToSecurityAlerts(callback) {
        if (!this.connected) {
            console.error('WebSocket not connected');
            return null;
        }

        const subscription = this.client.subscribe('/topic/security-alerts', (message) => {
            const alert = JSON.parse(message.body);
            callback(alert);
        });

        this.subscriptions['security-alerts'] = subscription;
        return subscription;
    }

    subscribeToNotifications(callback) {
        if (!this.connected) {
            console.error('WebSocket not connected');
            return null;
        }

        const subscription = this.client.subscribe('/topic/notifications', (message) => {
            const notification = JSON.parse(message.body);
            callback(notification);
        });

        this.subscriptions['notifications'] = subscription;
        return subscription;
    }

    subscribeToUserNotifications(username, callback) {
        if (!this.connected) {
            console.error('WebSocket not connected');
            return null;
        }

        const subscription = this.client.subscribe(`/topic/user/${username}`, (message) => {
            const notification = JSON.parse(message.body);
            callback(notification);
        });

        this.subscriptions[`user-${username}`] = subscription;
        return subscription;
    }

    unsubscribe(subscriptionKey) {
        if (this.subscriptions[subscriptionKey]) {
            this.subscriptions[subscriptionKey].unsubscribe();
            delete this.subscriptions[subscriptionKey];
        }
    }

    sendMessage(destination, message) {
        if (this.connected && this.client) {
            this.client.publish({
                destination: destination,
                body: JSON.stringify(message),
            });
        } else {
            console.error('WebSocket not connected');
        }
    }

    requestStats() {
        this.sendMessage('/app/stats', {});
    }
}

export default new WebSocketService();

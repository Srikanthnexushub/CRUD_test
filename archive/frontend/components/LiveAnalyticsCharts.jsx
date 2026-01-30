import React, { useState, useEffect } from 'react';
import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    BarElement,
    ArcElement,
    Title,
    Tooltip,
    Legend,
    Filler
} from 'chart.js';
import { Line, Bar, Doughnut } from 'react-chartjs-2';
import api from '../services/api';
import '../styles/LiveAnalyticsCharts.css';

// Register ChartJS components
ChartJS.register(
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    BarElement,
    ArcElement,
    Title,
    Tooltip,
    Legend,
    Filler
);

const LiveAnalyticsCharts = ({ realtimeEvents }) => {
    const [loginTrends, setLoginTrends] = useState(null);
    const [eventDistribution, setEventDistribution] = useState(null);
    const [hourlyActivity, setHourlyActivity] = useState(null);

    useEffect(() => {
        // Initialize charts with historical data
        loadHistoricalData();

        // Update charts every 10 seconds with new events
        const interval = setInterval(() => {
            updateChartsWithRealtimeData();
        }, 10000);

        return () => clearInterval(interval);
    }, []);

    useEffect(() => {
        // Update charts when new realtime events arrive
        if (realtimeEvents && realtimeEvents.length > 0) {
            updateChartsWithRealtimeData();
        }
    }, [realtimeEvents]);

    const loadHistoricalData = async () => {
        try {
            // Get audit logs from last 24 hours
            const endDate = new Date();
            const startDate = new Date(endDate.getTime() - 24 * 60 * 60 * 1000);

            const response = await api.getAuditLogs({
                startDate: startDate.toISOString(),
                endDate: endDate.toISOString(),
                size: 1000
            });

            const logs = response.data.content;
            processDataForCharts(logs);
        } catch (error) {
            console.error('Failed to load historical data:', error);
        }
    };

    const updateChartsWithRealtimeData = () => {
        if (realtimeEvents && realtimeEvents.length > 0) {
            processDataForCharts(realtimeEvents);
        }
    };

    const processDataForCharts = (events) => {
        // Process Login Trends (last 12 hours)
        const hours = 12;
        const now = new Date();
        const loginData = Array(hours).fill(0).map(() => ({ success: 0, failure: 0 }));
        const labels = [];

        for (let i = hours - 1; i >= 0; i--) {
            const hour = new Date(now.getTime() - i * 60 * 60 * 1000);
            labels.push(hour.getHours() + ':00');
        }

        events.forEach(event => {
            const eventTime = new Date(event.timestamp);
            const hoursAgo = Math.floor((now - eventTime) / (1000 * 60 * 60));

            if (hoursAgo < hours) {
                const index = hours - 1 - hoursAgo;
                if (event.eventType === 'LOGIN_SUCCESS') {
                    loginData[index].success++;
                } else if (event.eventType === 'LOGIN_FAILURE') {
                    loginData[index].failure++;
                }
            }
        });

        setLoginTrends({
            labels,
            datasets: [
                {
                    label: 'Successful Logins',
                    data: loginData.map(d => d.success),
                    borderColor: 'rgb(75, 192, 192)',
                    backgroundColor: 'rgba(75, 192, 192, 0.2)',
                    fill: true,
                    tension: 0.4
                },
                {
                    label: 'Failed Logins',
                    data: loginData.map(d => d.failure),
                    borderColor: 'rgb(255, 99, 132)',
                    backgroundColor: 'rgba(255, 99, 132, 0.2)',
                    fill: true,
                    tension: 0.4
                }
            ]
        });

        // Process Event Distribution
        const eventTypes = {};
        events.forEach(event => {
            const type = event.eventType || 'UNKNOWN';
            eventTypes[type] = (eventTypes[type] || 0) + 1;
        });

        const sortedEvents = Object.entries(eventTypes)
            .sort((a, b) => b[1] - a[1])
            .slice(0, 6); // Top 6 event types

        setEventDistribution({
            labels: sortedEvents.map(([type]) => type.replace(/_/g, ' ')),
            datasets: [{
                label: 'Events',
                data: sortedEvents.map(([, count]) => count),
                backgroundColor: [
                    'rgba(255, 99, 132, 0.8)',
                    'rgba(54, 162, 235, 0.8)',
                    'rgba(255, 206, 86, 0.8)',
                    'rgba(75, 192, 192, 0.8)',
                    'rgba(153, 102, 255, 0.8)',
                    'rgba(255, 159, 64, 0.8)',
                ],
                borderWidth: 2,
                borderColor: '#fff'
            }]
        });

        // Process Hourly Activity Pattern
        const hourlyData = Array(24).fill(0);
        events.forEach(event => {
            const hour = new Date(event.timestamp).getHours();
            hourlyData[hour]++;
        });

        setHourlyActivity({
            labels: Array.from({ length: 24 }, (_, i) => i + ':00'),
            datasets: [{
                label: 'Activity',
                data: hourlyData,
                backgroundColor: 'rgba(102, 126, 234, 0.8)',
                borderColor: 'rgb(102, 126, 234)',
                borderWidth: 1
            }]
        });
    };

    const lineChartOptions = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: {
                position: 'top',
            },
            title: {
                display: true,
                text: 'Login Trends (Last 12 Hours)'
            }
        },
        scales: {
            y: {
                beginAtZero: true,
                ticks: {
                    stepSize: 1
                }
            }
        }
    };

    const doughnutChartOptions = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: {
                position: 'right',
            },
            title: {
                display: true,
                text: 'Event Distribution'
            }
        }
    };

    const barChartOptions = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: {
                display: false
            },
            title: {
                display: true,
                text: '24-Hour Activity Pattern'
            }
        },
        scales: {
            y: {
                beginAtZero: true,
                ticks: {
                    stepSize: 1
                }
            }
        }
    };

    return (
        <div className="analytics-charts-container">
            <div className="chart-card">
                <div className="chart-wrapper">
                    {loginTrends ? (
                        <Line data={loginTrends} options={lineChartOptions} />
                    ) : (
                        <div className="chart-loading">Loading chart data...</div>
                    )}
                </div>
            </div>

            <div className="chart-card">
                <div className="chart-wrapper">
                    {eventDistribution ? (
                        <Doughnut data={eventDistribution} options={doughnutChartOptions} />
                    ) : (
                        <div className="chart-loading">Loading chart data...</div>
                    )}
                </div>
            </div>

            <div className="chart-card chart-card-wide">
                <div className="chart-wrapper">
                    {hourlyActivity ? (
                        <Bar data={hourlyActivity} options={barChartOptions} />
                    ) : (
                        <div className="chart-loading">Loading chart data...</div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default LiveAnalyticsCharts;

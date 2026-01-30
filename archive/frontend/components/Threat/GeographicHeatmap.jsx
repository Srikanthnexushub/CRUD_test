import React, { useState, useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import MarkerClusterGroup from 'react-leaflet-cluster';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import '../../styles/Threat.css';

// Fix for default marker icons in React-Leaflet
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
    iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

// Custom marker icons based on risk level
const createCustomIcon = (riskScore) => {
    let color = '#10b981'; // green
    if (riskScore >= 80) color = '#ef4444'; // red
    else if (riskScore >= 60) color = '#f97316'; // orange
    else if (riskScore >= 40) color = '#eab308'; // yellow

    return L.divIcon({
        className: 'custom-marker',
        html: `<div style="background-color: ${color}; width: 24px; height: 24px; border-radius: 50%; border: 2px solid white; box-shadow: 0 2px 4px rgba(0,0,0,0.3);"></div>`,
        iconSize: [24, 24],
        iconAnchor: [12, 12],
    });
};

const MapCenterController = ({ center }) => {
    const map = useMap();

    useEffect(() => {
        if (center) {
            map.setView(center, map.getZoom());
        }
    }, [center, map]);

    return null;
};

const GeographicHeatmap = ({ apiBaseUrl = '/api/threat' }) => {
    const [threats, setThreats] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedCountry, setSelectedCountry] = useState('all');
    const [countries, setCountries] = useState([]);
    const [mapCenter, setMapCenter] = useState([20, 0]); // Default center
    const [stats, setStats] = useState({ total: 0, highRisk: 0, blocked: 0 });

    useEffect(() => {
        loadThreats();
        const interval = setInterval(loadThreats, 60000); // Refresh every minute
        return () => clearInterval(interval);
    }, []);

    useEffect(() => {
        // Extract unique countries from threats
        const uniqueCountries = [...new Set(threats
            .filter(t => t.geolocation?.country)
            .map(t => t.geolocation.country))];
        setCountries(uniqueCountries.sort());
    }, [threats]);

    const loadThreats = async () => {
        try {
            setLoading(true);
            const token = localStorage.getItem('token');
            const response = await fetch(`${apiBaseUrl}/assessments/recent?limit=100`, {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) throw new Error('Failed to load threat data');

            const data = await response.json();

            // Filter threats with valid geolocation
            const validThreats = data.filter(t =>
                t.geolocation?.latitude &&
                t.geolocation?.longitude &&
                !isNaN(t.geolocation.latitude) &&
                !isNaN(t.geolocation.longitude)
            );

            setThreats(validThreats);

            // Calculate stats
            const stats = {
                total: validThreats.length,
                highRisk: validThreats.filter(t => t.riskScore >= 60).length,
                blocked: validThreats.filter(t => !t.allowed).length
            };
            setStats(stats);

            setError(null);
        } catch (err) {
            console.error('Error loading threat map data:', err);
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleCountryFilter = (country) => {
        setSelectedCountry(country);

        if (country !== 'all') {
            // Find first threat in selected country and center map there
            const countryThreat = threats.find(t => t.geolocation?.country === country);
            if (countryThreat) {
                setMapCenter([
                    countryThreat.geolocation.latitude,
                    countryThreat.geolocation.longitude
                ]);
            }
        } else {
            setMapCenter([20, 0]); // Reset to default
        }
    };

    const filteredThreats = selectedCountry === 'all'
        ? threats
        : threats.filter(t => t.geolocation?.country === selectedCountry);

    const formatTimestamp = (timestamp) => {
        const date = new Date(timestamp);
        return date.toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    const getRiskLabel = (score) => {
        if (score < 40) return 'LOW';
        if (score < 60) return 'MEDIUM';
        if (score < 80) return 'HIGH';
        return 'CRITICAL';
    };

    return (
        <div className="geographic-heatmap">
            <div className="heatmap-header">
                <h2>Geographic Threat Map</h2>
                <div className="heatmap-controls">
                    <div className="country-filter">
                        <label>Filter by Country:</label>
                        <select
                            value={selectedCountry}
                            onChange={(e) => handleCountryFilter(e.target.value)}
                            className="country-select"
                        >
                            <option value="all">All Countries ({threats.length})</option>
                            {countries.map(country => {
                                const count = threats.filter(t => t.geolocation?.country === country).length;
                                return (
                                    <option key={country} value={country}>
                                        {country} ({count})
                                    </option>
                                );
                            })}
                        </select>
                    </div>
                    <button className="btn-refresh" onClick={loadThreats} disabled={loading}>
                        {loading ? '↻' : '⟳'}
                    </button>
                </div>
            </div>

            {/* Map Statistics */}
            <div className="map-stats">
                <div className="map-stat">
                    <span className="stat-label">Total Threats:</span>
                    <span className="stat-value">{stats.total}</span>
                </div>
                <div className="map-stat">
                    <span className="stat-label">High Risk:</span>
                    <span className="stat-value stat-warning">{stats.highRisk}</span>
                </div>
                <div className="map-stat">
                    <span className="stat-label">Blocked:</span>
                    <span className="stat-value stat-danger">{stats.blocked}</span>
                </div>
            </div>

            {/* Legend */}
            <div className="map-legend">
                <div className="legend-item">
                    <div className="legend-marker" style={{ backgroundColor: '#10b981' }}></div>
                    <span>Low (0-39)</span>
                </div>
                <div className="legend-item">
                    <div className="legend-marker" style={{ backgroundColor: '#eab308' }}></div>
                    <span>Medium (40-59)</span>
                </div>
                <div className="legend-item">
                    <div className="legend-marker" style={{ backgroundColor: '#f97316' }}></div>
                    <span>High (60-79)</span>
                </div>
                <div className="legend-item">
                    <div className="legend-marker" style={{ backgroundColor: '#ef4444' }}></div>
                    <span>Critical (80-100)</span>
                </div>
            </div>

            {error && (
                <div className="error-message">
                    <span className="error-icon">⚠</span>
                    {error}
                </div>
            )}

            {/* Map Container */}
            <div className="map-container">
                {loading && threats.length === 0 ? (
                    <div className="map-loading">Loading map data...</div>
                ) : filteredThreats.length === 0 ? (
                    <div className="map-no-data">No threat data available for selected filter</div>
                ) : (
                    <MapContainer
                        center={mapCenter}
                        zoom={2}
                        style={{ height: '100%', width: '100%' }}
                        scrollWheelZoom={true}
                    >
                        <TileLayer
                            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                        />
                        <MapCenterController center={mapCenter} />
                        <MarkerClusterGroup>
                            {filteredThreats.map((threat, index) => (
                                <Marker
                                    key={`${threat.id}-${index}`}
                                    position={[
                                        threat.geolocation.latitude,
                                        threat.geolocation.longitude
                                    ]}
                                    icon={createCustomIcon(threat.riskScore)}
                                >
                                    <Popup>
                                        <div className="threat-popup">
                                            <div className="popup-header">
                                                <h3>Threat Assessment</h3>
                                                <span className={`risk-badge risk-${getRiskLabel(threat.riskScore).toLowerCase()}`}>
                                                    {threat.riskScore} - {getRiskLabel(threat.riskScore)}
                                                </span>
                                            </div>
                                            <div className="popup-content">
                                                <div className="popup-row">
                                                    <strong>User:</strong> {threat.username || 'Unknown'}
                                                </div>
                                                <div className="popup-row">
                                                    <strong>IP:</strong> {threat.ipAddress}
                                                </div>
                                                <div className="popup-row">
                                                    <strong>Location:</strong> {threat.geolocation.city}, {threat.geolocation.country}
                                                </div>
                                                <div className="popup-row">
                                                    <strong>Time:</strong> {formatTimestamp(threat.timestamp)}
                                                </div>
                                                <div className="popup-row">
                                                    <strong>Status:</strong>{' '}
                                                    <span className={threat.allowed ? 'status-allowed' : 'status-blocked'}>
                                                        {threat.allowed ? 'Allowed' : 'Blocked'}
                                                    </span>
                                                </div>
                                                {threat.riskFactors && threat.riskFactors.length > 0 && (
                                                    <div className="popup-factors">
                                                        <strong>Risk Factors:</strong>
                                                        <ul>
                                                            {threat.riskFactors.slice(0, 3).map((factor, i) => (
                                                                <li key={i}>{factor}</li>
                                                            ))}
                                                        </ul>
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    </Popup>
                                </Marker>
                            ))}
                        </MarkerClusterGroup>
                    </MapContainer>
                )}
            </div>
        </div>
    );
};

export default GeographicHeatmap;

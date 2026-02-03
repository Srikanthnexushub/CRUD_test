-- V10: Add threat intelligence table
-- Stores IP threat information, risk assessments, and security intelligence

CREATE TABLE IF NOT EXISTS threat_intelligence (
    id BIGSERIAL PRIMARY KEY,
    ip_address VARCHAR(45) NOT NULL,
    risk_score INTEGER NOT NULL DEFAULT 0,
    threat_type VARCHAR(50),
    threat_category VARCHAR(50),
    country_code VARCHAR(2),
    is_vpn BOOLEAN DEFAULT FALSE,
    is_proxy BOOLEAN DEFAULT FALSE,
    is_tor BOOLEAN DEFAULT FALSE,
    is_datacenter BOOLEAN DEFAULT FALSE,
    is_blacklisted BOOLEAN DEFAULT FALSE,
    failed_login_count INTEGER DEFAULT 0,
    suspicious_activity_count INTEGER DEFAULT 0,
    last_seen TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    expires_at TIMESTAMP,
    notes TEXT,
    source VARCHAR(50),

    CONSTRAINT unique_ip_address UNIQUE (ip_address)
);

-- Indexes for performance
CREATE INDEX idx_threat_ip_address ON threat_intelligence(ip_address);
CREATE INDEX idx_threat_risk_score ON threat_intelligence(risk_score DESC);
CREATE INDEX idx_threat_expires_at ON threat_intelligence(expires_at);
CREATE INDEX idx_threat_created_at ON threat_intelligence(created_at DESC);
CREATE INDEX idx_threat_blacklisted ON threat_intelligence(is_blacklisted) WHERE is_blacklisted = TRUE;
CREATE INDEX idx_threat_high_risk ON threat_intelligence(risk_score DESC) WHERE risk_score >= 60;
CREATE INDEX idx_threat_tor ON threat_intelligence(is_tor) WHERE is_tor = TRUE;
CREATE INDEX idx_threat_vpn_proxy ON threat_intelligence(is_vpn, is_proxy) WHERE is_vpn = TRUE OR is_proxy = TRUE;

-- Comments for documentation
COMMENT ON TABLE threat_intelligence IS 'IP threat intelligence and risk assessment data';
COMMENT ON COLUMN threat_intelligence.ip_address IS 'IP address being tracked';
COMMENT ON COLUMN threat_intelligence.risk_score IS 'Calculated risk score (0-100)';
COMMENT ON COLUMN threat_intelligence.threat_type IS 'Type of threat detected (e.g., brute_force, sql_injection)';
COMMENT ON COLUMN threat_intelligence.threat_category IS 'Category classification (e.g., BLACKLISTED, SUSPICIOUS)';
COMMENT ON COLUMN threat_intelligence.country_code IS 'ISO 3166-1 alpha-2 country code';
COMMENT ON COLUMN threat_intelligence.is_vpn IS 'Whether IP is identified as VPN';
COMMENT ON COLUMN threat_intelligence.is_proxy IS 'Whether IP is identified as proxy';
COMMENT ON COLUMN threat_intelligence.is_tor IS 'Whether IP is Tor exit node';
COMMENT ON COLUMN threat_intelligence.is_datacenter IS 'Whether IP belongs to datacenter (AWS, Azure, GCP, etc.)';
COMMENT ON COLUMN threat_intelligence.is_blacklisted IS 'Whether IP is blacklisted';
COMMENT ON COLUMN threat_intelligence.failed_login_count IS 'Number of failed login attempts';
COMMENT ON COLUMN threat_intelligence.suspicious_activity_count IS 'Number of suspicious activities detected';
COMMENT ON COLUMN threat_intelligence.last_seen IS 'Last time this IP was observed';
COMMENT ON COLUMN threat_intelligence.expires_at IS 'When this threat intelligence record expires';
COMMENT ON COLUMN threat_intelligence.source IS 'Source of intelligence (INTERNAL, ABUSEIPDB, etc.)';

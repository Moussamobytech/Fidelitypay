-- =============================================================================
-- Developer Portal Database Setup
-- =============================================================================
-- This script will be automatically executed by Hibernate with ddl-auto=update
-- But you can also run it manually for explicit table creation

-- =============================================================================
-- 1. API KEYS TABLE
-- =============================================================================
-- Stores one API key as a lookup identifier plus a secure secret hash.
-- IMPORTANT: Never store the complete API key in plain text!

CREATE TABLE IF NOT EXISTS api_keys (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    public_key VARCHAR(255) UNIQUE NOT NULL,
    secret_key_hash VARCHAR(255) NOT NULL,
    secret_key_hint VARCHAR(10),
    environment VARCHAR(10) NOT NULL CHECK (environment IN ('sandbox', 'live')),
    last_used_at TIMESTAMP NULL,
    last_used_ip VARCHAR(45),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    
    INDEX idx_api_key_user_id (user_id),
    INDEX idx_api_key_public_key (public_key),
    INDEX idx_api_key_environment (environment),
    INDEX idx_api_key_active (is_active)
);

-- =============================================================================
-- 2. API REQUEST LOGS TABLE
-- =============================================================================
-- Tracks all API requests for metrics and monitoring

CREATE TABLE IF NOT EXISTS api_request_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    api_key_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL,
    endpoint VARCHAR(500) NOT NULL,
    status_code INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    latency_ms BIGINT NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_api_log_api_key_id (api_key_id),
    INDEX idx_api_log_user_id (user_id),
    INDEX idx_api_log_status (status),
    INDEX idx_api_log_created_at (created_at),
    INDEX idx_api_log_status_code (status_code)
);

-- =============================================================================
-- 3. WEBHOOKS TABLE
-- =============================================================================
-- User-configured webhook endpoints for event notifications

CREATE TABLE IF NOT EXISTS webhooks (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    event VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    secret VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_triggered_at TIMESTAMP NULL,
    last_status_code INT,
    failure_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    
    INDEX idx_webhook_user_id (user_id),
    INDEX idx_webhook_event (event),
    INDEX idx_webhook_active (is_active)
);

-- =============================================================================
-- CLEANUP QUERIES (Optional - for maintenance)
-- =============================================================================

-- Delete logs older than 90 days (run periodically)
-- DELETE FROM api_request_logs WHERE created_at < DATE_SUB(NOW(), INTERVAL 90 DAY);

-- Find inactive API keys (not used in 30 days)
-- SELECT * FROM api_keys WHERE last_used_at < DATE_SUB(NOW(), INTERVAL 30 DAY) AND is_active = TRUE;

-- Find failing webhooks
-- SELECT * FROM webhooks WHERE failure_count > 5 AND is_active = TRUE;

-- =============================================================================
-- VERIFICATION QUERIES
-- =============================================================================

-- Check if tables were created successfully
SELECT 
    'api_keys' as table_name, 
    COUNT(*) as row_count 
FROM api_keys
UNION ALL
SELECT 
    'api_request_logs', 
    COUNT(*) 
FROM api_request_logs
UNION ALL
SELECT 
    'webhooks', 
    COUNT(*) 
FROM webhooks;

-- =============================================================================
-- END OF SCRIPT
-- =============================================================================

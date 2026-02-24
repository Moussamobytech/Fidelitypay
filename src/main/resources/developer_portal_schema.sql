-- =============================================================================
-- Developer Portal Database Setup
-- =============================================================================
-- This script will be automatically executed by Hibernate with ddl-auto=update
-- But you can also run it manually for explicit table creation

-- =============================================================================
-- 1. API KEYS TABLE
-- =============================================================================
-- Stores API key pairs with secure hashing
-- IMPORTANT: Never store secret keys in plain text!

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
    metadata TEXT,
    
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
    metadata TEXT,
    
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
-- SAMPLE DATA (For Testing - Remove in Production)
-- =============================================================================

-- Sample API Key for testing (password: sk_sandbox_test123456)
-- Note: This is a BCrypt hash - in real usage, use the API to create keys
INSERT INTO api_keys (id, user_id, name, public_key, secret_key_hash, secret_key_hint, environment, is_active, created_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    'demo-user',
    'Test Sandbox Key',
    'pk_sandbox_test123abc',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LeWWY21s0e0e0e0e',
    '1234',
    'sandbox',
    TRUE,
    NOW()
) ON DUPLICATE KEY UPDATE name=name;

-- Sample API Request Logs
INSERT INTO api_request_logs (api_key_id, user_id, method, endpoint, status_code, status, ip_address, latency_ms, created_at)
VALUES 
    ('550e8400-e29b-41d4-a716-446655440000', 'demo-user', 'POST', '/api/v1/payments/initiate', 200, 'SUCCESS', '192.168.1.1', 245, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    ('550e8400-e29b-41d4-a716-446655440000', 'demo-user', 'GET', '/api/v1/payments/status', 200, 'SUCCESS', '192.168.1.1', 123, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    ('550e8400-e29b-41d4-a716-446655440000', 'demo-user', 'POST', '/api/v1/payments/initiate', 500, 'ERROR', '192.168.1.2', 1234, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
    ('550e8400-e29b-41d4-a716-446655440000', 'demo-user', 'POST', '/api/v1/payments/refund', 200, 'SUCCESS', '192.168.1.1', 456, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
    ('550e8400-e29b-41d4-a716-446655440000', 'demo-user', 'GET', '/api/v1/payments/list', 200, 'SUCCESS', '192.168.1.1', 189, DATE_SUB(NOW(), INTERVAL 5 HOUR)),
    ('550e8400-e29b-41d4-a716-446655440000', 'demo-user', 'POST', '/api/v1/payments/initiate', 400, 'VALIDATION_ERROR', '192.168.1.3', 67, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
    ('550e8400-e29b-41d4-a716-446655440000', 'demo-user', 'POST', '/api/v1/payments/initiate', 200, 'SUCCESS', '192.168.1.1', 278, DATE_SUB(NOW(), INTERVAL 7 HOUR)),
    ('550e8400-e29b-41d4-a716-446655440000', 'demo-user', 'GET', '/api/v1/payments/status', 404, 'ERROR', '192.168.1.1', 98, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
    ('550e8400-e29b-41d4-a716-446655440000', 'demo-user', 'POST', '/api/v1/payments/initiate', 200, 'SUCCESS', '192.168.1.1', 312, DATE_SUB(NOW(), INTERVAL 10 HOUR)),
    ('550e8400-e29b-41d4-a716-446655440000', 'demo-user', 'GET', '/api/v1/developer/metrics', 200, 'SUCCESS', '192.168.1.1', 45, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
    ('550e8400-e29b-41d4-a716-446655440000', 'demo-user', 'POST', '/api/v1/payments/initiate', 200, 'SUCCESS', '192.168.1.1', 234, DATE_SUB(NOW(), INTERVAL 15 HOUR)),
    ('550e8400-e29b-41d4-a716-446655440000', 'demo-user', 'GET', '/api/v1/payments/status', 200, 'SUCCESS', '192.168.1.1', 156, DATE_SUB(NOW(), INTERVAL 18 HOUR)),
    ('550e8400-e29b-41d4-a716-446655440000', 'demo-user', 'POST', '/api/v1/payments/initiate', 503, 'TIMEOUT', '192.168.1.1', 5000, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
    ('550e8400-e29b-41d4-a716-446655440000', 'demo-user', 'POST', '/api/v1/payments/initiate', 200, 'SUCCESS', '192.168.1.1', 267, DATE_SUB(NOW(), INTERVAL 22 HOUR)),
    ('550e8400-e29b-41d4-a716-446655440000', 'demo-user', 'GET', '/api/v1/developer/activity', 200, 'SUCCESS', '192.168.1.1', 89, DATE_SUB(NOW(), INTERVAL 23 HOUR))
ON DUPLICATE KEY UPDATE method=method;

-- Sample Webhook
INSERT INTO webhooks (id, user_id, url, event, description, secret, is_active, created_at)
VALUES (
    '770e8400-e29b-41d4-a716-446655440000',
    'demo-user',
    'https://example.com/webhooks/payments',
    'payment.success',
    'Production payment success notifications',
    'whsec_test123abc',
    TRUE,
    NOW()
) ON DUPLICATE KEY UPDATE url=url;

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

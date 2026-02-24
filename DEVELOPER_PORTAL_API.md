# Developer Portal API - Complete Implementation Guide

## 📋 Table of Contents
1. [Overview](#overview)
2. [Database Schema](#database-schema)
3. [API Endpoints](#api-endpoints)
4. [Authentication](#authentication)
5. [Usage Examples](#usage-examples)
6. [Security Best Practices](#security-best-practices)

## 🎯 Overview

This implementation provides a complete backend for a Developer Portal with:
- **API Key Management**: Generate, rotate, and revoke API keys securely
- **Metrics & Monitoring**: Track API usage, success rates, latency, and more
- **Activity Logs**: View detailed logs of all API requests
- **Webhook Management**: Configure callback URLs for events

## 🗄️ Database Schema

### 1. `api_keys` Table
Stores API keys with secure hashing (NEVER stores secrets in plain text).

```sql
CREATE TABLE api_keys (
    id VARCHAR(255) PRIMARY KEY,            -- UUID
    user_id VARCHAR(255) NOT NULL,          -- User who owns this key
    name VARCHAR(100) NOT NULL,             -- Key label/description
    public_key VARCHAR(255) UNIQUE NOT NULL,-- pk_live_xxx or pk_sandbox_xxx
    secret_key_hash VARCHAR(255) NOT NULL,  -- Hashed secret key (BCrypt)
    secret_key_hint VARCHAR(10),            -- Last 4 chars for display
    environment VARCHAR(10) NOT NULL,       -- 'sandbox' or 'live'
    last_used_at TIMESTAMP,                 -- Last usage timestamp
    last_used_ip VARCHAR(45),               -- Last IP address
    is_active BOOLEAN NOT NULL DEFAULT TRUE,-- Can be revoked
    created_at TIMESTAMP NOT NULL,          -- Creation time
    expires_at TIMESTAMP,                   -- Optional expiration
    metadata TEXT,                          -- Additional notes
    
    INDEX idx_api_key_user_id (user_id),
    INDEX idx_api_key_public_key (public_key),
    INDEX idx_api_key_environment (environment),
    INDEX idx_api_key_active (is_active)
);
```

### 2. `api_request_logs` Table
Tracks all API requests for metrics and monitoring.

```sql
CREATE TABLE api_request_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    api_key_id VARCHAR(255) NOT NULL,       -- API key used
    user_id VARCHAR(255) NOT NULL,          -- User ID
    method VARCHAR(10) NOT NULL,            -- HTTP method
    endpoint VARCHAR(500) NOT NULL,         -- API endpoint
    status_code INT NOT NULL,               -- HTTP status code
    status VARCHAR(20) NOT NULL,            -- SUCCESS, ERROR, etc.
    ip_address VARCHAR(45),                 -- Client IP
    user_agent VARCHAR(500),                -- Client user agent
    latency_ms BIGINT NOT NULL,             -- Response time
    error_message TEXT,                     -- Error details if failed
    created_at TIMESTAMP NOT NULL,          -- Request timestamp
    metadata TEXT,                          -- Additional data
    
    INDEX idx_api_log_api_key_id (api_key_id),
    INDEX idx_api_log_user_id (user_id),
    INDEX idx_api_log_status (status),
    INDEX idx_api_log_created_at (created_at),
    INDEX idx_api_log_status_code (status_code)
);
```

### 3. `webhooks` Table
User-configured webhook endpoints for event notifications.

```sql
CREATE TABLE webhooks (
    id VARCHAR(255) PRIMARY KEY,            -- UUID
    user_id VARCHAR(255) NOT NULL,          -- Owner
    url VARCHAR(1000) NOT NULL,             -- Callback URL
    event VARCHAR(100) NOT NULL,            -- Event type
    description VARCHAR(255),               -- Notes
    secret VARCHAR(255),                    -- HMAC secret
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_triggered_at TIMESTAMP,            -- Last webhook call
    last_status_code INT,                   -- Last HTTP status
    failure_count INT NOT NULL DEFAULT 0,   -- Consecutive failures
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    
    INDEX idx_webhook_user_id (user_id),
    INDEX idx_webhook_event (event),
    INDEX idx_webhook_active (is_active)
);
```

## 🔌 API Endpoints

### API Key Management

#### 1. Get All API Keys
```http
GET /api/v1/developer/keys
Headers:
  X-User-Id: {userId}
Query Parameters:
  environment: sandbox|live (optional)

Response 200:
[
  {
    "id": "uuid",
    "name": "Production Server",
    "publicKey": "pk_live_abc123...",
    "secretKey": null,  // Only shown once at creation
    "secretKeyMasked": "sk_live_****1234",
    "environment": "live",
    "isActive": true,
    "createdAt": "2024-01-15T10:30:00",
    "lastUsedAt": "2024-01-20T14:45:00",
    "lastUsedIp": "192.168.1.1",
    "expiresAt": null,
    "metadata": "Server key for production API"
  }
]
```

#### 2. Create New API Key
```http
POST /api/v1/developer/keys
Headers:
  X-User-Id: {userId}
  Content-Type: application/json

Body:
{
  "name": "Mobile App Key",
  "environment": "sandbox",
  "metadata": "Key for mobile app testing"
}

Response 201:
{
  "id": "uuid",
  "name": "Mobile App Key",
  "publicKey": "pk_sandbox_xyz789...",
  "secretKey": "sk_sandbox_full_secret_key_shown_ONCE",  // ⚠️ SAVE THIS!
  "secretKeyMasked": "sk_sandbox_****6789",
  "environment": "sandbox",
  "isActive": true,
  "createdAt": "2024-01-20T15:00:00",
  ...
}
```

#### 3. Revoke API Key
```http
POST /api/v1/developer/keys/{id}/revoke
Headers:
  X-User-Id: {userId}

Response 200:
{
  "message": "API key revoked successfully",
  "keyId": "uuid"
}
```

#### 4. Rotate All Keys
```http
POST /api/v1/developer/keys/rotate
Headers:
  X-User-Id: {userId}

Response 200:
{
  "message": "All API keys have been rotated successfully",
  "newKeys": [
    { ...sandboxKey with full secret... },
    { ...liveKey with full secret... }
  ]
}
```

### Metrics & Monitoring

#### 5. Get Metrics
```http
GET /api/v1/developer/metrics
Headers:
  X-User-Id: {userId}
Query Parameters:
  period: last_hour|last_24h|last_7d|last_30d (default: last_24h)

Response 200:
{
  "totalRequests": 15234,
  "successfulRequests": 14892,
  "failedRequests": 342,
  "successRate": 97.76,
  "errorRate": 2.24,
  "avgLatency": 245.5,
  "period": "last_24h",
  "hourlyBreakdown": [
    {
      "hour": "2024-01-20 14:00:00",
      "count": 523
    },
    ...
  ],
  "topEndpoints": [
    {
      "endpoint": "/api/v1/payments/initiate",
      "count": 5432
    },
    ...
  ]
}
```

#### 6. Get Activity Logs
```http
GET /api/v1/developer/activity
Headers:
  X-User-Id: {userId}
Query Parameters:
  limit: 1-100 (default: 10)
  filter: all|errors|slow (default: all)

Response 200:
[
  {
    "id": 123456,
    "method": "POST",
    "endpoint": "/api/v1/payments/initiate",
    "statusCode": 200,
    "status": "SUCCESS",
    "ipAddress": "192.168.1.1",
    "latencyMs": 234,
    "errorMessage": null,
    "createdAt": "2024-01-20T15:30:00"
  },
  ...
]
```

### Webhook Management

#### 7. Get Webhooks
```http
GET /api/v1/developer/webhooks
Headers:
  X-User-Id: {userId}
Query Parameters:
  event: payment.success|payment.failed|... (optional)

Response 200:
[
  {
    "id": "uuid",
    "url": "https://myapp.com/webhooks/payments",
    "event": "payment.success",
    "description": "Production payment notifications",
    "isActive": true,
    "lastTriggeredAt": "2024-01-20T14:00:00",
    "lastStatusCode": 200,
    "failureCount": 0,
    "createdAt": "2024-01-15T10:00:00"
  }
]
```

#### 8. Create Webhook
```http
POST /api/v1/developer/webhooks
Headers:
  X-User-Id: {userId}
  Content-Type: application/json

Body:
{
  "url": "https://myapp.com/webhooks/payments",
  "event": "payment.success",
  "description": "Production payment notifications"
}

Response 201:
{
  "id": "uuid",
  "url": "https://myapp.com/webhooks/payments",
  "event": "payment.success",
  ...
}
```

#### 9. Update Webhook Status
```http
PATCH /api/v1/developer/webhooks/{id}
Headers:
  X-User-Id: {userId}
  Content-Type: application/json

Body:
{
  "isActive": false
}

Response 200: { webhook object }
```

#### 10. Delete Webhook
```http
DELETE /api/v1/developer/webhooks/{id}
Headers:
  X-User-Id: {userId}

Response 200:
{
  "message": "Webhook deleted successfully",
  "webhookId": "uuid"
}
```

## 🔐 Authentication

**Current Implementation (Testing)**:
- Uses `X-User-Id` header for user identification
- **⚠️ For PRODUCTION**: Replace with proper JWT authentication or session-based auth

**Production Implementation**:
```java
// In SecurityConfig.java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/developer/**").authenticated()
            .anyRequest().permitAll()
        )
        .oauth2ResourceServer(oauth2 -> oauth2.jwt());
    return http.build();
}
```

Then extract user ID from JWT token:
```java
@GetMapping("/keys")
public ResponseEntity<List<ApiKeyResponse>> getApiKeys(@AuthenticationPrincipal Jwt jwt) {
    String userId = jwt.getSubject();
    // ...
}
```

## 📝 Usage Examples

### Example 1: Frontend Integration (React/Angular)

```javascript
// Create a new API key
async function createApiKey(name, environment) {
  const response = await fetch('http://localhost:8080/api/v1/developer/keys', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-User-Id': 'user-123'  // Replace with actual auth
    },
    body: JSON.stringify({
      name: name,
      environment: environment,
      metadata: 'Created from dashboard'
    })
  });
  
  const data = await response.json();
  
  // ⚠️ IMPORTANT: Show the secret key to user IMMEDIATELY
  // It will never be retrievable again!
  alert(`Save this secret key: ${data.secretKey}`);
  
  return data;
}

// Get metrics
async function getDashboardMetrics() {
  const response = await fetch(
    'http://localhost:8080/api/v1/developer/metrics?period=last_24h',
    {
      headers: { 'X-User-Id': 'user-123' }
    }
  );
  
  return await response.json();
}
```

### Example 2: Using API Keys in Client Code

```javascript
// Client making authenticated API calls
const publicKey = 'pk_live_abc123...';
const secretKey = 'sk_live_xyz789...';  // From secure storage

fetch('http://localhost:8080/api/v1/payments/initiate', {
  method: 'POST',
  headers: {
    'X-API-Public-Key': publicKey,
    'X-API-Key': secretKey,  // This will be logged for metrics
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    amount: 10000,
    operator: 'OM',
    country: 'BJ'
  })
});
```

## 🔒 Security Best Practices

### 1. Key Storage
- ✅ Secret keys are hashed with BCrypt (strength 12)
- ✅ Only the last 4 characters are stored for display
- ✅ Full secret key is ONLY shown once at creation
- ❌ NEVER log secret keys in plain text

### 2. Key Rotation
- Rotate keys regularly (every 90 days recommended)
- Revoke old keys immediately after rotation
- Use the `/keys/rotate` endpoint for emergency rotation

### 3. Webhook Security
- Auto-disable webhooks after 10 consecutive failures
- Use HMAC signatures for webhook verification
- Validate webhook URLs before saving

### 4. Rate Limiting
**TODO**: Implement rate limiting per API key
```java
// Example with Bucket4j
@RateLimiter(name = "apiKey", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> createPayment() { ... }
```

### 5. IP Whitelisting
**TODO**: Add IP whitelist support for API keys
```java
// In ApiKey entity
@Column
private String allowedIps; // Comma-separated IPs

// In validation
public boolean isIpAllowed(String ip, String allowedIps) {
    if (allowedIps == null) return true;
    return Arrays.asList(allowedIps.split(",")).contains(ip);
}
```

## 🧪 Testing

### Create test data:
```sql
-- Insert a test user's API key
INSERT INTO api_keys (id, user_id, name, public_key, secret_key_hash, secret_key_hint, environment, is_active, created_at)
VALUES (
  UUID(),
  'demo-user',
  'Test Sandbox Key',
  'pk_sandbox_test123',
  '$2a$12$hashed_secret_here',
  '1234',
  'sandbox',
  TRUE,
  NOW()
);

-- Insert test logs
INSERT INTO api_request_logs (api_key_id, user_id, method, endpoint, status_code, status, latency_ms, created_at)
VALUES 
  ('api-key-id', 'demo-user', 'POST', '/api/v1/payments/initiate', 200, 'SUCCESS', 245, NOW()),
  ('api-key-id', 'demo-user', 'GET', '/api/v1/payments/status', 200, 'SUCCESS', 123, NOW()),
  ('api-key-id', 'demo-user', 'POST', '/api/v1/payments/initiate', 500, 'ERROR', 1234, NOW());
```

### Test endpoints:
```bash
# Health check
curl http://localhost:8080/api/v1/developer/health

# Get metrics
curl -H "X-User-Id: demo-user" http://localhost:8080/api/v1/developer/metrics

# Create API key
curl -X POST http://localhost:8080/api/v1/developer/keys \
  -H "X-User-Id: demo-user" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Key","environment":"sandbox"}'
```

## 📊 Monitoring Dashboard

The frontend should display:
1. **Overview Cards**: Total requests, success rate, avg latency, error rate
2. **Charts**: Hourly request breakdown (line/bar chart)
3. **API Keys Table**: List with status, last used, actions (revoke/rotate)
4. **Activity Feed**: Real-time logs with filtering
5. **Webhooks**: List of configured webhooks with status

## 🚀 Next Steps

1. **Implement JWT Authentication**: Replace X-User-Id header
2. **Add Rate Limiting**: Protect against abuse
3. **Implement IP Whitelisting**: Extra security layer
4. **Add Webhook Signing**: HMAC validation in webhook calls
5. **Create Scheduled Jobs**: Auto-cleanup old logs, check for expired keys
6. **Add Email Notifications**: Alert on key rotation, high error rates
7. **Implement API Key Scopes**: Limit what each key can access

---

**Created by**: FidelityPay Team  
**Version**: 1.0.0  
**Last Updated**: 2024-01-20

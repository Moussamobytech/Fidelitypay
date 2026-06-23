-- ============================
-- PAYMENT PROVIDER CATALOG
-- ============================
INSERT INTO payment_providers (code, display_name, status, credential_schema, created_at, updated_at) VALUES
('KKIAPAY', 'KkiaPay', 'ACTIVE', '{"fields":[{"key":"publicKey","label":"Clé API publique","required":true}]}', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('PAYDUNYA', 'PayDunya', 'ACTIVE', '{"fields":[{"key":"masterKey","label":"Clé principale","required":true},{"key":"privateKey","label":"Clé privée","required":true},{"key":"token","label":"Token","required":true}]}', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    credential_schema = VALUES(credential_schema),
    updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO payment_provider_routes
(provider_id, direction, country, operator, flow_type, provider_channel, enabled, live_enabled, sandbox_enabled, priority, cost, avg_latency, failure_rate, metrics_sample_count, updated_at)
SELECT pp.id, seed.direction, seed.country, seed.operator, seed.flow_type, seed.provider_channel,
       seed.enabled, seed.live_enabled, seed.sandbox_enabled, seed.priority, seed.cost, seed.avg_latency, seed.failure_rate, 0, CURRENT_TIMESTAMP(6)
FROM (
SELECT 'KKIAPAY', 'PAYIN', 'BJ', 'MTN', 'MOBILE_MONEY_REQUEST', 'mtn-benin', true, true, true, 10, 0, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'BJ', 'MOOV', 'MOBILE_MONEY_REQUEST', 'moov-benin', true, true, true, 10, 0, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'BJ', 'CELTIIS', 'MOBILE_MONEY_REQUEST', 'celtiis', true, true, true, 10, 0, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'BJ', 'MTN', 'HOSTED_CHECKOUT', 'mtn-benin', true, true, true, 20, 1.75, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'BJ', 'MOOV', 'HOSTED_CHECKOUT', 'moov-benin', true, true, true, 20, 1.75, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'CI', 'MTN', 'MOBILE_MONEY_REQUEST', 'mtn-ci', true, true, true, 10, 0, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'CI', 'MOOV', 'MOBILE_MONEY_REQUEST', 'moov-ci', true, true, true, 10, 0, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'CI', 'OM', 'ORANGE_CI_OTP', 'orange-money-ci', true, true, true, 10, 0, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'CI', 'WAVE', 'WAVE_REDIRECT', 'wave-ci', true, true, false, 10, 0, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'CI', 'OM', 'HOSTED_CHECKOUT', 'orange-money-ci', true, true, true, 20, 2.5, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'CI', 'MTN', 'HOSTED_CHECKOUT', 'mtn-ci', true, true, true, 20, 2.5, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'CI', 'MOOV', 'HOSTED_CHECKOUT', 'moov-ci', true, true, true, 20, 2.5, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'CI', 'WAVE', 'HOSTED_CHECKOUT', 'wave-ci', true, true, true, 20, 2.5, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'SN', 'OM', 'MOBILE_MONEY_REQUEST', 'orange', true, true, true, 10, 0, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'SN', 'YAS', 'MOBILE_MONEY_REQUEST', 'mixx', true, true, true, 10, 0, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'SN', 'WAVE', 'WAVE_REDIRECT', 'wave-senegal', true, true, false, 10, 0, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'SN', 'OM', 'HOSTED_CHECKOUT', 'orange-money-senegal', true, true, true, 20, 2.5, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'SN', 'WAVE', 'HOSTED_CHECKOUT', 'wave-senegal', true, true, true, 20, 2.5, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'SN', 'YAS', 'HOSTED_CHECKOUT', 'free-money-senegal', true, true, true, 20, 2.5, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'TG', 'MOOV', 'MOBILE_MONEY_REQUEST', 'moov-togo', true, true, true, 10, 0, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'TG', 'YAS', 'MOBILE_MONEY_REQUEST', 'mixx', true, true, true, 10, 0, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'TG', 'TMONEY', 'HOSTED_CHECKOUT', 't-money-togo', true, true, true, 20, 2.5, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'NE', 'AIRTEL', 'MOBILE_MONEY_REQUEST', 'airtel', true, true, true, 10, 0, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'BF', 'OM', 'HOSTED_CHECKOUT', 'orange-money-burkina', true, true, true, 20, 2.5, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'BF', 'MOOV', 'HOSTED_CHECKOUT', 'moov-burkina', true, true, true, 20, 2.5, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'CM', 'MTN', 'HOSTED_CHECKOUT', 'mtn-cameroon', true, true, true, 20, 0, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'INT', 'VISA', 'HOSTED_CHECKOUT', 'card', true, true, true, 20, 3.25, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'INT', 'MASTERCARD', 'HOSTED_CHECKOUT', 'card', true, true, true, 20, 3.25, 0, 0
) seed(provider, direction, country, operator, flow_type, provider_channel, enabled, live_enabled, sandbox_enabled, priority, cost, avg_latency, failure_rate)
JOIN payment_providers pp ON pp.code = seed.provider
ON DUPLICATE KEY UPDATE
    provider_channel = VALUES(provider_channel),
    enabled = VALUES(enabled),
    live_enabled = VALUES(live_enabled),
    sandbox_enabled = VALUES(sandbox_enabled),
    priority = VALUES(priority),
    cost = VALUES(cost),
    avg_latency = VALUES(avg_latency),
    failure_rate = VALUES(failure_rate),
    metrics_sample_count = VALUES(metrics_sample_count),
    updated_at = CURRENT_TIMESTAMP(6);

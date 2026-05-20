-- ============================
-- ROUTES WAVE
-- ============================

-- ============================
-- ROUTES WAVE
-- ============================

INSERT IGNORE INTO routes (name, operator, country, provider, availability, avg_latency, cost, failure_rate, priority) VALUES
('PAYDUNYA_WAVE_SN', 'WAVE', 'SN', 'PAYDUNYA', true, 0, 1.5, 0.02, 1),
('KKIAPAY_WAVE_SN',   'WAVE', 'SN', 'KKIAPAY',  true, 0, 1.8, 0.05, 2), -- FALLBACK SN
('KKIAPAY_WAVE_CI',   'WAVE', 'CI', 'KKIAPAY',  true, 0, 1.8, 0.05, 1),
('PAYDUNYA_WAVE_CI', 'WAVE', 'CI', 'PAYDUNYA', true, 0, 2.0, 0.06, 2); -- FALLBACK CI

-- ============================
-- ROUTES ORANGE MONEY (OM)
-- ============================

INSERT IGNORE INTO routes (name, operator, country, provider, availability, avg_latency, cost, failure_rate, priority) VALUES
('PAYDUNYA_OM_SN', 'OM', 'SN', 'PAYDUNYA', true, 0, 1.2, 0.01, 1),
('KKIAPAY_OM_SN',   'OM', 'SN', 'KKIAPAY',  true, 0, 1.5, 0.04, 2), -- FALLBACK SN
('PAYDUNYA_OM_BJ', 'OM', 'BJ', 'PAYDUNYA', true, 0, 1.7, 0.05, 2); -- FALLBACK BJ

-- ============================
-- ROUTES MOOV
-- ============================

INSERT IGNORE INTO routes (name, operator, country, provider, availability, avg_latency, cost, failure_rate, priority) VALUES
('KKIAPAY_MOOV_BJ', 'MOOV', 'BJ', 'KKIAPAY',  true, 0, 1.3, 0.03, 1),
('PAYDUNYA_MOOV_BJ', 'MOOV', 'BJ', 'PAYDUNYA', true, 0, 1.6, 0.06, 2), -- FALLBACK BJ
('PAYDUNYA_MOOV_CI', 'MOOV', 'CI', 'PAYDUNYA', true, 0, 1.6, 0.04, 1),
('KKIAPAY_MOOV_CI',  'MOOV', 'CI', 'KKIAPAY',  true, 0, 1.9, 0.07, 2); -- FALLBACK CI

DELETE FROM routes WHERE name = 'KKIAPAY_OM_BJ';

-- ============================
-- PAYMENT PROVIDER CATALOG
-- ============================
INSERT IGNORE INTO payment_providers (code, display_name, status, credential_schema, created_at, updated_at) VALUES
('KKIAPAY', 'KkiaPay', 'ACTIVE', '{"fields":[{"key":"publicKey","label":"Public Key","required":true},{"key":"privateKey","label":"Private Key","required":true},{"key":"secretKey","label":"Secret Key","required":false},{"key":"token","label":"Token","required":false}]}', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('PAYDUNYA', 'PayDunya', 'ACTIVE', '{"fields":[{"key":"masterKey","label":"Master Key","required":true},{"key":"privateKey","label":"Private Key","required":true},{"key":"token","label":"Token","required":true}]}', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

INSERT IGNORE INTO payment_provider_routes
(provider_id, direction, country, operator, flow_type, environment, provider_channel, enabled, observed_up, priority, avg_latency, failure_rate, updated_at)
SELECT pp.id, seed.direction, seed.country, seed.operator, seed.flow_type, seed.environment, seed.provider_channel,
       seed.enabled, seed.observed_up, seed.priority, seed.avg_latency, seed.failure_rate, CURRENT_TIMESTAMP(6)
FROM (
SELECT 'KKIAPAY', 'PAYIN', 'BJ', 'MTN', 'MOBILE_MONEY_REQUEST', 'LIVE', 'mtn-benin', true, true, 10, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'BJ', 'MOOV', 'MOBILE_MONEY_REQUEST', 'LIVE', 'moov-benin', true, true, 10, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'BJ', 'CELTIIS', 'MOBILE_MONEY_REQUEST', 'LIVE', 'celtiis-benin', true, true, 10, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'CI', 'MTN', 'MOBILE_MONEY_REQUEST', 'LIVE', 'mtn-ci', true, true, 10, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'CI', 'MOOV', 'MOBILE_MONEY_REQUEST', 'LIVE', 'moov-ci', true, true, 10, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'CI', 'OM', 'ORANGE_CI_OTP', 'LIVE', 'orange-money-ci', true, true, 10, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'CI', 'WAVE', 'WAVE_REDIRECT', 'LIVE', 'wave-ci', true, true, 10, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'TG', 'MOOV', 'MOBILE_MONEY_REQUEST', 'LIVE', 'moov-togo', true, true, 10, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'TG', 'MIXX', 'MOBILE_MONEY_REQUEST', 'LIVE', 't-money-togo', true, true, 10, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'SN', 'OM', 'MOBILE_MONEY_REQUEST', 'LIVE', 'orange-money-senegal', true, true, 10, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'SN', 'MIXX', 'MOBILE_MONEY_REQUEST', 'LIVE', 'free-money-senegal', true, true, 10, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'SN', 'WAVE', 'WAVE_REDIRECT', 'LIVE', 'wave-senegal', true, true, 10, 0, 0
UNION ALL SELECT 'KKIAPAY', 'PAYIN', 'NE', 'AIRTEL', 'MOBILE_MONEY_REQUEST', 'LIVE', 'airtel-niger', true, true, 10, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'SN', 'OM', 'HOSTED_CHECKOUT', 'LIVE', 'orange-money-senegal', true, true, 20, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'SN', 'WAVE', 'HOSTED_CHECKOUT', 'LIVE', 'wave-senegal', true, true, 20, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'SN', 'MIXX', 'HOSTED_CHECKOUT', 'LIVE', 'free-money-senegal', true, true, 20, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'SN', 'EXPRESSO', 'HOSTED_CHECKOUT', 'LIVE', 'expresso-sn', true, true, 20, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'CI', 'OM', 'HOSTED_CHECKOUT', 'LIVE', 'orange-money-ci', true, true, 20, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'CI', 'MTN', 'HOSTED_CHECKOUT', 'LIVE', 'mtn-ci', true, true, 20, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'CI', 'MOOV', 'HOSTED_CHECKOUT', 'LIVE', 'moov-ci', true, true, 20, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'CI', 'WAVE', 'HOSTED_CHECKOUT', 'LIVE', 'wave-ci', true, true, 20, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'BJ', 'MTN', 'HOSTED_CHECKOUT', 'LIVE', 'mtn-benin', true, true, 20, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'BJ', 'MOOV', 'HOSTED_CHECKOUT', 'LIVE', 'moov-benin', true, true, 20, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'TG', 'MOOV', 'HOSTED_CHECKOUT', 'LIVE', 'moov-togo', true, true, 20, 0, 0
UNION ALL SELECT 'PAYDUNYA', 'PAYIN', 'TG', 'MIXX', 'HOSTED_CHECKOUT', 'LIVE', 't-money-togo', true, true, 20, 0, 0
) seed(provider, direction, country, operator, flow_type, environment, provider_channel, enabled, observed_up, priority, avg_latency, failure_rate)
JOIN payment_providers pp ON pp.code = seed.provider;

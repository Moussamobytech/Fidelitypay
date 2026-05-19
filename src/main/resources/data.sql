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

INSERT IGNORE INTO payment_routes
(provider, direction, country, operator, flow_type, environment, provider_channel, enabled, observed_up, priority, avg_latency, failure_rate)
VALUES
('KKIAPAY', 'PAYIN', 'BJ', 'MTN', 'MOBILE_MONEY_REQUEST', 'LIVE', 'mtn-benin', true, true, 10, 0, 0),
('KKIAPAY', 'PAYIN', 'BJ', 'MOOV', 'MOBILE_MONEY_REQUEST', 'LIVE', 'moov-benin', true, true, 10, 0, 0),
('KKIAPAY', 'PAYIN', 'BJ', 'CELTIIS', 'MOBILE_MONEY_REQUEST', 'LIVE', 'celtiis-benin', true, true, 10, 0, 0),
('KKIAPAY', 'PAYIN', 'CI', 'MTN', 'MOBILE_MONEY_REQUEST', 'LIVE', 'mtn-ci', true, true, 10, 0, 0),
('KKIAPAY', 'PAYIN', 'CI', 'MOOV', 'MOBILE_MONEY_REQUEST', 'LIVE', 'moov-ci', true, true, 10, 0, 0),
('KKIAPAY', 'PAYIN', 'CI', 'OM', 'ORANGE_CI_OTP', 'LIVE', 'orange-money-ci', true, true, 10, 0, 0),
('KKIAPAY', 'PAYIN', 'CI', 'WAVE', 'WAVE_REDIRECT', 'LIVE', 'wave-ci', true, true, 10, 0, 0),
('KKIAPAY', 'PAYIN', 'TG', 'MOOV', 'MOBILE_MONEY_REQUEST', 'LIVE', 'moov-togo', true, true, 10, 0, 0),
('KKIAPAY', 'PAYIN', 'TG', 'MIXX', 'MOBILE_MONEY_REQUEST', 'LIVE', 't-money-togo', true, true, 10, 0, 0),
('KKIAPAY', 'PAYIN', 'SN', 'OM', 'MOBILE_MONEY_REQUEST', 'LIVE', 'orange-money-senegal', true, true, 10, 0, 0),
('KKIAPAY', 'PAYIN', 'SN', 'MIXX', 'MOBILE_MONEY_REQUEST', 'LIVE', 'free-money-senegal', true, true, 10, 0, 0),
('KKIAPAY', 'PAYIN', 'SN', 'WAVE', 'WAVE_REDIRECT', 'LIVE', 'wave-senegal', true, true, 10, 0, 0),
('KKIAPAY', 'PAYIN', 'NE', 'AIRTEL', 'MOBILE_MONEY_REQUEST', 'LIVE', 'airtel-niger', true, true, 10, 0, 0),
('PAYDUNYA', 'PAYIN', 'SN', 'OM', 'HOSTED_CHECKOUT', 'LIVE', 'orange-money-senegal', true, true, 20, 0, 0),
('PAYDUNYA', 'PAYIN', 'SN', 'WAVE', 'HOSTED_CHECKOUT', 'LIVE', 'wave-senegal', true, true, 20, 0, 0),
('PAYDUNYA', 'PAYIN', 'SN', 'MIXX', 'HOSTED_CHECKOUT', 'LIVE', 'free-money-senegal', true, true, 20, 0, 0),
('PAYDUNYA', 'PAYIN', 'SN', 'EXPRESSO', 'HOSTED_CHECKOUT', 'LIVE', 'expresso-sn', true, true, 20, 0, 0),
('PAYDUNYA', 'PAYIN', 'CI', 'OM', 'HOSTED_CHECKOUT', 'LIVE', 'orange-money-ci', true, true, 20, 0, 0),
('PAYDUNYA', 'PAYIN', 'CI', 'MTN', 'HOSTED_CHECKOUT', 'LIVE', 'mtn-ci', true, true, 20, 0, 0),
('PAYDUNYA', 'PAYIN', 'CI', 'MOOV', 'HOSTED_CHECKOUT', 'LIVE', 'moov-ci', true, true, 20, 0, 0),
('PAYDUNYA', 'PAYIN', 'CI', 'WAVE', 'HOSTED_CHECKOUT', 'LIVE', 'wave-ci', true, true, 20, 0, 0),
('PAYDUNYA', 'PAYIN', 'BJ', 'MTN', 'HOSTED_CHECKOUT', 'LIVE', 'mtn-benin', true, true, 20, 0, 0),
('PAYDUNYA', 'PAYIN', 'BJ', 'MOOV', 'HOSTED_CHECKOUT', 'LIVE', 'moov-benin', true, true, 20, 0, 0),
('PAYDUNYA', 'PAYIN', 'TG', 'MOOV', 'HOSTED_CHECKOUT', 'LIVE', 'moov-togo', true, true, 20, 0, 0),
('PAYDUNYA', 'PAYIN', 'TG', 'MIXX', 'HOSTED_CHECKOUT', 'LIVE', 't-money-togo', true, true, 20, 0, 0);

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
('KKIAPAY_OM_BJ',   'OM', 'BJ', 'KKIAPAY',  true, 0, 1.4, 0.03, 1),
('PAYDUNYA_OM_BJ', 'OM', 'BJ', 'PAYDUNYA', true, 0, 1.7, 0.05, 2); -- FALLBACK BJ

-- ============================
-- ROUTES MOOV
-- ============================

INSERT IGNORE INTO routes (name, operator, country, provider, availability, avg_latency, cost, failure_rate, priority) VALUES
('KKIAPAY_MOOV_BJ', 'MOOV', 'BJ', 'KKIAPAY',  true, 0, 1.3, 0.03, 1),
('PAYDUNYA_MOOV_BJ', 'MOOV', 'BJ', 'PAYDUNYA', true, 0, 1.6, 0.06, 2), -- FALLBACK BJ
('PAYDUNYA_MOOV_CI', 'MOOV', 'CI', 'PAYDUNYA', true, 0, 1.6, 0.04, 1),
('KKIAPAY_MOOV_CI',  'MOOV', 'CI', 'KKIAPAY',  true, 0, 1.9, 0.07, 2); -- FALLBACK CI

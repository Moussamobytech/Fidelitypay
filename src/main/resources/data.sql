INSERT IGNORE INTO routes (name, operator, provider, availability, cost, avg_latency, failure_rate, priority) VALUES 
('PAYDUNYA_WAVE', 'WAVE', 'PAYDUNYA', true, 10.0, 200.0, 0.01, 1),
('KKIAPAY_MTN', 'MTN', 'KKIAPAY', true, 15.0, 300.0, 0.02, 1),
('PAYDUNYA_ORANGE', 'ORANGE', 'PAYDUNYA', true, 12.0, 250.0, 0.05, 1),
('KKIAPAY_WAVE_BACKUP', 'WAVE', 'KKIAPAY', true, 20.0, 400.0, 0.0, 2);

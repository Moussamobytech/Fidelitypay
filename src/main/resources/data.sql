-- ============================
-- ROUTES WAVE
-- ============================

INSERT IGNORE INTO routes (
  name,
  operator,
  provider,
  availability,
  avg_latency,
  cost,
  failure_rate,
  priority
) VALUES
(
  'PAYDUNYA_WAVE',
  'WAVE',
  'PAYDUNYA',
  true,
  0,
  1.5,
  0.02,
  1
),
(
  'KKIAPAY_WAVE',
  'WAVE',
  'KKIAPAY',
  true,
  0,
  1.8,
  0.05,
  2
);

-- ============================
-- ROUTES ORANGE MONEY
-- ============================

INSERT IGNORE INTO routes (
  name,
  operator,
  provider,
  availability,
  avg_latency,
  cost,
  failure_rate,
  priority
) VALUES
(
  'PAYDUNYA_OM',
  'OM',
  'PAYDUNYA',
  true,
  0,
  1.2,
  0.01,
  1
);

-- ============================
-- ROUTES MOOV
-- ============================

INSERT IGNORE INTO routes (
  name,
  operator,
  provider,
  availability,
  avg_latency,
  cost,
  failure_rate,
  priority
) VALUES
(
  'KKIAPAY_MOOV',
  'MOOV',
  'KKIAPAY',
  true,
  0,
  1.3,
  0.03,
  1
);

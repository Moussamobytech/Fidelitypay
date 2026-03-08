-- ============================
-- ROUTES WAVE
-- ============================

INSERT IGNORE INTO routes (
  name,
  operator,
  country,
  provider,
  availability,
  avg_latency,
  cost,
  failure_rate,
  priority
) VALUES
(
  'PAYDUNYA_WAVE_SN',
  'WAVE',
  'SN',
  'PAYDUNYA',
  true,
  0,
  1.5,
  0.02,
  1
),
(
  'KKIAPAY_WAVE_CI',
  'WAVE',
  'CI',
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
  country,
  provider,
  availability,
  avg_latency,
  cost,
  failure_rate,
  priority
) VALUES
(
  'PAYDUNYA_OM_SN',
  'OM',
  'SN',
  'PAYDUNYA',
  true,
  0,
  1.2,
  0.01,
  1
),
(
  'KKIAPAY_OM_BJ',
  'OM',
  'BJ',
  'KKIAPAY',
  true,
  0,
  1.4,
  0.03,
  1
);

-- ============================
-- ROUTES MOOV
-- ============================

INSERT IGNORE INTO routes (
  name,
  operator,
  country,
  provider,
  availability,
  avg_latency,
  cost,
  failure_rate,
  priority
) VALUES
(
  'KKIAPAY_MOOV_BJ',
  'MOOV',
  'BJ',
  'KKIAPAY',
  true,
  0,
  1.3,
  0.03,
  1
),
(
  'MOOV_ADJAME_CI',
  'MOOV',
  'CI',
  'PAYDUNYA',
  true,
  0,
  1.6,
  0.04,
  2
);

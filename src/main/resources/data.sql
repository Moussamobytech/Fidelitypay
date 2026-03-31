-- ============================
-- ROUTES WAVE (suite)
-- ============================

-- Wave CI via PAYDUNYA
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
  'PAYDUNYA_WAVE_CI',
  'WAVE',
  'CI',
  'PAYDUNYA',
  true,
  0,
  1.7,
  0.03,
  2
),

-- Wave SN via KKIAPAY
(
  'KKIAPAY_WAVE_SN',
  'WAVE',
  'SN',
  'KKIAPAY',
  true,
  0,
  1.6,
  0.04,
  2
);

-- ============================
-- ROUTES ORANGE MONEY (suite)
-- ============================

-- Orange Money CI via PAYDUNYA
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
  'PAYDUNYA_OM_CI',
  'OM',
  'CI',
  'PAYDUNYA',
  true,
  0,
  1.3,
  0.02,
  2
),

-- Orange Money SN via KKIAPAY
(
  'KKIAPAY_OM_SN',
  'OM',
  'SN',
  'KKIAPAY',
  true,
  0,
  1.5,
  0.04,
  2
);

-- ============================
-- ROUTES MOOV (suite)
-- ============================

-- Moov SN via PAYDUNYA
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
  'PAYDUNYA_MOOV_SN',
  'MOOV',
  'SN',
  'PAYDUNYA',
  true,
  0,
  1.4,
  0.03,
  2
),

-- Moov CI via KKIAPAY
(
  'KKIAPAY_MOOV_CI',
  'MOOV',
  'CI',
  'KKIAPAY',
  true,
  0,
  1.7,
  0.05,
  2
);
-- ============================
-- ROUTES MALI (ML)
-- ============================

-- Routes WAVE Mali
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
  'PAYDUNYA_WAVE_ML',
  'WAVE',
  'ML',
  'PAYDUNYA',
  true,
  0,
  1.8,
  0.04,
  2
),
(
  'KKIAPAY_WAVE_ML',
  'WAVE',
  'ML',
  'KKIAPAY',
  true,
  0,
  1.9,
  0.05,
  2
);

-- Routes ORANGE MONEY Mali
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
  'PAYDUNYA_OM_ML',
  'OM',
  'ML',
  'PAYDUNYA',
  true,
  0,
  1.4,
  0.03,
  1
),
(
  'KKIAPAY_OM_ML',
  'OM',
  'ML',
  'KKIAPAY',
  true,
  0,
  1.6,
  0.04,
  2
);

-- Routes MOOV Mali
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
  'PAYDUNYA_MOOV_ML',
  'MOOV',
  'ML',
  'PAYDUNYA',
  true,
  0,
  1.5,
  0.04,
  2
),
(
  'KKIAPAY_MOOV_ML',
  'MOOV',
  'ML',
  'KKIAPAY',
  true,
  0,
  1.7,
  0.05,
  2
);
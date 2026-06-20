-- One-time migration from duplicated LIVE/SANDBOX route rows to route capabilities.
-- Run while the application container is stopped.

ALTER TABLE payment_provider_routes
    ADD COLUMN live_enabled BIT(1) NOT NULL DEFAULT b'1' AFTER enabled,
    ADD COLUMN sandbox_enabled BIT(1) NOT NULL DEFAULT b'1' AFTER live_enabled;

-- KkiaPay does not expose Wave in its sandbox API.
UPDATE payment_provider_routes route
JOIN payment_providers provider ON provider.id = route.provider_id
SET route.sandbox_enabled = b'0'
WHERE provider.code = 'KKIAPAY'
  AND route.operator = 'WAVE'
  AND route.environment = 'LIVE';

-- Settings belong to the canonical LIVE row after environments are merged.
DELETE setting
FROM merchant_payment_route_settings setting
JOIN payment_provider_routes route ON route.id = setting.payment_provider_route_id
WHERE route.environment = 'SANDBOX'
   OR route.direction = 'PAYOUT';

DELETE FROM payment_provider_routes
WHERE environment = 'SANDBOX'
   OR direction = 'PAYOUT';

-- Remove obsolete aliases and unsupported routes from earlier catalog seeds.
DELETE setting
FROM merchant_payment_route_settings setting
JOIN payment_provider_routes route ON route.id = setting.payment_provider_route_id
JOIN payment_providers provider ON provider.id = route.provider_id
WHERE (provider.code = 'KKIAPAY' AND (
          (route.country = 'SN' AND route.operator = 'MIXX')
       OR (route.country = 'TG' AND route.operator IN ('MIXX', 'TMONEY'))
      ))
   OR (provider.code = 'PAYDUNYA' AND (
          (route.country = 'SN' AND route.operator IN ('EXPRESSO', 'MIXX'))
       OR (route.country = 'TG' AND route.operator IN ('MIXX', 'MOOV'))
      ));

DELETE route
FROM payment_provider_routes route
JOIN payment_providers provider ON provider.id = route.provider_id
WHERE (provider.code = 'KKIAPAY' AND (
          (route.country = 'SN' AND route.operator = 'MIXX')
       OR (route.country = 'TG' AND route.operator IN ('MIXX', 'TMONEY'))
      ))
   OR (provider.code = 'PAYDUNYA' AND (
          (route.country = 'SN' AND route.operator IN ('EXPRESSO', 'MIXX'))
       OR (route.country = 'TG' AND route.operator IN ('MIXX', 'MOOV'))
      ));

ALTER TABLE payment_provider_routes
    DROP INDEX uk_payment_provider_route,
    DROP INDEX idx_provider_route_lookup,
    DROP COLUMN environment,
    ADD CONSTRAINT uk_payment_provider_route
        UNIQUE (provider_id, direction, country, operator, flow_type),
    ADD INDEX idx_provider_route_lookup (direction, country, operator, enabled);

UPDATE payment_providers
SET credential_schema = '{"fields":[{"key":"publicKey","label":"Clé API publique","required":true}]}'
WHERE code = 'KKIAPAY';

UPDATE payment_providers
SET credential_schema = '{"fields":[{"key":"masterKey","label":"Clé principale","required":true},{"key":"privateKey","label":"Clé privée","required":true},{"key":"token","label":"Token","required":true}]}'
WHERE code = 'PAYDUNYA';

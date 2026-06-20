# Developer Portal API

Developer portal endpoints are dashboard endpoints. They require a JWT from `/api/v1/auth/login`.

Merchant payment endpoints are different: they use one `X-API-Key` header.

## API Keys

`api_keys` stores merchant API credentials owned by a dashboard user.

Columns:

- `id`: internal UUID.
- `user_id`: key owner.
- `name`: dashboard label.
- `public_key`: internal lookup portion of the API key.
- `secret_key_hash`: BCrypt hash of the secret portion.
- `secret_key_hint`: last characters used for masked dashboard display.
- `environment`: legacy internal column; provider accounts define Live or Sandbox.
- `is_active`: active keys can authenticate. Merchant-deleted keys are kept inactive internally for audit/history.
- `created_at`, `last_used_at`, `last_used_ip`, `expires_at`: audit fields.

The complete API key is returned only once, during creation.

## Create A Key

```bash
curl -X POST http://localhost:8060/api/v1/developer/keys \
  -H "Authorization: Bearer JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Production backend"
  }'
```

Response:

```json
{
  "id": "uuid",
  "name": "Production backend",
  "apiKey": "fp_xxx.secret_xxx",
  "apiKeyMasked": "fp_xxx.****abcd",
  "isActive": true,
  "createdAt": "2026-05-06T12:00:00"
}
```

## List Keys

```bash
curl -X GET "http://localhost:8060/api/v1/developer/keys" \
  -H "Authorization: Bearer JWT_TOKEN"
```

Later responses expose only `apiKeyMasked`. Merchant list responses return active keys only.

## Rename A Key

```bash
curl -X PATCH http://localhost:8060/api/v1/developer/keys/{keyId} \
  -H "Authorization: Bearer JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Production backend v2"
  }'
```

## Delete A Key

```bash
curl -X DELETE http://localhost:8060/api/v1/developer/keys/{keyId} \
  -H "Authorization: Bearer JWT_TOKEN"
```

Deleted keys stop authenticating and disappear from merchant key listings.

## Merchant Payment Auth

Use the API key from a merchant backend:

```bash
curl -X POST http://localhost:8060/api/v1/payments/initiate \
  -H "Content-Type: application/json" \
  -H "X-API-Key: fp_xxx.secret_xxx" \
  -H "Idempotency-Key: order-1001" \
  -d '{
    "amount": 5000,
    "country": "SN",
    "operator": "WAVE",
    "customer": {
      "phone": "221776006060",
      "firstname": "Awa",
      "lastname": "Diop",
      "email": "awa@example.com"
    },
    "returnUrl": "https://merchant.example/success",
    "cancelUrl": "https://merchant.example/cancel"
  }'
```

Payments are asynchronous. Store `paymentId`, then wait for the merchant webhook or query the status endpoint.

Merchant API calls use Live provider accounts. Sandbox and Live provider credentials are exercised from the dashboard test bench before external integration. The response `flowType` identifies the customer journey: `MOBILE_MONEY_REQUEST`, `WAVE_REDIRECT`, `ORANGE_CI_OTP`, or `HOSTED_CHECKOUT`.

`returnUrl` and `cancelUrl` are optional browser redirect URLs for hosted checkout flows. They are not proof of payment. Treat webhooks or status reads as the source of truth.

## Dashboard Test Payment

The dashboard also exposes a JWT-authenticated manual tester at `POST /api/payments/initiate`.
It is for merchants testing their FidelityPay setup from the dashboard. Public platform integrations and SDKs should use the API-key endpoint `POST /api/v1/payments/initiate`.

## Webhooks

Provider callbacks remain internal to FidelityPay. Merchants configure webhook URLs in the dashboard under Integration & Clés and receive:

- `payment.success`
- `payment.failed`
- `payment.cancelled`
- `payment.requires_action`
- `payment.reconciliation`

# Developer Portal API

Developer portal endpoints are dashboard endpoints. They require a JWT from `/api/v1/auth/login`.

Merchant payment endpoints are different: they use `X-API-Public-Key` and `X-API-Secret-Key`.

## API Keys

`api_keys` stores merchant API credentials owned by a dashboard user.

Columns:

- `id`: internal UUID.
- `user_id`: key owner.
- `name`: dashboard label.
- `public_key`: lookup key, safe to display.
- `secret_key_hash`: BCrypt hash of the secret key.
- `secret_key_hint`: last characters used for dashboard display.
- `environment`: `sandbox` or `live`.
- `is_active`: revoked keys are inactive.
- `created_at`, `last_used_at`, `last_used_ip`, `expires_at`: audit fields.

The raw secret key is returned only once, during creation.

## Create A Key

```bash
curl -X POST http://localhost:8060/api/v1/developer/keys \
  -H "Authorization: Bearer JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Production backend",
    "environment": "live"
  }'
```

Response:

```json
{
  "id": "uuid",
  "name": "Production backend",
  "publicKey": "pk_live_xxx",
  "secretKey": "sk_live_xxx",
  "secretKeyMasked": "sk_live_****abcd",
  "environment": "live",
  "isActive": true,
  "createdAt": "2026-05-06T12:00:00"
}
```

## List Keys

```bash
curl -X GET "http://localhost:8060/api/v1/developer/keys?environment=live" \
  -H "Authorization: Bearer JWT_TOKEN"
```

Later responses never expose `secretKey`.

## Revoke A Key

```bash
curl -X POST http://localhost:8060/api/v1/developer/keys/{keyId}/revoke \
  -H "Authorization: Bearer JWT_TOKEN"
```

## Merchant Payment Auth

Use the key pair from a merchant backend:

```bash
curl -X POST http://localhost:8060/api/v1/payments/initiate \
  -H "Content-Type: application/json" \
  -H "X-API-Public-Key: pk_live_xxx" \
  -H "X-API-Secret-Key: sk_live_xxx" \
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

## Webhooks

Provider callbacks remain internal to FidelityPay. Merchants configure webhook URLs in the dashboard and receive:

- `payment.success`
- `payment.failed`
- `payment.cancelled`
- `payment.requires_action`

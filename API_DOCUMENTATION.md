# Documentation API FidelityPay

Cette documentation couvre le pay-in marchand. Les endpoints dashboard/admin utilisent le JWT; les endpoints marchands de paiement utilisent les clés API.

## Authentification marchand

Chaque appel marchand doit envoyer:

- `X-API-Public-Key`: clé publique du marchand
- `X-API-Secret-Key`: secret associé, affiché une seule fois à la création
- `Idempotency-Key`: identifiant unique de la tentative côté marchand
- `Content-Type: application/json`

## Initier un paiement pay-in

`POST /api/v1/payments/initiate`

URL locale: `http://localhost:8060/api/v1/payments/initiate`

```json
{
  "amount": 5000,
  "country": "SN",
  "operator": "WAVE",
  "customer": {
    "phone": "221776006060",
    "firstname": "Awa",
    "lastname": "Diop",
    "email": "awa@example.com"
  },
  "returnUrl": "https://merchant.example.com/success",
  "cancelUrl": "https://merchant.example.com/cancel"
}
```

`amount` est un entier en XOF. Le routage est fait par correspondance exacte entre `direction=PAYIN`, `country`, `operator`, l'environnement de la clé API, et les routes de paiement actives.

### Exemple curl

```bash
curl -X POST http://localhost:8060/api/v1/payments/initiate \
  -H "Content-Type: application/json" \
  -H "X-API-Public-Key: pk_sandbox_xxx" \
  -H "X-API-Secret-Key: sk_sandbox_xxx" \
  -H "Idempotency-Key: order-123-init-1" \
  -d '{
    "amount": 5000,
    "country": "SN",
    "operator": "WAVE",
    "customer": {
      "phone": "221776006060",
      "firstname": "Awa",
      "lastname": "Diop"
    },
    "returnUrl": "https://merchant.example.com/success",
    "cancelUrl": "https://merchant.example.com/cancel"
  }'
```

### Réponse

```json
{
  "paymentId": "FP-...",
  "status": "PENDING",
  "paymentUrl": "https://provider.example/checkout/...",
  "provider": "KKIAPAY",
  "operator": "WAVE",
  "country": "SN",
  "amount": 5000,
  "currency": "XOF",
  "nextAction": null,
  "failureReason": null
}
```

Pour Orange Money Côte d'Ivoire via Kkiapay, la réponse peut être:

```json
{
  "paymentId": "FP-...",
  "status": "REQUIRES_ACTION",
  "provider": "KKIAPAY",
  "operator": "OM",
  "country": "CI",
  "amount": 5000,
  "currency": "XOF",
  "nextAction": {
    "type": "SUBMIT_OTP",
    "url": "/api/v1/payments/FP-.../actions/otp"
  }
}
```

## Valider une action OTP

`POST /api/v1/payments/{paymentId}/actions/otp`

Même authentification par clés API.

```json
{
  "otp": "123456"
}
```

## Vérifier le statut

`GET /api/v1/payments/{paymentId}`

Même authentification par clés API. Le paiement doit appartenir à la clé API utilisée.

## Statuts de paiement

- `PENDING`: paiement initié, en attente de confirmation.
- `REQUIRES_ACTION`: une action payeur est nécessaire.
- `PENDING_RECONCILIATION`: le fournisseur a peut-être reçu la demande, mais Fidelity Pay attend une confirmation par callback ou status API.
- `SUCCESS`: paiement réussi.
- `FAILED`: paiement échoué ou refusé.
- `CANCELLED`: paiement annulé avant finalisation.

## Capacités pay-in Kkiapay

Matrice officielle pay-in actuellement supportée:

- `BJ`: `MTN`, `MOOV`, `CELTIIS`
- `CI`: `MTN`, `MOOV`, `OM`, `WAVE`
- `TG`: `MOOV`, `MIXX`
- `SN`: `OM`, `MIXX`, `WAVE`
- `NE`: `AIRTEL`

Alias acceptés: `ORANGE`/`ORANGE_MONEY` vers `OM`, `FREE`/`FREEMONEY`/`MIXX BY YAS` vers `MIXX`.

## Callbacks et webhooks

Les callbacks fournisseurs sont internes à Fidelity Pay:

- Kkiapay: `/api/payments/callback/kkiapay`
- PayDunya: `/api/payments/callback/paydunya`

Les marchands ne configurent pas ces URLs. Ils configurent plutôt des webhooks dashboard via `/api/v1/developer/webhooks`, puis Fidelity Pay envoie les événements:

- `payment.success`
- `payment.failed`
- `payment.cancelled`
- `payment.requires_action`

Les notifications développeur sont signées avec `X-FidelityPay-Signature`.

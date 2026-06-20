# Documentation API FidelityPay

Cette documentation couvre le pay-in marchand. Les endpoints dashboard/admin utilisent le JWT; les endpoints marchands de paiement utilisent les clés API.

## Authentification marchand

Chaque appel marchand doit envoyer:

- `X-API-Key`: clé unique du marchand, affichée une seule fois à la création
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

`amount` est un entier en XOF. Les appels API marchands utilisent les comptes agrégateurs Live. Le téléphone est obligatoire pour le mobile money et optionnel pour Visa/Mastercard.

### Exemple curl

```bash
curl -X POST http://localhost:8060/api/v1/payments/initiate \
  -H "Content-Type: application/json" \
  -H "X-API-Key: fp_xxx.secret_xxx" \
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
  "flowType": "WAVE_REDIRECT",
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
    "provider": "KKIAPAY",
    "message": "Submit the Orange Money CI OTP to continue this payment"
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

## Parcours de paiement

- `MOBILE_MONEY_REQUEST`: le payeur confirme la demande sur son téléphone.
- `WAVE_REDIRECT`: ouvrez `paymentUrl` pour poursuivre dans Wave.
- `ORANGE_CI_OTP`: envoyez l'OTP avec l'endpoint d'action.
- `HOSTED_CHECKOUT`: ouvrez `paymentUrl` et terminez le paiement chez l'agrégateur.

La couverture active est exposée dans le dashboard Intégration; elle dépend des agrégateurs et environnements configurés.

## Callbacks et webhooks

Les callbacks fournisseurs sont internes à Fidelity Pay. Les marchands configurent leur webhook dans le dashboard Intégration, puis Fidelity Pay envoie:

- `payment.success`
- `payment.failed`
- `payment.cancelled`
- `payment.requires_action`
- `payment.reconciliation`

Les notifications développeur sont signées avec `X-FidelityPay-Signature`.

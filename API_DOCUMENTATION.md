# Documentation API FidelityPay

Voici la liste des endpoints disponibles pour tester l'API sur Postman.

## 1. Initier un Paiement
Cet endpoint permet de démarrer une transaction. Le système choisira automatiquement la meilleure route (Kkiapay, PayDunya, etc.) selon l'opérateur.

*   **Méthode** : `POST`
*   **URL** : `http://localhost:8080/api/payments/initiate`
*   **Headers** :
    *   `Content-Type`: `application/json`

### Corps de la requête (Body JSON) :
**Important** : Le champ `phone` est obligatoire pour identifier le payeur.

```json
{
    "amount": 500,
    "country": "SN",
    "operator": "WAVE",
    "phone": "221776006060"
}
```

*   `amount` : Montant de la transaction (ex: 500).
*   `country` : Code pays ISO (ex: "SN" pour Sénégal, "BJ" pour Bénin, "ML" pour Mali).
*   `operator` : Nom de l'opérateur (ex: "WAVE", "OM" pour Orange Money, "MOOV").
*   `phone` : Numéro de téléphone du client qui va payer.

---

## 2. Vérifier le Statut d'un Paiement
Permet de suivre l'état d'un paiement grâce à son ID unique.

*   **Méthode** : `GET`
*   **URL** : `http://localhost:8080/api/payments/status/{paymentId}`

### Exemple :
Si l'initialisation vous renvoie un ID comme `a1b2c3d4-0000...` :
`http://localhost:8080/api/payments/status/a1b2c3d4-0000-0000-0000-000000000000`

---

## 3. Lister les Options de Paiement
Affiche les moyens de paiement disponibles pour un pays donné.

*   **Méthode** : `GET`
*   **URL** : `http://localhost:8080/api/payment-options?country=SN`

### Paramètres :
*   `country` : Le code du pays (ex: `SN`, `BJ`).

---

## 4. Monitoring des Routes
Permet de piloter et consulter l'état des routes de paiement.

*   **Vérification manuelle** : `POST http://localhost:8080/api/monitoring/check` (Force un check de toutes les routes).
*   **Liste des routes** : `GET http://localhost:8080/api/monitoring/routes` (Récupère l'état et la latence actuelle des routes).
*   **Historique des logs** : `GET http://localhost:8080/api/monitoring/logs` (Récupère tout l'historique technique des transactions).

---

## 5. Lister Tous les Paiements
Récupère l'historique complet de tous les paiements enregistrés.

*   **Méthode** : `GET`
*   **URL** : `http://localhost:8080/api/payments`

---

## Codes de Statut (Base de données)
*   `PENDING` : Paiement initié, en attente de réponse ou de validation.
*   `SUCCESS` : Paiement réussi.
*   `FAILED` : Paiement échoué ou refusé.

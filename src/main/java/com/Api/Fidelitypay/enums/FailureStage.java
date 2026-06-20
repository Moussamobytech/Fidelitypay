package com.Api.Fidelitypay.enums;

/**
 * Étape du parcours de paiement où l'échec a été détecté.
 */
public enum FailureStage {
    /** Contrôles métier avant appel fournisseur (téléphone, montant, etc.). */
    VALIDATION,
    /** Sélection de route / disponibilité agrégateur. */
    ROUTING,
    /** Appel d'initialisation ou d'action côté fournisseur. */
    PROVIDER_INIT,
    /** Étape interactive (OTP, validation d'action). */
    PROVIDER_ACTION,
    /** Notification asynchrone fournisseur (callback / IPN). */
    PROVIDER_CALLBACK,
    /** Résultat fournisseur ambigu nécessitant réconciliation. */
    RECONCILIATION,
    /** Erreur applicative inattendue. */
    INTERNAL,
    /** Étape non identifiée. */
    UNKNOWN
}

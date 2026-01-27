package com.Api.Fidelitypay.Enum;

public enum ErrorType {
    NETWORK, // Problème internet / DNS
    TIMEOUT, // Temps de réponse trop long
    AUTHENTICATION, // Clé API invalide
    PROVIDER_DOWN, // Serveur du provider HS
    BAD_REQUEST, // Erreur 400
    INTERNAL_ERROR, // Erreur interne
    UNKNOWN // Erreur inconnue
}
